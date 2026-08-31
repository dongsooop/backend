package com.dongsoop.dongsoop.eclass.client;

import com.dongsoop.dongsoop.eclass.client.dto.MoodleAssignment;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleAssignmentsResponse;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleSiteInfoResponse;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleSubmissionStatusResponse;
import com.dongsoop.dongsoop.eclass.exception.EclassApiException;
import com.dongsoop.dongsoop.eclass.exception.EclassInvalidTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Moodle 웹서비스 REST 클라이언트.
 *
 * <p>Moodle은 오류도 HTTP 200으로 {@code {exception, errorcode}}를 돌려주므로,
 * 상태 코드가 아니라 응답 본문의 exception 필드로 실패를 판별해야 한다.
 */
@Component
@RequiredArgsConstructor
public class EclassClient {

    private static final String WS_PATH = "/webservice/rest/server.php";
    private static final String INVALID_TOKEN_CODE = "invalidtoken";
    private static final String SUBMITTED_STATUS = "submitted";

    // 필드명이 곧 빈 이름이다 — Lombok 생성자에는 @Qualifier가 복사되지 않아
    // 이름이 다르면 공용 restTemplate(읽기 4초)이 주입된다
    private final RestTemplate eclassRestTemplate;

    private final ObjectMapper objectMapper;

    @Value("${eclass.base-url}")
    private String baseUrl;

    @Value("${eclass.user-agent}")
    private String userAgent;

    public MoodleSiteInfoResponse getSiteInfo(String token) {
        JsonNode node = call(token, "core_webservice_get_site_info", Map.of());

        return convert(node, MoodleSiteInfoResponse.class);
    }

    /**
     * 수강 중인 전체 과목의 과제를 반환한다. 마감이 없는 과제(duedate=0)도 그대로 포함하므로
     * 기간 필터는 호출하는 쪽에서 한다.
     */
    public List<MoodleAssignment> getAssignments(String token) {
        JsonNode node = call(token, "mod_assign_get_assignments", Map.of());

        return convert(node, MoodleAssignmentsResponse.class)
                .flatten();
    }

    public boolean isSubmitted(String token, long assignId) {
        JsonNode node = call(token, "mod_assign_get_submission_status",
                Map.of("assignid", String.valueOf(assignId)));

        return SUBMITTED_STATUS.equals(convert(node, MoodleSubmissionStatusResponse.class).submissionStatus());
    }

    private JsonNode call(String token, String function, Map<String, String> args) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("wstoken", token);
        form.add("wsfunction", function);
        form.add("moodlewsrestformat", "json");
        args.forEach(form::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, userAgent);

        String body;
        try {
            body = eclassRestTemplate.postForObject(baseUrl + WS_PATH, new HttpEntity<>(form, headers), String.class);
        } catch (RestClientException exception) {
            throw new EclassApiException(function, exception);
        }

        if (body == null || body.isBlank()) {
            throw new EclassApiException(function, "empty response");
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (JsonProcessingException exception) {
            throw new EclassApiException(function, exception);
        }

        if (node.has("exception")) {
            String errorCode = node.path("errorcode").asText();
            if (INVALID_TOKEN_CODE.equals(errorCode)) {
                throw new EclassInvalidTokenException();
            }
            throw new EclassApiException(function, errorCode);
        }

        return node;
    }

    private <T> T convert(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException exception) {
            throw new EclassApiException(type.getSimpleName(), exception);
        }
    }
}
