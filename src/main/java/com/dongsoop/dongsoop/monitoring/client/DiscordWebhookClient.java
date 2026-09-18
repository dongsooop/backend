package com.dongsoop.dongsoop.monitoring.client;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class DiscordWebhookClient {

    /** 디스코드 메시지 본문 제한 */
    static final int MAX_CONTENT_LENGTH = 2000;

    private final RestTemplate restTemplate;

    public void send(String webhookUrl, String content) {
        for (String chunk : split(content)) {
            restTemplate.postForEntity(webhookUrl, Map.of("content", chunk), String.class);
        }
    }

    public static java.util.List<String> split(String content) {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        String rest = content;
        while (rest.length() > MAX_CONTENT_LENGTH) {
            int cut = rest.lastIndexOf('\n', MAX_CONTENT_LENGTH);
            if (cut <= 0) {
                cut = MAX_CONTENT_LENGTH;
            }
            chunks.add(rest.substring(0, cut));
            rest = rest.substring(cut).stripLeading();
        }
        chunks.add(rest);
        return chunks;
    }
}
