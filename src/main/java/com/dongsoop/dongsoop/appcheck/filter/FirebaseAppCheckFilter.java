package com.dongsoop.dongsoop.appcheck.filter;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.appcheck.exception.UnknownFirebaseFetchJWKException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@Slf4j
public class FirebaseAppCheckFilter extends OncePerRequestFilter {

    private static final String DEVICE_AUTH_HEADER = "X-Firebase-AppCheck";

    private final FirebaseAppCheck firebaseAppCheck;
    private final HandlerExceptionResolver exceptionResolver;

    public FirebaseAppCheckFilter(FirebaseAppCheck firebaseAppCheck,
                                  @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.firebaseAppCheck = firebaseAppCheck;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String deviceToken = extractDeviceAuthTokenFromHeader(request);

            try {
                firebaseAppCheck.validate(deviceToken);
                log.debug("Firebase AppCheck validation successful");
            } catch (UnknownFirebaseFetchJWKException e) {
                log.warn("Firebase AppCheck validation failed, retrying after cache update: {}", e.getMessage());
                updateCacheSafely();
                firebaseAppCheck.validate(deviceToken);
                log.debug("Firebase AppCheck validation successful after cache update");
            }
        } catch (TokenNotFoundException e) {
            log.warn("Firebase AppCheck token not found: {}", e.getMessage());
            exceptionResolver.resolveException(request, response, null, e);
            return;
        } catch (UnknownFirebaseFetchJWKException e) {
            log.error("Firebase AppCheck validation failed even after cache update: {}", e.getMessage(), e);
            exceptionResolver.resolveException(request, response, null, e);
            return;
        } catch (Exception e) {
            log.error("Unexpected error during Firebase AppCheck validation: {}", e.getMessage(), e);
            exceptionResolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void updateCacheSafely() throws UnknownFirebaseFetchJWKException {

        try {
            firebaseAppCheck.updateCache();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnknownFirebaseFetchJWKException(e);
        } catch (IOException e) {
            throw new UnknownFirebaseFetchJWKException(e);
        }
    }

    private String extractDeviceAuthTokenFromHeader(HttpServletRequest request) throws TokenNotFoundException {
        String deviceAuthToken = request.getHeader(DEVICE_AUTH_HEADER);

        if (!StringUtils.hasText(deviceAuthToken)) {
            throw new TokenNotFoundException();
        }

        return deviceAuthToken;
    }
}
