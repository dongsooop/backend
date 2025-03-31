package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeParser {

    private final NoticeLinkParser noticeLinkParser;

    private static final Integer DEPARTMENT_NOTICE_ID_INDEX = 5;
    private static final Integer UNIVERSITY_NOTICE_ID_INDEX = 4;

    public NoticeDetails parse(Element row) {
        if (!isNoticeRow(row)) {
            return null;
        }

        String title = parseTitle(row);
        String author = parseWriter(row);
        LocalDate date = parseDate(row);
        String link = parseLink(row);
        Long contentNumber = parseNoticeNumber(link);

        return new NoticeDetails(contentNumber, author, title, link, date);
    }

    public boolean isNoticeRow(Element row) {
        Element numberElement = row.getElementsByClass("td-num")
                .first();

        return row.tagName().equals("tr")
                && numberElement != null
                && numberElement.text().matches("\\d+");
    }

    public Long parseNoticeNumber(String link) {
        String[] splitLink = link.split("/");

        if (splitLink.length == 7) {
            return Long.parseLong(splitLink[DEPARTMENT_NOTICE_ID_INDEX]);
        }

        if (splitLink.length == 6) {
            return Long.parseLong(splitLink[UNIVERSITY_NOTICE_ID_INDEX]);
        }

        throw new IllegalArgumentException("공지사항 경로가 올바르지 않습니다: " + link);
    }

    public String parseTitle(Element row) {
        return parseTextByClass(row, "td-subject");
    }

    public String parseLink(Element row) {
        Element titleElement = row.getElementsByClass("td-subject")
                .first();
        if (titleElement == null) {
            return "";
        }

        Element linkElement = titleElement.getElementsByTag("a")
                .first();
        if (linkElement == null) {
            return "";
        }

        String link = linkElement.attr("href");
        return noticeLinkParser.parse(link);
    }

    public String parseWriter(Element row) {
        return parseTextByClass(row, "td-write");
    }

    public LocalDate parseDate(Element row) {
        String date = parseTextByClass(row, "td-date");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return LocalDate.parse(date, dateTimeFormatter);
    }

    private String parseTextByClass(Element row, String className) {
        Elements element = row.getElementsByClass(className);

        return element.text();
    }

}
