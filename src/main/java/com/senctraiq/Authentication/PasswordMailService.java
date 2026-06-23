package com.senctraiq.Authentication;

import com.senctraiq.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordMailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:8084}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@senctraiq.local}")
    private String fromAddress;

    public void sendPasswordResetLink(User user, String token) {
        String resetLink = UriComponentsBuilder
                .fromUriString(frontendUrl.replaceAll("/+$", ""))
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("SenctraIQ password reset");
        message.setText("""
                Hello %s,

                Use the link below to reset your SenctraIQ password. The link expires in 30 minutes.

                %s

                If you did not request this reset, you can ignore this email.
                """.formatted(user.getFirstName(), resetLink));

        send(message);
    }

    public void sendTemporaryPassword(User user, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("SenctraIQ account unlocked");
        message.setText("""
                Hello %s,

                Your SenctraIQ account has been unlocked.

                Temporary password: %s

                Sign in with this temporary password. You will be prompted to choose a new password immediately.
                """.formatted(user.getFirstName(), temporaryPassword));

        send(message);
    }

    private void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException error) {
            throw new IllegalStateException("Unable to send password email", error);
        }
    }
}
