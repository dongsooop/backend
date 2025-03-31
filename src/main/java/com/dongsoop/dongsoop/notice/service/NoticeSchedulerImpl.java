package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.Department;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.jsoup.JsoupConnectionFailedException;
import com.dongsoop.dongsoop.notice.dto.NoticeMaxIdByType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.util.NoticeParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeSchedulerImpl implements NoticeScheduler {

    @Value("${university.domain}")
    private String universityUrl;

    private final NoticeParser noticeParser;

    private final NoticeRepository noticeRepository;

    private final NoticeDetailsRepository noticeDetailsRepository;

    private final DepartmentRepository departmentRepository;

    private static final Logger logger = LoggerFactory.getLogger(NoticeSchedulerImpl.class);

    public void scheduled() {
        // 학과별 최신 공지 번호(가장 높은 번호) 가져오기
        List<NoticeMaxIdByType> noticeMaxIdList = noticeRepository.findMaxIdGroupByType();
        Map<Department, Long> noticeMaxIdMap = transformNoticeMaxIdListToMap(noticeMaxIdList);

        // 저장할 최신 공지사항 리스트 변수 초기화
        List<Notice> noticeList = new ArrayList<>();
        Set<NoticeDetails> noticeDetailsSet = new HashSet<>();

        // 학과 전체 가져오기
        List<Department> departmentList = departmentRepository.findAll();

        departmentList.forEach(department ->
                updateNotices(department, noticeMaxIdMap, noticeList, noticeDetailsSet));

        noticeDetailsRepository.saveAll(noticeDetailsSet);
        noticeRepository.saveAll(noticeList);
    }

    private void updateNotices(Department department, Map<Department, Long> noticeMaxIdMap, List<Notice> noticeList,
                               Set<NoticeDetails> noticeDetailsSet) {
        // 학과 최신 공지 번호(가장 높은 번호)
        Long maxId = noticeMaxIdMap.getOrDefault(department, 0L);

        // 최신 공지 번호보다 높은 번호의 공지 상세 목록 가져오기
        Set<NoticeDetails> newNoticeDetailsSet = getNewNoticeDetailsSet(maxId, department);
        List<Notice> newNoticeList = newNoticeDetailsSet.stream()
                .map(noticeDetails -> new Notice(department, noticeDetails))
                .toList();

        // 최신화
        noticeDetailsSet.addAll(newNoticeDetailsSet);
        noticeList.addAll(newNoticeList);
    }

    private Set<NoticeDetails> getNewNoticeDetailsSet(Long maxId, Department department) {
        try {
            // 최신 공지사항 파싱
            return new HashSet<>(parseNewNotice(department, maxId));
        } catch (IOException e) {
            logger.error("notice parsing failed: ", e);
            throw new JsoupConnectionFailedException();
        }
    }

    private Map<Department, Long> transformNoticeMaxIdListToMap(List<NoticeMaxIdByType> noticeMaxIdList) {
        Map<Department, Long> noticeMaxIdMap = new HashMap<>();

        // { type: maxId } 형태로 변환
        noticeMaxIdList.forEach(noticeMaxId -> noticeMaxIdMap.put(noticeMaxId.getDepartment(), noticeMaxId.getMaxId()));
        return noticeMaxIdMap;
    }

    private List<NoticeDetails> parseNewNotice(Department department, Long maxId) throws IOException {
        Connection connect = Jsoup.connect(universityUrl + department.getNoticeUrl());
        Document document = connect.get();

        Elements rows = document.select("tbody tr");

        List<NoticeDetails> noticeDetailsList = rows.stream()
                .map(noticeParser::parse)
                .toList();

        // 공지 반환
        return noticeDetailsList.stream()
                .filter(Objects::nonNull)
                .filter(noticeDetails -> noticeDetails.getId() > maxId)
                .toList();
    }

}
