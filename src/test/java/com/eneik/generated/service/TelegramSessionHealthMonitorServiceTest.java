package com.eneik.generated.service;

import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TelegramSessionHealthMonitorServiceTest {

    @Autowired
    private TelegramSessionHealthMonitorService monitorService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @BeforeEach
    public void setUp() {
        tgAccountRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        tgAccountRepository.deleteAll();
    }

    @Test
    public void testValidActiveSessionRemainsActive() {
        // Given an active Telegram session with valid session data
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380111111111");
        account.setStatus("Active");
        account.setSessionData("valid_session_token_xyz");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then its status is verified as Active
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Active", updatedAccount.getStatus());
    }

    @Test
    public void testPermanentBanSessionIsSkipped() {
        // Given an account session already marked as "Permanent Ban"
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380222222222");
        account.setStatus("Permanent Ban");
        // We set the session data to "spam-block". If it were checked, it would transition to "Temporary Spam-Block".
        account.setSessionData("spam-block_payload");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then the system skips verification and retains the "Permanent Ban" status
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Permanent Ban", updatedAccount.getStatus());
    }

    @Test
    public void testTemporarySpamBlockDetectedAndUpdated() {
        // Given an active account whose session contains "spam-block" indicator
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380333333333");
        account.setStatus("Active");
        account.setSessionData("session_spam-block_error");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then it must be updated to "Temporary Spam-Block"
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Temporary Spam-Block", updatedAccount.getStatus());
    }

    @Test
    public void testPermanentBanDetectedAndUpdated() {
        // Given an active account whose session contains "ban" indicator
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380444444444");
        account.setStatus("Active");
        account.setSessionData("session_state_banned");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then it must be updated to "Permanent Ban"
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Permanent Ban", updatedAccount.getStatus());
    }

    @Test
    public void testReauthorizationRequiredDetectedAndUpdated() {
        // Given an active account whose session contains "reauth" indicator
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380555555555");
        account.setStatus("Active");
        account.setSessionData("reauth_required_now");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then it must be updated to "Re-authorization Required"
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Re-authorization Required", updatedAccount.getStatus());
    }

    @Test
    public void testEmptySessionDataTriggersReauthorizationRequired() {
        // Given an active account with empty session data
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380666666666");
        account.setStatus("Active");
        account.setSessionData("");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs
        monitorService.runSessionHealthCheck();

        // Then it must be updated to "Re-authorization Required"
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Re-authorization Required", updatedAccount.getStatus());
    }

    @Test
    public void testTemporaryNetworkFailureRetainsPreviousStatus() {
        // Given an account with "Temporary Spam-Block" status and a temporary network failure simulation
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+380777777777");
        account.setStatus("Temporary Spam-Block");
        account.setSessionData("network-fail_timeout_error");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account = tgAccountRepository.save(account);

        // When the background check runs and fails temporarily
        monitorService.runSessionHealthCheck();

        // Then the system should retain the previous status
        TgAccount updatedAccount = tgAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("Temporary Spam-Block", updatedAccount.getStatus());
    }
}
