package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.exception.domain.notice.NoticeLinkFormatNotAvailable;
import com.dongsoop.dongsoop.exception.domain.notice.NoticeSubjectNotAvailableException;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoticeParser {

    private static final Pattern DEPARTMENT_NOTICE_LINK_PATTERN = Pattern.compile(
            "^/combBbs/dmu/\\d+/\\d+/(\\d+)/view.do$");
    private static final Pattern UNIVERSITY_NOTICE_LINK_PATTERN = Pattern.compile(
            "^/bbs/dmu/\\d+/(\\d+)/artclView.do\\?layout=unknown$");
    private final NoticeLinkParser noticeLinkParser;

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
        Long noticeNumberByDepartmentLink = parseLinkByRegex(link, DEPARTMENT_NOTICE_LINK_PATTERN);
        if (noticeNumberByDepartmentLink != null) {
            return noticeNumberByDepartmentLink;
        }

        Long noticeNumberByUniversityLink = parseLinkByRegex(link, UNIVERSITY_NOTICE_LINK_PATTERN);
        if (noticeNumberByUniversityLink != null) {
            return noticeNumberByUniversityLink;
        }

        throw new NoticeLinkFormatNotAvailable(link);
    }

    private Long parseLinkByRegex(String link, Pattern pattern) {
        Matcher matcher = pattern.matcher(link);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            log.error("공지 번호 파싱 중 숫자가 아닌 오류: {}", link, e);
            return null;
        }
    }

    public String parseTitle(Element row) {
        Element subjectElement = row.getElementsByClass("td-subject")
                .first();

        if (subjectElement == null) {
            throw new NoticeSubjectNotAvailableException();
        }

        Elements subjectTextElement = subjectElement.getElementsByTag("strong");

        // strong 태그가 없으면 td-subject 내용 전체 반환
        if (subjectTextElement.isEmpty()) {
            return subjectElement.text();
        }

        // strong 태그가 있으면 strong 태그 내용만 반환
        Element textElement = subjectTextElement.first();
        if (textElement == null) {
            throw new NoticeSubjectNotAvailableException();
        }

        return textElement.text();
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
