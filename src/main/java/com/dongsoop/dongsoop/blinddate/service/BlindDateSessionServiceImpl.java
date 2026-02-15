package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlindDateSessionServiceImpl implements BlindDateSessionService {

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateStorage blindDateStorage;

    public boolean isSessionFull(String sessionId) {
        List<ParticipantInfo> participantInfos = participantStorage.findAllBySessionId(sessionId);
        Integer maxCount = blindDateStorage.getMaxSessionMemberCount();
        if (maxCount == null) {
            return false;
        }
        return participantInfos.size() >= maxCount;
    }
}
