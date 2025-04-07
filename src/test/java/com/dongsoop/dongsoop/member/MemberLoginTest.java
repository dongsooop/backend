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

        mockMvc.perform(post("/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"a@dongyang.ac.kr\", \"password\": \"" + password + "\" }")
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호가 20자 초과일 경우 예외 발생")
    void throws_an_exception_if_the_password_length_more_than_20() throws Exception {
        String password = "abc!@121asvc:ryd2@&a2"; // 20자 이상

        mockMvc.perform(post("/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"a@dongyang.ac.kr\", \"password\": \"" + password + "\" }")
        ).andExpect(status().isBadRequest());
    }
}
