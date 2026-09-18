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
        Set<NoticeDetails> newNoticeDetailsSet = crawlNoticePage(department, 1).stream()
                .filter(noticeDetails -> noticeDetails.getId() > recentlyNoticeId)
                .collect(Collectors.toSet());
        List<Notice> newNoticeList = newNoticeDetailsSet.stream()
                .map(noticeDetails -> new Notice(department, noticeDetails))
                .toList();

        log.info("Updated notices for department: {}, new count: {}", department.getId().name(),
                newNoticeDetailsSet.size());

        return new CrawledNotice(newNoticeDetailsSet, newNoticeList);
    }

    public List<NoticeDetails> crawlNoticePage(Department department, int page) {
        try {
            String noticeUrl = department.getNoticeUrl();
            String separator = noticeUrl.contains("?") ? "&" : "?";
            URL url = new URL(this.universityUrl, noticeUrl + separator + "page=" + page);

            Document document = Jsoup.connect(url.toExternalForm())
                    .timeout(timeout) // 연결 타임 아웃 설정
                    .userAgent(userAgent)
                    .followRedirects(true) // 리다이렉트 시 해당 페이지 크롤링
                    .get();

            Elements rows = document.select("tbody tr");

            return rows.stream()
                    .map(noticeParser::parse)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new NoticeParsingException(department, page, e);
        }
    }
}
