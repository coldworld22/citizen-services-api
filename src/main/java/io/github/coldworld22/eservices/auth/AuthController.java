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
@RequestMapping("/api/v1/auth")
@Tag(name = "Sign-in")
@SecurityRequirements
public class AuthController {

    private final OtpService otpService;
    private final StaffDirectoryLoginService staffLogin;

    public AuthController(OtpService otpService, StaffDirectoryLoginService staffLogin) {
        this.otpService = otpService;
        this.staffLogin = staffLogin;
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a one-time code to the user registered with this national ID")
    public OtpRequestResponse requestCode(@Valid @RequestBody OtpRequest body) {
        return otpService.requestCode(body.nationalId());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Exchange a valid one-time code for an access token")
    public TokenResponse verifyCode(@Valid @RequestBody OtpVerifyRequest body) {
        return otpService.verifyCode(body.nationalId(), body.code());
    }

    @PostMapping("/staff/login")
    @Operation(summary = "Staff sign-in with an Active Directory (LDAP) username and password")
    public TokenResponse staffLogin(@Valid @RequestBody StaffLoginRequest body) {
        return staffLogin.login(body.username().trim(), body.password());
    }
}
