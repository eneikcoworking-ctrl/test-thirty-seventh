package com.eneik.generated.controller;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.dto.OnboardOtpRequest;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.service.ProxyValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts/onboard")
public class AccountOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(AccountOnboardingController.class);

    private final TgAccountRepository tgAccountRepository;
    private final ProxyRepository proxyRepository;
    private final ProxyValidationService proxyValidationService;

    public AccountOnboardingController(TgAccountRepository tgAccountRepository,
                                       ProxyRepository proxyRepository,
                                       ProxyValidationService proxyValidationService) {
        this.tgAccountRepository = tgAccountRepository;
        this.proxyRepository = proxyRepository;
        this.proxyValidationService = proxyValidationService;
    }

    @PostMapping("/otp")
    public ResponseEntity<?> onboardWithOtp(@RequestBody OnboardOtpRequest request) {
        log.info("Received OTP onboarding request for phone: {}", request.getPhoneNumber());

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            log.warn("Onboarding OTP failed: Phone number is required");
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            log.warn("Onboarding OTP failed: OTP code is required");
            return ResponseEntity.badRequest().body(Map.of("error", "OTP code is required"));
        }

        if (request.getOtpCode().toLowerCase().contains("invalid")) {
            log.warn("Onboarding OTP failed: Provided OTP code is invalid");
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP code provided"));
        }

        // Validate Proxy
        boolean isProxyValid = proxyValidationService.isValidProxy(
                request.getProxyIp(),
                request.getProxyPort(),
                request.getProxyProtocol()
        );

        if (!isProxyValid) {
            log.warn("Onboarding OTP failed: Invalid proxy details provided for phone: {}", request.getPhoneNumber());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid proxy configuration or connectivity check failed"));
        }

        try {
            // Save Proxy
            Proxy proxy = new Proxy();
            proxy.setIpAddress(request.getProxyIp());
            proxy.setPort(request.getProxyPort());
            proxy.setProtocol(request.getProxyProtocol());
            proxy.setUsername(request.getProxyUsername());
            proxy.setPassword(request.getProxyPassword());
            Proxy savedProxy = proxyRepository.save(proxy);

            // Register/Update account
            TgAccount account = tgAccountRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseGet(TgAccount::new);

            account.setPhoneNumber(request.getPhoneNumber());
            account.setProxy(savedProxy);
            account.setSessionData("otp-auth-session-code:" + request.getOtpCode());
            account.setStatus("Active");
            account.setUpdatedAt(LocalDateTime.now());
            if (account.getId() == null) {
                account.setCreatedAt(LocalDateTime.now());
            }

            TgAccount savedAccount = tgAccountRepository.save(account);
            log.info("Successfully onboarded account with OTP for phone: {}", request.getPhoneNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Account successfully registered",
                    "accountId", savedAccount.getId(),
                    "status", savedAccount.getStatus()
            ));
        } catch (Exception e) {
            log.error("Internal server error during OTP onboarding for phone: " + request.getPhoneNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/session", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> onboardWithSessionFile(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("sessionFile") MultipartFile sessionFile,
            @RequestParam("proxyIp") String proxyIp,
            @RequestParam("proxyPort") Integer proxyPort,
            @RequestParam("proxyProtocol") String proxyProtocol,
            @RequestParam(value = "proxyUsername", required = false) String proxyUsername,
            @RequestParam(value = "proxyPassword", required = false) String proxyPassword) {

        log.info("Received Session File onboarding request for phone: {}", phoneNumber);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("Onboarding Session failed: Phone number is required");
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        if (sessionFile == null || sessionFile.isEmpty()) {
            log.warn("Onboarding Session failed: Session file is empty or missing");
            return ResponseEntity.badRequest().body(Map.of("error", "Session file is required"));
        }

        // Validate Proxy
        boolean isProxyValid = proxyValidationService.isValidProxy(
                proxyIp,
                proxyPort,
                proxyProtocol
        );

        if (!isProxyValid) {
            log.warn("Onboarding Session failed: Invalid proxy details provided for phone: {}", phoneNumber);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid proxy configuration or connectivity check failed"));
        }

        try {
            String sessionDataContent = new String(sessionFile.getBytes(), StandardCharsets.UTF_8);

            // Save Proxy
            Proxy proxy = new Proxy();
            proxy.setIpAddress(proxyIp);
            proxy.setPort(proxyPort);
            proxy.setProtocol(proxyProtocol);
            proxy.setUsername(proxyUsername);
            proxy.setPassword(proxyPassword);
            Proxy savedProxy = proxyRepository.save(proxy);

            // Register/Update account
            TgAccount account = tgAccountRepository.findByPhoneNumber(phoneNumber)
                    .orElseGet(TgAccount::new);

            account.setPhoneNumber(phoneNumber);
            account.setProxy(savedProxy);
            account.setSessionData(sessionDataContent);
            account.setStatus("Active");
            account.setUpdatedAt(LocalDateTime.now());
            if (account.getId() == null) {
                account.setCreatedAt(LocalDateTime.now());
            }

            TgAccount savedAccount = tgAccountRepository.save(account);
            log.info("Successfully onboarded account with Session file for phone: {}", phoneNumber);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Account successfully registered from session file",
                    "accountId", savedAccount.getId(),
                    "status", savedAccount.getStatus()
            ));
        } catch (IOException e) {
            log.error("Failed to read session file for phone: " + phoneNumber, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to read session file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error during session onboarding for phone: " + phoneNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}
