package com.eneik.generated.service;

import com.eneik.generated.Application;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.TelegramException;
import com.eneik.generated.exception.TelegramFloodWaitException;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
public class DispatchServiceTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @MockBean
    private TelegramClient telegramClient;

    private TgAccount account1;
    private TgAccount account2;
    private TgAccount account3;

    @BeforeEach
    public void setUp() {
        tgAccountRepository.deleteAll();

        // Account 1: Active, with 1 daily sent count, and a limit of 2
        account1 = new TgAccount();
        account1.setPhoneNumber("+1111111111");
        account1.setStatus("Active");
        account1.setDailySentCount(1);
        account1.setDailyLimit(2);
        account1 = tgAccountRepository.save(account1);

        // Account 2: Active, with 0 daily sent count, and a limit of 2
        account2 = new TgAccount();
        account2.setPhoneNumber("+2222222222");
        account2.setStatus("Active");
        account2.setDailySentCount(0);
        account2.setDailyLimit(2);
        account2 = tgAccountRepository.save(account2);

        // Account 3: Active, with 0 daily sent count, and a limit of 2
        account3 = new TgAccount();
        account3.setPhoneNumber("+3333333333");
        account3.setStatus("Active");
        account3.setDailySentCount(0);
        account3.setDailyLimit(2);
        account3 = tgAccountRepository.save(account3);
    }

    @Test
    public void testSuccessfulDispatchIncrementsDailySentCount() throws TelegramException {
        // When
        dispatchService.dispatchMessage("chat_123", "Hello World");

        // Then, the first active account (account1) should have its daily sent count incremented
        TgAccount updatedAccount1 = tgAccountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount1.getDailySentCount()).isEqualTo(2);

        // Verify sendMessage was called with the first account
        verify(telegramClient, times(1)).sendMessage(argThat(acc -> acc.getId().equals(account1.getId())), eq("chat_123"), eq("Hello World"));
    }

    @Test
    public void testRotationWhenAccountDailyLimitIsHit() throws TelegramException {
        // Force account 1 to its limit (2/2)
        account1.setDailySentCount(2);
        tgAccountRepository.save(account1);

        // Attempt dispatch - should bypass account 1 and use account 2
        dispatchService.dispatchMessage("chat_123", "Hello World");

        // Verify account 2 was used and its sent count was incremented
        TgAccount updatedAccount2 = tgAccountRepository.findById(account2.getId()).orElseThrow();
        assertThat(updatedAccount2.getDailySentCount()).isEqualTo(1);

        // Account 1 should still be at 2
        TgAccount updatedAccount1 = tgAccountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount1.getDailySentCount()).isEqualTo(2);

        // Verify client call
        verify(telegramClient, times(1)).sendMessage(argThat(acc -> acc.getId().equals(account2.getId())), eq("chat_123"), eq("Hello World"));
        verify(telegramClient, never()).sendMessage(argThat(acc -> acc.getId().equals(account1.getId())), any(), any());
    }

    @Test
    public void testRotationWhenTelegramFloodWaitIsThrown() throws TelegramException {
        // Setup: account 1 throws FLOOD_WAIT, account 2 succeeds
        doThrow(new TelegramFloodWaitException("Too many requests"))
                .when(telegramClient)
                .sendMessage(argThat(acc -> acc.getId().equals(account1.getId())), any(), any());

        // Attempt dispatch
        dispatchService.dispatchMessage("chat_123", "Hello World");

        // Then account 1 should be marked as "Temporary Spam-Block"
        TgAccount updatedAccount1 = tgAccountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount1.getStatus()).isEqualTo("Temporary Spam-Block");
        // Count should NOT be incremented for account 1
        assertThat(updatedAccount1.getDailySentCount()).isEqualTo(1);

        // Account 2 should have been picked next, and successfully updated
        TgAccount updatedAccount2 = tgAccountRepository.findById(account2.getId()).orElseThrow();
        assertThat(updatedAccount2.getStatus()).isEqualTo("Active");
        assertThat(updatedAccount2.getDailySentCount()).isEqualTo(1);

        // Verify mock invocations
        verify(telegramClient, times(1)).sendMessage(argThat(acc -> acc.getId().equals(account1.getId())), eq("chat_123"), eq("Hello World"));
        verify(telegramClient, times(1)).sendMessage(argThat(acc -> acc.getId().equals(account2.getId())), eq("chat_123"), eq("Hello World"));
    }

    @Test
    public void testExceptionThrownWhenNoActiveAccountsRemaining() {
        // Make all accounts hit limits or not active
        account1.setDailySentCount(2);
        account2.setStatus("Permanent Ban");
        account3.setDailySentCount(2);
        tgAccountRepository.save(account1);
        tgAccountRepository.save(account2);
        tgAccountRepository.save(account3);

        // Try to dispatch message, expect exception
        assertThatThrownBy(() -> dispatchService.dispatchMessage("chat_123", "Hello World"))
                .isInstanceOf(TelegramException.class)
                .hasMessageContaining("No available active Telegram accounts with remaining daily quota.");
    }
}
