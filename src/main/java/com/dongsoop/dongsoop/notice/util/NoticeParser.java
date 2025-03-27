package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.department.DepartmentType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class NoticeParser {

    public Notice parse(Element row, DepartmentType departmentType) {
        if (!isNoticeRow(row)) {
            return null;
        }

        Long contentNumber = parseNoticeNumber(row);
        String title = parseTitle(row);
        String author = parseAuthor(row);
        LocalDate date = parseDate(row);
        String link = parseLink(row);
        System.out.println("link: " + link);
        System.out.println("title: " + title);

        return new Notice(contentNumber, author, title, departmentType, date, link);
    }

    public boolean isNoticeRow(Element row) {
        Element numberElement = row.getElementsByClass("td-num")
                .first();

        return row.tagName().equals("tr")
                && numberElement != null
                && numberElement.text().matches("\\d+");
    }

    public Long parseNoticeNumber(Element row) {
        String contentNumber = parseTextByClass(row, "td-num");
        return Long.parseLong(contentNumber);
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

        String regex = "javascript:jf_combBbs_view\\('([^']*)',(.*)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(linkElement.attr("href"));

        System.out.println(matcher.group(2));
        System.out.println(matcher.groupCount());
        if (!matcher.find()) {
            return "";
        }

        StringBuilder link = new StringBuilder();
        link.append("/").append(matcher.group(1)); // 첫 번째 값 (dmu)

        while (matcher.find(2)) {  // group(2)부터 반복
            link.append("/").append(matcher.group(2));
        }

        return link.toString();
    }

    public String parseAuthor(Element row) {
        return parseTextByClass(row, "td-write");
    }

    public LocalDate parseDate(Element row) {
        String date = parseTextByClass(row, "td-date");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return LocalDate.parse(date, dateTimeFormatter);
    }

    private String parseTextByClass(Element row, String className) {
        Element element = row.getElementsByClass(className)
                .first();
        if (element == null) {
            return "";
        }

        return element.text();

    }

}
