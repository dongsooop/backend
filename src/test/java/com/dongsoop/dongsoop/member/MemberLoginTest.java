package com.dongsoop.dongsoop.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailValidator;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailValidator;
import com.dongsoop.dongsoop.member.controller.MemberController;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberLoginTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private MemberDuplicationValidator memberDuplicationValidator;
    @MockitoBean
    private PasswordUpdateMailValidator passwordUpdateMailValidator;
    @MockitoBean
    private RegisterMailValidator registerMailValidator;
    @MockitoBean
    private MemberDeviceService memberDeviceService;
    @MockitoBean
    private FCMService fcmService;
    @MockitoBean
    private JwtFilter jwtFilter;

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

    @Test
    @DisplayName("이메일 서명에서 kr이 잘못된 경우 예외 발생")
    void throws_an_exception_if_the_email_signature_kr_not_available() throws Exception {
        String krNotAvailable = "a@dongyang.ac.k";

        postToLogin(krNotAvailable, null);
    }

    @Test
    @DisplayName("이메일 서명에서 ac가 잘못된 경우 예외 발생")
    void throws_an_exception_if_the_email_signature_ac_not_available() throws Exception {
        String acNotAvailable = "a@dongyang.a.kr";

        postToLogin(acNotAvailable, null);
    }

    @Test
    @DisplayName("이메일 서명에서 at(@)이 잘못된 경우 예외 발생")
    void throws_an_exception_if_the_email_signature_at_not_available() throws Exception {
        String emailAtNotAvailable = "adongyang.ac.kr";

        postToLogin(emailAtNotAvailable, null);
    }

    @Test
    @DisplayName("이메일 서명에서 이름이 없는 경우 예외 발생")
    void throws_an_exception_if_the_email_name_is_empty() throws Exception {
        String signatureNameNotAvailable = "@dongyang.ac.kr";

        postToLogin(signatureNameNotAvailable, null);
    }

    @Test
    @DisplayName("이메일 서명에서 첫 번째 닷(.)이 없는 경우 예외 발생")
    void throws_an_exception_if_the_email_signature_first_dot_is_empty() throws Exception {
        String firstDotNotAvailable = "a@dongyangac.kr";

        postToLogin(firstDotNotAvailable, null);
    }

    @Test
    @DisplayName("이메일 서명에서 두 번째 닷(.)이 없는 경우 예외 발생")
    void throws_an_exception_if_the_email_signature_second_dot_is_empty() throws Exception {
        String secondDotNotAvailable = "a@dongyang.ackr";

        postToLogin(secondDotNotAvailable, null);
    }

    @Test
    @DisplayName("이메일에 시그니처가 중첩된 경우 예외 발생")
    void throws_an_exception_if_the_signature_duplicated() throws Exception {
        String signatureDuplication = "@dongyang.ac.kr@dongyang.ac.kr";

        postToLogin(signatureDuplication, null);
    }

    @Test
    @DisplayName("이메일 포맷이 올바르지 않으면 예외 발생")
    void throws_an_exception_if_the_email_name_contain_special_characters() throws Exception {
        String useSpecialCharacterAtName = "dongyang!@dongyang.ac.kr";

        postToLogin(useSpecialCharacterAtName, null);
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
