package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.exception.NoticeParsingException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
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
     * 지정한 학과의 공지 목록 페이지를 크롤링한다.
     *
     * @param department 크롤링할 학과
     * @param page 크롤링할 페이지 번호
     * @return 페이지에서 파싱한 공지 목록
     */
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
