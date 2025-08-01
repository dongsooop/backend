package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.dto.CrawledNotice;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.exception.NoticeParsingException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NoticeCrawl {

    private final NoticeParser noticeParser;

    @Value("${university.domain}")
    private URL universityUrl;

    @Value("${notice.connect.timeout}")
    private Integer timeout;

    @Value("${notice.connect.user-agent}")
    private String userAgent;

    /**
     * 학과 공지사항을 크롤링하여 최신 공지 목록을 반환
     *
     * @param department       크롤링 하려는 학과
     * @param recentlyNoticeId DB에 저장된 최신 공지 ID
     * @return 크롤링된 공지 정보
     */
    public CrawledNotice crawlNewNotices(Department department, Long recentlyNoticeId) {
        // 최신 공지 번호보다 높은 번호의 공지 상세 목록 가져오기
        Set<NoticeDetails> newNoticeDetailsSet = parseNewNotice(department, recentlyNoticeId);
        List<Notice> newNoticeList = newNoticeDetailsSet.stream()
                .map(noticeDetails -> new Notice(department, noticeDetails))
                .toList();

        log.info("Updated notices for department: {}, new count: {}", department.getId().name(),
                newNoticeDetailsSet.size());

        return new CrawledNotice(newNoticeDetailsSet, newNoticeList);
    }

    /**
     * 학과 공지사항을 파싱하여 최신 공지 목록을 반환
     *
     * @param department       파싱하려는 학과
     * @param recentlyNoticeId DB에 저장된 최신 공지 ID
     * @return 공지 세부 정보가 담긴 목록 반환
     * @throws NoticeParsingException 공지 파싱 중 예외 발생 시
     */
    private Set<NoticeDetails> parseNewNotice(Department department, Long recentlyNoticeId) {
        try {
            URL url = new URL(this.universityUrl, department.getNoticeUrl());

            Document document = Jsoup.connect(url.toExternalForm())
                    .timeout(timeout) // 연결 타임 아웃 설정
                    .userAgent(userAgent)
                    .followRedirects(true) // 리다이렉트 시 해당 페이지 크롤링
                    .get();

            Elements rows = document.select("tbody tr");

            List<NoticeDetails> noticeDetailsList = rows.stream()
                    .map(noticeParser::parse)
                    .toList();

            // 공지 반환
            return noticeDetailsList.stream()
                    .filter(Objects::nonNull)
                    .filter(noticeDetails -> noticeDetails.getId() > recentlyNoticeId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new NoticeParsingException(department, recentlyNoticeId, e);
        }
    }
}
