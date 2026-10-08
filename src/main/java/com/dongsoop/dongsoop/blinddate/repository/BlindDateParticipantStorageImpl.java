package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.exception.InvalidBlindDateChoiceException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BlindDateParticipantStorageImpl implements BlindDateParticipantStorage {

    // memberId -> ParticipantInfo (한 사용자당 1개, 여러 소켓 보유 가능)
    private final Map<Long, ParticipantInfo> participants = new ConcurrentHashMap<>();

    // socketId -> memberId (socketId로 참여자를 O(1)에 찾기 위한 역방향 인덱스)
    private final Map<String, Long> socketIdToMemberId = new ConcurrentHashMap<>();

    // sessionId -> 익명 번호 카운터
    private final Map<String, AtomicInteger> nameCounters = new ConcurrentHashMap<>();

    // 최종 응답과 쌍별 처리 권한은 sessionId별 한 곳에서 관리한다.
    private final Map<String, ChoiceRound> choiceRounds = new ConcurrentHashMap<>();

    /**
     * 참여자 추가 또는 소켓 추가
     * <p>
     * memberId 기준으로 ConcurrentHashMap#compute를 사용해 조회와 반영을 원자적으로 묶는다. (외부에서 별도 락을
     * 잡고 호출하지 않아도 이 메서드 자체로 스레드 안전함)
     *
     * @param sessionId 참여하려는 세션 id
     * @param memberId  참여 주체 회원 id
     * @param socketId  참여 주체 소켓 id
     */
    public ParticipantInfo addParticipant(String sessionId, Long memberId, String socketId) {
        ParticipantInfo participant = participants.compute(memberId, (id, existing) -> {
            // 처음 참여하는 경우
            if (existing == null) {
                AtomicInteger atomicCounter = nameCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(1));
                int counter = atomicCounter.getAndIncrement();
                String anonymousName = "익명" + counter;

                ParticipantInfo created = ParticipantInfo.create(sessionId, memberId, socketId, anonymousName);

                log.info("[BlindDate] Participant added: sessionId={}, memberId={}, socketId={}, name={}",
                        sessionId, memberId, socketId, anonymousName);

                return created;
            }

            // 참여중인 경우
            // 같은 세션이면 소켓만 추가
            if (existing.getSessionId().equals(sessionId)) {
                existing.addSocket(socketId);
                log.info("[BlindDate] Socket added to existing participant: memberId={}, socketId={}, totalSockets={}",
                        memberId, socketId, existing.getSocketIds().size());

                return existing;
            }

            // 다른 세션에 이미 참여 중
            throw new IllegalStateException(
                    String.format("[BlindDate] Member %d already in session %s, cannot join session %s",
                            memberId, existing.getSessionId(), sessionId));
        });

        // socketId -> memberId 인덱스 갱신 (compute가 예외 없이 끝난 경우에만 도달)
        socketIdToMemberId.put(socketId, memberId);

        return participant;
    }

    /**
     * 소켓 제거 (연결 해제). 참가자 제거 여부는 세션 상태를 아는 호출자가 결정한다.
     */
    public boolean removeSocket(String socketId) throws IllegalArgumentException {
        // socketId -> memberId 인덱스로 O(1) 조회
        Long memberId = socketIdToMemberId.remove(socketId);
        ParticipantInfo participant = memberId != null ? participants.get(memberId) : null;

        if (participant == null) {
            log.warn("[BlindDate] Participant not found for socketId: {}", socketId);
            throw new IllegalArgumentException("[BlindDate] Participant not found for socketId: " + socketId);
        }

        // 소켓 제거
        boolean removed = participant.removeSocket(socketId);
        if (!removed) {
            log.warn("[BlindDate] Participant doesn't have this socket: {}", socketId);
            throw new IllegalArgumentException("[BlindDate]  Participant doesn't have this socket");
        }

        log.info("[BlindDate] Socket removed: memberId={}, socketId={}, remainingSockets={}",
                participant.getMemberId(), socketId, participant.getSocketIds().size());

        return participant.hasNoSockets();
    }

    /**
     * 회원 ID로 참여 정보 조회
     */
    public ParticipantInfo getByMemberId(Long memberId) {
        return participants.get(memberId);
    }

    /**
     * 소켓 ID로 참여 정보 조회
     */
    public ParticipantInfo getBySocketId(String socketId) {
        Long memberId = socketIdToMemberId.get(socketId);
        return memberId != null ? participants.get(memberId) : null;
    }

    /**
     * 세션의 참여자 수 조회
     */
    public List<ParticipantInfo> findAllBySessionId(String sessionId) {
        return participants.values().stream()
                .filter(p -> p.getSessionId().equals(sessionId))
                .toList();
    }

    /**
     * 세션의 참여자 ID와 이름 Map
     */
    public Map<Long, String> getParticipantsIdAndName(String sessionId) {
        return participants.values().stream()
                .filter(p -> p.getSessionId().equals(sessionId))
                .collect(Collectors.toMap(
                        ParticipantInfo::getMemberId,
                        ParticipantInfo::getAnonymousName,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public void openChoices(String sessionId) {
        choiceRounds.computeIfAbsent(sessionId,
                id -> new ChoiceRound(getParticipantsIdAndName(id).keySet()));
    }

    @Override
    public boolean recordChoice(String sessionId, Long choicerId, Long targetId) {
        ChoiceRound round = choiceRounds.get(sessionId);
        if (round == null) {
            return false;
        }
        synchronized (round) {
            validateChoice(round, choicerId, targetId);
            if (round.responses.containsKey(choicerId)) {
                return false;
            }
            // HashMap은 null을 지원한다. containsKey로 미선택과 미응답을 구분한다.
            round.responses.put(choicerId, targetId);
            return true;
        }
    }

    @Override
    public Long claimMutualChoice(String sessionId, Long memberId) {
        ChoiceRound round = choiceRounds.get(sessionId);
        if (round == null) {
            return null;
        }
        synchronized (round) {
            Long targetId = round.responses.get(memberId);
            if (targetId == null || !memberId.equals(round.responses.get(targetId))
                    || round.claimedMembers.contains(memberId) || round.claimedMembers.contains(targetId)) {
                return null;
            }
            // 채팅방 생성 실패 여부와 무관하게 동일 쌍을 다시 실행하지 않는다.
            round.claimedMembers.add(memberId);
            round.claimedMembers.add(targetId);
            return targetId;
        }
    }

    private void validateChoice(ChoiceRound round, Long choicerId, Long targetId) {
        if (choicerId == null || !round.participantIds.contains(choicerId)) {
            throw new InvalidBlindDateChoiceException(
                    403, "CHOICE_FORBIDDEN", "해당 세션의 참가자만 선택할 수 있습니다.");
        }
        if (targetId == null) {
            return;
        }
        if (choicerId.equals(targetId)) {
            throw new InvalidBlindDateChoiceException(
                    400, "INVALID_CHOICE", "자신을 선택할 수 없습니다.");
        }
        if (!round.participantIds.contains(targetId)) {
            throw new InvalidBlindDateChoiceException(
                    404, "CHOICE_TARGET_NOT_FOUND", "해당 세션에서 선택 대상을 찾을 수 없습니다.");
        }
    }

    @Override
    public Map<Long, Long> getChoices(String sessionId) {
        ChoiceRound round = choiceRounds.get(sessionId);
        if (round == null) {
            return Map.of();
        }
        synchronized (round) {
            return Collections.unmodifiableMap(new HashMap<>(round.responses));
        }
    }

    /** 다른 참가자의 응답과 무관하게 접수된 최종 선택으로 상호 선택을 판정한다. */
    public boolean isMatched(String sessionId, Long memberId) {
        Map<Long, Long> choices = getChoices(sessionId);
        Long targetId = choices.get(memberId);
        return targetId != null && memberId.equals(choices.get(targetId));
    }

    private static final class ChoiceRound {
        private final Set<Long> participantIds;
        private final Map<Long, Long> responses = new HashMap<>();
        private final Set<Long> claimedMembers = new HashSet<>();

        private ChoiceRound(Set<Long> participantIds) {
            this.participantIds = Set.copyOf(participantIds);
        }
    }

    /**
     * 전체 데이터 초기화
     */
    public synchronized void clear() {
        participants.clear();
        socketIdToMemberId.clear();
        nameCounters.clear();
        choiceRounds.clear();

        log.info("[BlindDate] All participant data cleared");
    }

    /**
     * 참여자 제거 (회원 ID로)
     */
    public synchronized void removeParticipant(Long memberId) {
        ParticipantInfo participant = participants.remove(memberId);
        if (participant != null) {
            participant.getSocketIds().forEach(socketIdToMemberId::remove);
            log.info("Participant removed: memberId={}, sessionId={}", memberId, participant.getSessionId());
        }
    }

    /**
     * 익명 이름 조회
     */
    public String getAnonymousName(Long memberId) {
        ParticipantInfo participant = participants.get(memberId);
        return participant != null ? participant.getAnonymousName() : null;
    }


    /**
     * 매칭되지 않은 멤버 조회
     */
    public Set<Long> getNotMatched(String sessionId) {
        Set<Long> allMemberIds = participants.values().stream()
                .filter(p -> p.getSessionId().equals(sessionId))
                .map(ParticipantInfo::getMemberId)
                .collect(Collectors.toSet());

        allMemberIds.removeIf(memberId -> isMatched(sessionId, memberId));

        return allMemberIds;
    }
}
