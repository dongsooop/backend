package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.DepartmentType;
import com.dongsoop.dongsoop.exception.domain.jsoup.JsoupConnectionFailedException;
import com.dongsoop.dongsoop.notice.dto.NoticeMaxIdByType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.util.NoticeParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    @Value("${university.domain}")
    private String universityUrl;

    private final NoticeParser noticeParser;

    private final NoticeRepository noticeRepository;

    //    @Scheduled(cron = "0 0 10,14,18 * * *")
    public void scheduled() {
        List<NoticeMaxIdByType> noticeMaxIdList = noticeRepository.findMaxIdGroupByType();
        Map<DepartmentType, Long> noticeMaxIdMap = transformNoticeMaxIdListToMap(noticeMaxIdList);

        List<Notice> noticeList = new ArrayList<>();

        Stream<DepartmentType> stream = Arrays.stream(DepartmentType.values());

        // 각 학과를 반복하며 최신 공지사항을 업데이트
        stream.forEach(departmentType -> {
            try {
                Long maxId = noticeMaxIdMap.getOrDefault(departmentType, 0L);
                List<Notice> newNoticeList = getNewNotice(departmentType, maxId);
                noticeList.addAll(newNoticeList);
            } catch (IOException e) {
                throw new JsoupConnectionFailedException();
            }
        });

        noticeRepository.saveAll(noticeList);
    }

    private Map<DepartmentType, Long> transformNoticeMaxIdListToMap(List<NoticeMaxIdByType> noticeMaxIdList) {
        Map<DepartmentType, Long> noticeMaxIdMap = new HashMap<>();

        // { type: maxId } 형태로 변환
        noticeMaxIdList.forEach(noticeMaxId -> noticeMaxIdMap.put(noticeMaxId.getType(), noticeMaxId.getMaxId()));
        return noticeMaxIdMap;
    }

    private List<Notice> getNewNotice(DepartmentType type, Long maxId) throws IOException {
        Connection connect = Jsoup.connect(universityUrl + type.getNoticeUrl());
        Document document = connect.get();

        Elements rows = document.select("tbody tr");
        System.out.println(rows);

        List<Notice> noticeList = rows.stream()
                .map(notice -> noticeParser.parse(notice, type))
                .toList();

        // 공지 반환
        return noticeList.stream()
                .filter(Objects::nonNull)
                .filter(notice -> notice.getId() > maxId)
                .toList();
    }

}
