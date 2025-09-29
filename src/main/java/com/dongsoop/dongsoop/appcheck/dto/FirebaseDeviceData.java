package com.dongsoop.dongsoop.appcheck.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseDeviceData {

    String kty;
    String use;
    String alg;
    String kid;
    String n;
    String e;
    String typ;
}
