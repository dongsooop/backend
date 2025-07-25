package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.MailSendingFormatFileCannotReadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailTextGeneratorImpl implements MailTextGenerator {

    @Value("${mail.format.path}")
    private String mailFormatPath;

    @Override
    public String generateVerificationText(String code) {
        Path filePath = Paths.get(mailFormatPath);
        try {
            String staticForm = Files.readString(filePath);
            return staticForm.replace("{{code}}", code);
        } catch (IOException exception) {
            throw new MailSendingFormatFileCannotReadException(exception, filePath.toString());
        }
    }
}
