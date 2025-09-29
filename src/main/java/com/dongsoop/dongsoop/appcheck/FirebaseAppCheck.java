package com.dongsoop.dongsoop.appcheck;

import java.io.IOException;

public interface FirebaseAppCheck {

    void validate(String token);

    void updateCache() throws IOException, InterruptedException;
}
