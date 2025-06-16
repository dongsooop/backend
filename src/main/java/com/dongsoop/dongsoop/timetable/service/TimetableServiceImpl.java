package com.dongsoop.dongsoop.timetable.service;

import com.dongsoop.dongsoop.exception.domain.timetable.TimetableOverlapException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.OverlapTimetable;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.entity.Timetable;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepositoryCustom;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;

    private final TimetableRepositoryCustom timetableRepositoryCustom;

    private final MemberService memberService;

    private final TimetableMapper timetableMapper;

    public void createTimetable(CreateTimetableRequest request) {
        Timetable timetable = timetableMapper.toEntity(request);

        validateOverlapTimetable(request);

        timetableRepository.save(timetable);
    }

    public List<TimetableView> getTimetableView(Year year, SemesterType semester) {
        Member referenceMember = memberService.getMemberReferenceByContext();
        return timetableRepository.findAllByMemberAndYearAndSemester(referenceMember,
                year, semester);
    }

    private void validateOverlapTimetable(CreateTimetableRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();

        Optional<OverlapTimetable> optionalOverlapTimetable = timetableRepositoryCustom.findOverlapWithinRange(
                memberId,
                request.year(),
                request.semester(),
                request.week(),
                request.startAt(),
                request.endAt());

        optionalOverlapTimetable.ifPresent(overlapTimetable -> {
            throw new TimetableOverlapException(request.startAt(), request.endAt(), overlapTimetable.startAt(),
                    overlapTimetable.endAt());
        });
    }
}
