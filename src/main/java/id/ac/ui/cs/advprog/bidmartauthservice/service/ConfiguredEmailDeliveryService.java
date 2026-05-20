package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthEmailProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthVerificationProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.model.EmailDeliveryMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfiguredEmailDeliveryService implements EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final AuthEmailProperties authEmailProperties;
    private final AuthVerificationProperties authVerificationProperties;

    @Override
    public void sendVerificationEmail(String email, String token) {
        String subject = authEmailProperties.getApplicationName() + " - Verifikasi Email";
        String body = """
                Halo,

                Terima kasih sudah mendaftar di %s.
                Gunakan token berikut untuk memverifikasi email Anda:

                %s

                Token ini berlaku selama %d jam.
                """.formatted(
                authEmailProperties.getApplicationName(),
                token,
                authVerificationProperties.getEmailExpiryHours()
        );

        deliver(email, subject, body, "verification token", token);
    }

    @Override
    public void sendTwoFactorCode(String email, String code) {
        String subject = authEmailProperties.getApplicationName() + " - Kode Verifikasi 2FA";
        String body = """
                Halo,

                Berikut kode verifikasi 2FA Anda:

                %s

                Kode ini berlaku selama %d menit.
                """.formatted(code, authVerificationProperties.getLoginChallengeExpiryMinutes());

        deliver(email, subject, body, "two-factor code", code);
    }

    private void deliver(String recipient, String subject, String body, String logLabel, String logValue) {
        if (authEmailProperties.getDeliveryMode() == EmailDeliveryMode.LOGGING) {
            log.info("Email delivery mode LOGGING aktif. {} untuk {}: {}", logLabel, recipient, logValue);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(authEmailProperties.getFromAddress());
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email '{}' berhasil dikirim ke {}", subject, recipient);
        } catch (MailException exception) {
            throw new IllegalStateException("Gagal mengirim email ke " + recipient, exception);
        }
    }
}
