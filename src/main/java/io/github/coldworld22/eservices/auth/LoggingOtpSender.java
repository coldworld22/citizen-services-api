package io.github.coldworld22.eservices.auth;

import io.github.coldworld22.eservices.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development sender: records that a code was sent without ever logging the code itself. */
@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(User user, String code) {
        log.info("One-time code issued for user {} (phone {})", user.getId(), mask(user.getPhone()));
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "unknown";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
