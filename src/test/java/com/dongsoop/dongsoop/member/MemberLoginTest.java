package com.dongsoop.dongsoop.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberLoginTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("비밀번호가 8자 미만일 경우 예외 발생")
    void throws_an_exception_if_the_password_length_less_than_8() throws Exception {
        String password = "abc!@12"; // 8자 미만

        postToLogin(null, password);
    }

    @Test
    @DisplayName("비밀번호가 20자 초과일 경우 예외 발생")
    void throws_an_exception_if_the_password_length_more_than_20() throws Exception {
        String password = "abc!@121asvc:ryd2@&a2"; // 20자 이상

        postToLogin(null, password);
    }

    @Test
    @DisplayName("비밀번호에 특수문자가 포함되어 있지 않으면 예외 발생")
    void throws_an_exception_if_the_password_does_not_contain_special_characters() throws Exception {
        String password = "abcd1234"; // 특수문자가 없는 8글자 이상 비밀번호

        postToLogin(null, password);
    }

    @Test
    @DisplayName("비밀번호에 숫자가 포함되어 있지 않으면 예외 발생")
    void throws_an_exception_if_the_password_does_not_contain_numbers() throws Exception {
        String password = "abcd!@#$"; // 숫자가 없는 8글자 이상 비밀번호

        postToLogin(null, password);
    }

    @Test
    @DisplayName("비밀번호에 알파벳이 포함되어 있지 않으면 예외 발생")
    void throws_an_exception_if_the_password_does_not_contain_alphabet() throws Exception {
        String password = "1234!@#$"; // 알파벳이 없는 8글자 이상 비밀번호

        postToLogin(null, password);
    }

    void postToLogin(String email, String password) throws Exception {
        String testEmail = "test@dongyang.ac.kr";
        if (email != null) {
            testEmail = email;
        }

        String testPassword = "abc123!@#Z";
        if (password != null) {
            testPassword = password;
        }

        mockMvc.perform(post("/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"" + testEmail + "\", \"password\": \"" + testPassword + "\" }")
        ).andExpect(status().isBadRequest());
    }
}
