package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.notice.exception.NoticeLinkNotAvailableException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NoticeLinkParser {

    @Value("${notice.link.layout-header}")
    private String layoutHeader;

    @Value("${notice.link.department.notice-regex}")
    private String departmentNoticeRegex;

    @Value("${notice.link.department.url-prefix}")
    private String departmentUrlPrefix;

    @Value("${notice.link.department.url-start}")
    private String departmentUrlStart;

    @Value("${notice.link.department.url-suffix}")
    private String departmentUrlSuffix;

    public String parse(String link) {
        Pattern pattern = Pattern.compile(departmentNoticeRegex);
        Matcher matcher = pattern.matcher(link);

        if (!link.startsWith(departmentUrlStart)) {
            return parseUniversity(link);
        }

        String parsedLink = parseDepartment(matcher);
        if (parsedLink == null) {
            throw new NoticeLinkNotAvailableException(link);
        }

        return parsedLink;
    }

    private String parseUniversity(String link) {
        return link + layoutHeader;
    }

    private String parseDepartment(Matcher matcher) {
        StringBuilder linkBuilder = new StringBuilder();
        linkBuilder.append(departmentUrlPrefix);

        if (!matcher.find()) {
            return null;
        }

        do {
            String match = matcher.group(1);
            linkBuilder.append("/").append(match);
        } while (matcher.find());

        linkBuilder.append(departmentUrlSuffix);
        linkBuilder.append(layoutHeader);

        return linkBuilder.toString();
    }
}
