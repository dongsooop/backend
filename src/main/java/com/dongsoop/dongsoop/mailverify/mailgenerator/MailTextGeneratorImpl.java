package com.dongsoop.dongsoop.mailverify.mailgenerator;

import com.dongsoop.dongsoop.mailverify.exception.MailSendingFormatFileCannotReadException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MailTextGeneratorImpl implements MailTextGenerator {

    @Value("${mail.format.file.name}")
    private String mailFormatFileName;

    @Value("${resource.static.base-path}")
    private String staticResourceBasePath;

    @Override
    public String generateVerificationText(String code) {
        ClassPathResource resource = new ClassPathResource(staticResourceBasePath + mailFormatFileName);

        try {
            String staticForm = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return staticForm.replace("{{code}}", code);
        } catch (IOException exception) {
            throw new MailSendingFormatFileCannotReadException(exception);
        }
    }
}
