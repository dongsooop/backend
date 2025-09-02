package com.dongsoop.dongsoop.timetable.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.OverlapTimetable;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.dto.UpdateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.YearSemester;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.entity.Timetable;
import com.dongsoop.dongsoop.timetable.exception.TimetableNotFoundException;
import com.dongsoop.dongsoop.timetable.exception.TimetableNotOwnedException;
import com.dongsoop.dongsoop.timetable.exception.TimetableOverlapException;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;

    private final MemberService memberService;

    private final TimetableMapper timetableMapper;

    public void createTimetable(CreateTimetableRequest request) {
        Timetable timetable = timetableMapper.toEntity(request);

        validateOverlapTimetable(request);

        timetableRepository.save(timetable);
    }

    @Override
    @Transactional
    public List<CreateTimetableRequest> createTimetable(List<CreateTimetableRequest> request) {
        List<CreateTimetableRequest> failedRequests = new ArrayList<>();
        List<CreateTimetableRequest> successRequests = new ArrayList<>();

        for (CreateTimetableRequest r : request) {
            try {
                validateOverlapTimetable(r);
                successRequests.add(r);
            } catch (TimetableOverlapException e) {
                failedRequests.add(r);
            }
        }

        List<Timetable> timetableList = successRequests.stream()
                .map(timetableMapper::toEntity)
                .toList();

        timetableRepository.saveAll(timetableList);

        return failedRequests;
    }

    public List<TimetableView> getTimetableView(Year year, SemesterType semester) {
        Member referenceMember = memberService.getMemberReferenceByContext();
        return timetableRepository.findAllByMemberAndYearAndSemester(referenceMember,
                year, semester);
    }

    private void validateOverlapTimetable(CreateTimetableRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();

        Optional<OverlapTimetable> optionalOverlapTimetable = timetableRepository.findOverlapWithinRange(
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

    public void deleteTimetable(Long timetableId) {
        validateOwner(timetableId);
        timetableRepository.deleteById(timetableId);
    }

    public void updateTimetable(UpdateTimetableRequest request) {
        validateOwner(request.id());

        // 업데이트 시간과 겹치는 시간이 있는지
        YearSemester yearSemester = timetableRepository.findYearSemesterById(request.id());
        validateOverlapTimetable(request, yearSemester.getYear(), yearSemester.getSemester());

        Timetable timetable = timetableRepository.findById(request.id())
                .orElseThrow(() -> new TimetableNotFoundException(request.id()));

        timetable.update(request);

        timetableRepository.save(timetable);
    }

    private void validateOwner(Long timetableId) {
        Long memberId = memberService.getMemberIdByAuthentication();
        boolean isRequesterIsOwner = timetableRepository.existsByIdAndMemberId(timetableId, memberId);
        if (!isRequesterIsOwner) { // 요청자가 소유자가 아닌 경우
            throw new TimetableNotOwnedException(timetableId, memberId);
        }
    }

    public void validateOverlapTimetable(UpdateTimetableRequest request, Year year, SemesterType semester) {

        Long memberId = memberService.getMemberIdByAuthentication();

        Optional<OverlapTimetable> optionalOverlapTimetable = timetableRepository.findOverlapWithinRangeExcludingSelf(
                request.id(),
                memberId,
                year,
                semester,
                request.week(),
                request.startAt(),
                request.endAt());

        optionalOverlapTimetable.ifPresent(overlapTimetable -> {
            throw new TimetableOverlapException(request.startAt(), request.endAt(), overlapTimetable.startAt(),
                    overlapTimetable.endAt());
        });
    }

    public void deleteTimetable(Year year, SemesterType semester) {
        Long memberId = memberService.getMemberIdByAuthentication();

        timetableRepository.deleteByMemberIdAndYearAndSemester(memberId, year, semester);
    }
}
