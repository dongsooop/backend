package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import java.util.Collections;
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

    // sessionId -> (choicerId -> targetId)
    private final Map<String, Map<Long, Long>> choices = new ConcurrentHashMap<>();

    // sessionId -> Set<matchedMemberId>
    private final Map<String, Set<Long>> matches = new ConcurrentHashMap<>();

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
     * 소켓 제거 (연결 해제) 모든 소켓이 제거되면 참여자도 제거
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

        // 모든 소켓이 제거되면 참여자도 제거
        if (participant.hasNoSockets()) {
            participants.remove(participant.getMemberId());
            log.info("Participant fully removed: memberId={}, sessionId={}",
                    participant.getMemberId(), participant.getSessionId());

            return true;
        }

        return false;
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

    /**
     * 선택 기록
     */
    public boolean recordChoice(String sessionId, Long choicerId, Long targetId) {
        Map<Long, Long> sessionChoices = choices.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());

        // 이미 선택했는지 확인 + 반영을 원자적으로 처리 (확인과 반영 사이 공백 제거)
        if (sessionChoices.putIfAbsent(choicerId, targetId) != null) {
            log.warn("Already chosen: sessionId={}, choicerId={}", sessionId, choicerId);
            return false;
        }

        log.info("Choice recorded: sessionId={}, choicerId={} -> targetId={}", sessionId, choicerId, targetId);

        // 매칭 확인
        Long reverseChoice = sessionChoices.get(targetId);
        // 상대가 아직 선택하지 않았거나, 다른 사람을 선택한 경우
        if (reverseChoice == null || !reverseChoice.equals(choicerId)) {
            return false;
        }

        // 서로 선택함 → 매칭 성공
        Set<Long> sessionMatches = matches.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet());
        sessionMatches.add(choicerId);
        sessionMatches.add(targetId);

        log.info("🎉 Match found: sessionId={}, member1={}, member2={}", sessionId, choicerId, targetId);
        return true;
    }

    /**
     * 매칭 확인
     */
    public boolean isMatched(String sessionId, Long memberId) {
        Set<Long> sessionMatches = matches.get(sessionId);
        return sessionMatches != null && sessionMatches.contains(memberId);
    }

    /**
     * 전체 데이터 초기화
     */
    public synchronized void clear() {
        participants.clear();
        socketIdToMemberId.clear();
        nameCounters.clear();
        choices.clear();
        matches.clear();

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

        Set<Long> matched = matches.getOrDefault(sessionId, Collections.emptySet());
        allMemberIds.removeAll(matched);

        return allMemberIds;
    }
}
