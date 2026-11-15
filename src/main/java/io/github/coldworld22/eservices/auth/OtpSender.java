package io.github.coldworld22.eservices.auth;

import io.github.coldworld22.eservices.user.User;

/**
 * Delivers a one-time code to the user. In production this would call an SMS gateway or a national
 * identity provider; swapping the implementation does not touch the sign-in logic.
 */
public interface OtpSender {

    void send(User user, String code);
}
