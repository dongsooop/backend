package com.dongsoop.dongsoop.appcheck.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "test"})
public class LocalWebAppCheckServiceImpl implements WebAppCheckService {

    @Override
    public String issue() {
        log.debug("Web App Check token issuance skipped in local/test profile.");
        return "local-web-app-check-token-placeholder";
    }
}
