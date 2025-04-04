package com.dongsoop.dongsoop.notice.util;

import com.dongsoop.dongsoop.exception.domain.parser.NoticeLinkNotAvailableException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NoticeLinkParser {

    @Value("${parser.link.layout-header}")
    private String layoutHeader;

    @Value("${parser.link.department.notice-regex}")
    private String departmentNoticeRegex;

    @Value("${parser.link.department.url-prefix}")
    private String departmentUrlPrefix;

    @Value("${parser.link.department.url-start}")
    private String departmentUrlStart;

    @Value("${parser.link.department.url-suffix}")
    private String departmentUrlSuffix;

    public String parse(String link) {
        Pattern pattern = Pattern.compile(departmentNoticeRegex);
        Matcher matcher = pattern.matcher(link);

        if (link.startsWith(departmentUrlStart)) {
            return parseDepartment(matcher);
        }

        return parseUniversity(link);
    }

    private String parseUniversity(String link) {
        return link + layoutHeader;
    }

    private String parseDepartment(Matcher matcher) {
        StringBuilder linkBuilder = new StringBuilder();
        linkBuilder.append(departmentUrlPrefix);

        if (matcher.group(1) == null) {
            throw new NoticeLinkNotAvailableException();
        }

        while (matcher.find()) {
            String match = matcher.group(1);
            linkBuilder.append("/").append(match);
        }

        linkBuilder.append(departmentUrlSuffix);

        return linkBuilder.toString();
    }
}
