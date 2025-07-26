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

    @Value("${resource.static.base-path}")
    private String staticResourceBasePath;

    @Override
    public String generateVerificationText(String code) {
        Path filePath = getMailFormatPath();

        try {
            String staticForm = Files.readString(filePath);
            return staticForm.replace("{{code}}", code);
        } catch (IOException exception) {
            throw new MailSendingFormatFileCannotReadException(exception, filePath.toString());
        }
    }

    private Path getMailFormatPath() {
        Path basePath = Paths.get(staticResourceBasePath)
                .toAbsolutePath()
                .normalize();
        Path filePath = basePath.resolve(mailFormatPath)
                .normalize();

        if (!filePath.startsWith(basePath)) {
            throw new MailSendingFormatFileCannotReadException(filePath.toString());
        }

        return filePath;
    }
}
