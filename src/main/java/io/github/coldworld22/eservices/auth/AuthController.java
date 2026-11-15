package io.github.coldworld22.eservices.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/otp")
@Tag(name = "Sign-in")
@SecurityRequirements
public class AuthController {

    private final OtpService otpService;

    public AuthController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a one-time code to the user registered with this national ID")
    public OtpRequestResponse requestCode(@Valid @RequestBody OtpRequest body) {
        return otpService.requestCode(body.nationalId());
    }

    @PostMapping("/verify")
    @Operation(summary = "Exchange a valid one-time code for an access token")
    public TokenResponse verifyCode(@Valid @RequestBody OtpVerifyRequest body) {
        return otpService.verifyCode(body.nationalId(), body.code());
    }
}
