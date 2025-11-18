package com.dongsoop.dongsoop.member.constant;

public class MemberPassword {

    // 8글자 이상 20글자 이하, 영문, 숫자, 특수문자 포함
    public static final String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+={}\\[\\]?,./~])[A-Za-z\\d!@#$%^&*()_\\-+={}\\[\\]?,./~]{8,20}$";
}
