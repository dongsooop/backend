package com.dongsoop.dongsoop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(("/test"))
public class TestController {

    @PostMapping("/user")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok()
                .build();
    }
}
