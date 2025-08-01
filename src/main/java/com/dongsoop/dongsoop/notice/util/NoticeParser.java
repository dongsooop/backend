package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.exception.NoticeLinkFormatNotAvailable;
import com.dongsoop.dongsoop.notice.exception.NoticeSubjectNotAvailableException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoticeParser {

    private final String TD_SUBJECT_SELECTOR;
    private final Pattern DEPARTMENT_NOTICE_LINK_PATTERN;
    private final Pattern UNIVERSITY_NOTICE_LINK_PATTERN;
    private final NoticeLinkParser noticeLinkParser;

    public NoticeParser(NoticeLinkParser noticeLinkParser, @Value("${notice.link.layout-header}") String layoutHeader) {
        this.noticeLinkParser = noticeLinkParser;
        this.TD_SUBJECT_SELECTOR = ".td-subject";
        this.DEPARTMENT_NOTICE_LINK_PATTERN = Pattern.compile(
                "^/combBbs/dmu/\\d+/\\d+/(\\d+)/view.do\\" + layoutHeader + "$");
        this.UNIVERSITY_NOTICE_LINK_PATTERN = Pattern.compile(
                "^/bbs/dmu/\\d+/(\\d+)/artclView.do\\" + layoutHeader + "$");
    }

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
        Element subjectElement = row.selectFirst(TD_SUBJECT_SELECTOR);

        if (subjectElement == null) {
            throw new NoticeSubjectNotAvailableException();
        }

        Element subjectTextElement = subjectElement.selectFirst("strong");

        // strong 클래스가 있다면 반환하고 없다면 subject 내용 전체 반환
        return Objects.requireNonNullElse(subjectTextElement, subjectElement).text();
    }

    public String parseLink(Element row) {
        Element titleElement = row.selectFirst(TD_SUBJECT_SELECTOR);
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
