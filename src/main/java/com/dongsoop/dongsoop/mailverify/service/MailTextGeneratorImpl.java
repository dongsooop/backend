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

    @Value("${mail.format.file.name}")
    private String mailFormatFileName;

    @Value("${resource.static.base-path}")
    private String staticResourceBasePath;

    @Override
    public String generateVerificationText(String code) {
        Path filePath = getMailFormatPath();

        try {
            String staticForm = Files.readString(filePath);
            return staticForm.replace("{{code}}", code);
        } catch (IOException exception) {
            throw new MailSendingFormatFileCannotReadException(exception);
        }
    }

    private Path getMailFormatPath() {
        Path basePath = Paths.get(staticResourceBasePath)
                .toAbsolutePath()
                .normalize();

        Path filePath = basePath.resolve(mailFormatFileName)
                .normalize();

        if (!filePath.startsWith(basePath) || !Files.exists(filePath) || !filePath.toString().endsWith(".html")) {
            throw new MailSendingFormatFileCannotReadException();
        }

        return filePath;
    }
}
