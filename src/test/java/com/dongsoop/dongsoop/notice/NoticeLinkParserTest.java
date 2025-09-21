package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.notice.util.NoticeLinkParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeLinkParserTest {

    @InjectMocks
    NoticeLinkParser noticeLinkParser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noticeLinkParser, "layoutHeader", "?");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentNoticeRegex", "'([^' ]*)'");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlPrefix", "/combBbs");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlStart", "javascript");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlSuffix", "/view.do");
    }

    @Test
    void parse_university_notice_link_to_available_link() {
        String universityNoticeLink = "/bbs/dmu/677/248653/artclView.do";

        String parseResult = noticeLinkParser.parse(universityNoticeLink);
        assertThat(parseResult)
                .isEqualTo("/bbs/dmu/677/248653/artclView.do" + "?");
    }

    @Test
    void parse_department_notice_link_to_available_link() {
        String departmentNoticeLink = "javascript:jf_combBbs_view('dmu','98','292','248477');";

        String parseResult = noticeLinkParser.parse(departmentNoticeLink);
        assertThat(parseResult)
                .isEqualTo("/combBbs/dmu/98/292/248477/view.do" + "?");
    }
}
