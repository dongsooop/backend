package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.exception.domain.member.InvalidEmailFormatException;
import com.dongsoop.dongsoop.exception.domain.member.InvalidPasswordFormatException;
import com.dongsoop.dongsoop.exception.domain.member.InvalidStudentIdFormatException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    private static final String STUDENT_ID_REGEX = "^\\d{8}$";

    private String email;
    private String password;
    private String nickname;
    private String studentId;
    private String department;

    public void validate() {
        validateEmail();
        validatePassword();
        validateStudentId();
    }

    private void validateEmail() {
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            throw new InvalidEmailFormatException();
        }
    }

    private void validatePassword() {
        if (!Pattern.matches(PASSWORD_REGEX, password)) {
            throw new InvalidPasswordFormatException();
        }
    }

    private void validateStudentId() {
        if (!Pattern.matches(STUDENT_ID_REGEX, studentId)) {
            throw new InvalidStudentIdFormatException();
        }
    }
}