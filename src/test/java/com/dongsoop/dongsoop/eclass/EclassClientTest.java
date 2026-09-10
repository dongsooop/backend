package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dongsoop.dongsoop.eclass.dto.MoodleAssignment;
import com.dongsoop.dongsoop.eclass.dto.MoodleSiteInfoResponse;
import com.dongsoop.dongsoop.eclass.exception.EclassApiException;
import com.dongsoop.dongsoop.eclass.exception.EclassInvalidTokenException;
import com.dongsoop.dongsoop.eclass.util.EclassClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class EclassClientTest {

    private static final String BASE = "https://eclass.test";
    private static final String WS = BASE + "/webservice/rest/server.php";

    private MockRestServiceServer server;
    private EclassClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new EclassClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "baseUrl", BASE);
        ReflectionTestUtils.setField(client, "userAgent", "test-agent");
    }

    private String fixture(String name) throws IOException {
        return new ClassPathResource("eclass/" + name).getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("site_info 응답에서 userid와 fullname을 읽는다")
    void getSiteInfo() throws IOException {
        server.expect(requestTo(WS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(Matchers.containsString("wsfunction=core_webservice_get_site_info")))
                .andExpect(content().string(Matchers.containsString("wstoken=tok")))
                .andRespond(withSuccess(fixture("site_info.json"), MediaType.APPLICATION_JSON));

        MoodleSiteInfoResponse info = client.getSiteInfo("tok");

        assertThat(info.userid()).isEqualTo(10001L);
        assertThat(info.fullname()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("과제 목록을 과목명과 함께 평탄화해 반환한다")
    void getAssignments() throws IOException {
        server.expect(requestTo(WS))
                .andRespond(withSuccess(fixture("assignments.json"), MediaType.APPLICATION_JSON));

        List<MoodleAssignment> assignments = client.getAssignments("tok");

        assertThat(assignments).hasSize(3);
        assertThat(assignments.get(0).courseName()).isEqualTo("자바프로그래밍");
        assertThat(assignments.get(0).assignId()).isEqualTo(501L);
        assertThat(assignments.get(0).courseModuleId()).isEqualTo(9001L);
        assertThat(assignments.get(2).name()).isEqualTo("3주차_과제");
    }

    @Test
    @DisplayName("제출 상태가 submitted면 true")
    void isSubmitted() throws IOException {
        server.expect(requestTo(WS))
                .andExpect(content().string(Matchers.containsString("assignid=501")))
                .andRespond(withSuccess(fixture("submission_status_submitted.json"), MediaType.APPLICATION_JSON));

        assertThat(client.isSubmitted("tok", 501L)).isTrue();
    }

    @Test
    @DisplayName("Moodle이 200으로 invalidtoken 예외를 주면 EclassInvalidTokenException")
    void invalidToken() throws IOException {
        server.expect(requestTo(WS))
                .andRespond(withSuccess(fixture("error_invalidtoken.json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getSiteInfo("bad"))
                .isInstanceOf(EclassInvalidTokenException.class);
    }

    @Test
    @DisplayName("그 외 Moodle 예외는 EclassApiException")
    void otherError() {
        server.expect(requestTo(WS))
                .andRespond(withSuccess(
                        "{\"exception\":\"moodle_exception\",\"errorcode\":\"accessexception\",\"message\":\"x\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getAssignments("tok"))
                .isInstanceOf(EclassApiException.class);
    }
}
