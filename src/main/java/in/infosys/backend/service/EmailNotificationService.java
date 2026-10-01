package in.infosys.backend.service;


import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean enabled;

    @Value("${notification.email.from:}")
    private String from;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean send(
            String recipient,
            String subject,
            String message
    ) {

        if (!enabled) {
            System.out.println(
                    "[EMAIL DISABLED] To: "
                            + recipient
                            + " | Subject: "
                            + subject
                            + " | Message: "
                            + message
            );

            return false;
        }

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email is missing"
            );
        }

        try {

            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, false, "UTF-8");

            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(message, false);

            mailSender.send(mimeMessage);

            return true;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Email delivery failed: " + e.getMessage(),
                    e
            );
        }
    }
}