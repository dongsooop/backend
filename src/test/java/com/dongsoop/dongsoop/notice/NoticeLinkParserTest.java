package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.notice.util.NoticeLinkParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = NoticeLinkParser.class)
class NoticeLinkParserTest {

    @Autowired
    NoticeLinkParser noticeLinkParser;

    @Value("${notice.link.layout-header}")
    String layoutHeader;

    @Test
    void parse_university_notice_link_to_available_link() {
        String universityNoticeLink = "/bbs/dmu/677/248653/artclView.do";

        String parseResult = noticeLinkParser.parse(universityNoticeLink);
        assertThat(parseResult)
                .isEqualTo("/bbs/dmu/677/248653/artclView.do" + layoutHeader);
    }

    @Test
    void parse_department_notice_link_to_available_link() {
        String departmentNoticeLink = "javascript:jf_combBbs_view('dmu','98','292','248477');";

        String parseResult = noticeLinkParser.parse(departmentNoticeLink);
        assertThat(parseResult)
                .isEqualTo("/combBbs/dmu/98/292/248477/view.do" + layoutHeader);
    }
}
