# Security

How the API addresses each item of the [OWASP Top 10 (2021)](https://owasp.org/Top10/), and what is
deliberately left to the platform around it.

| # | Risk | What this codebase does | Where |
|---|---|---|---|
| A01 | Broken access control | Every endpoint needs a signed token except sign-in, health and API docs. Roles are checked per endpoint with `@PreAuthorize`. Citizens can only read and cancel their own requests, and get **404 rather than 403** for anyone else's, so IDs can't be probed. | `SecurityConfig`, `ServiceRequestController`, `ServiceRequestService.loadVisible` |
| A02 | Cryptographic failures | One-time codes are stored only as **BCrypt hashes**. Tokens are HS256 with a key of at least 256 bits, which is required at start-up and has no default. Database storage is encrypted, connections stay in private subnets, and secrets come from AWS Secrets Manager. | `OtpService`, `SecurityConfig.jwtSigningKey`, `infra/terraform` |
| A03 | Injection | All SQL goes through JPA with bound parameters; the one native query takes no input. LDAP lookups build the filter with `EqualsFilter`, which escapes the username, and a test sends `*)(uid=*` to prove it. Input is validated with Bean Validation (national ID format, lengths). | `ServiceRequestRepository`, `StaffDirectoryLoginService`, `StaffDirectoryLoginTest` |
| A04 | Insecure design | The request lifecycle is a state machine; illegal transitions are rejected by the entity itself. Optimistic locking stops two supervisors silently overwriting each other. Sign-in gives the same answer for unknown IDs so it can't be used to find registered users. | `RequestStatus`, `ServiceRequest`, `OtpService` |
| A05 | Security misconfiguration | Strict `Content-Security-Policy`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `nosniff` and `no-store` on every response. Only `health` and `info` actuator endpoints are exposed. Errors are RFC 7807 problem details with no stack traces. The container runs as a non-root user. | `SecurityConfig`, `application.yml`, `Dockerfile`, `SecurityHeadersTest` |
| A06 | Vulnerable components | Dependabot watches Maven, GitHub Actions, the Docker base image and Terraform providers. ECR scans every pushed image. | `.github/dependabot.yml`, `infra/terraform/ecs.tf` |
| A07 | Identification and authentication failures | Codes expire after 5 minutes, allow 5 wrong guesses, are single-use and are limited per national ID. Sign-in endpoints are rate limited per IP. Staff use Active Directory; blank passwords are refused because an empty LDAP bind can succeed anonymously. Tokens expire after 30 minutes. | `OtpService`, `AuthRateLimitFilter`, `StaffDirectoryLoginService` |
| A08 | Software and data integrity failures | CI deploys to AWS through OIDC with a role limited to this repository's `main` branch; no long-lived keys exist. Schema changes go only through versioned Flyway migrations, and Hibernate validates the schema at start-up. | `.github/workflows/deploy.yml`, `infra/terraform/github-oidc.tf` |
| A09 | Logging and monitoring failures | Sign-ins (successful and failed), OTP requests and every status change are written to an append-only audit table **in the same transaction** as the change, with actor, role and IP. Codes are never logged. Container logs go to CloudWatch. | `AuditService`, `LoggingOtpSender` |
| A10 | Server-side request forgery | The API never fetches URLs supplied by users. Its only outbound calls are to the database and the configured directory. | — |

CodeQL scans the code on every push to `main` and every week.

## Known limits

- **The sign-in rate limit is per instance and keyed on the client IP.** Behind several instances,
  or if a proxy's `X-Forwarded-For` can't be trusted, add a shared limit in front, such as an AWS WAF
  rate-based rule.
- **Demo mode** (`OTP_EXPOSE_CODE=true`) returns one-time codes in API responses. It exists so the
  public demo can be tried without an SMS gateway and must stay off with real users.
- **Tokens can't be revoked before they expire.** With a 30-minute lifetime that's acceptable for this
  service; a denylist or shorter tokens with refresh would be the next step.

## Reporting a vulnerability

Please email ayman4work44@gmail.com rather than opening a public issue.
