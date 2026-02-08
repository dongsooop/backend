package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlindDateSessionServiceImpl implements BlindDateSessionService {

    private final ParticipantInfoRepository participantInfoRepository;
    private final BlindDateInfoRepositoryImpl blindDateInfoRepository;

    public boolean isSessionFull(String sessionId) {
        List<ParticipantInfo> participantInfos = participantInfoRepository.findAllBySessionId(sessionId);
        int maxCount = blindDateInfoRepository.getMaxSessionMemberCount();
        return participantInfos.size() >= maxCount;
    }
}
