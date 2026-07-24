package com.eneik.generated.service;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.exception.DailyLimitExceededException;
import com.eneik.generated.exception.SessionLimitReachedException;
import com.eneik.generated.integration.TelegramClient;
import com.eneik.generated.repository.DailyLimitTrackerRepository;
import com.eneik.generated.repository.OutreachSessionRepository;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import com.eneik.generated.util.Sleeper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class MessageDispatchServiceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private DailyLimitTrackerRepository dailyLimitTrackerRepository;

    @Autowired
    private OutreachSessionRepository outreachSessionRepository;

    @Autowired
    private MessageDispatchService messageDispatchService;

    @MockBean
    private Sleeper sleeper;

    @SpyBean
    private TelegramClient telegramClient;

    private TgAccount account;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        dailyLimitTrackerRepository.deleteAll();
        outreachSessionRepository.deleteAll();
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();

        Proxy proxy = new Proxy();
        proxy.setIpAddress("127.0.0.1");
        proxy.setPort(9050);
        proxy.setProtocol("SOCKS5");
        Proxy savedProxy = proxyRepository.save(proxy);

        TgAccount tgAccount = new TgAccount();
        tgAccount.setPhoneNumber("+380991234567");
        tgAccount.setStatus("Active");
        tgAccount.setProxy(savedProxy);
        tgAccount.setDailyLimit(5); // Custom limit for testing
        account = tgAccountRepository.save(tgAccount);
    }

    @Test
    public void testDispatchPrecededByTypingAndPause() throws Exception {
        messageDispatchService.dispatchMessage(account.getId(), "@john_doe", "Hello John!");

        // Verify that typing signal and pause precede sending the message
        InOrder inOrder = inOrder(telegramClient, sleeper);
        inOrder.verify(telegramClient).sendTypingSignal(eq("+380991234567"), eq("@john_doe"));
        inOrder.verify(sleeper).sleep(anyLong());
        inOrder.verify(telegramClient).sendMessage(eq("+380991234567"), eq("@john_doe"), eq("Hello John!"));
    }

    @Test
    public void testDailyLimitBlocked() {
        // We set account daily limit to 5 in setup.
        // Send 5 messages:
        for (int i = 0; i < 5; i++) {
            messageDispatchService.dispatchMessage(account.getId(), "@john_doe_" + i, "Msg " + i);
        }

        // The 6th message should fail with DailyLimitExceededException
        assertThatThrownBy(() -> {
            messageDispatchService.dispatchMessage(account.getId(), "@john_doe_5", "Failed message");
        }).isInstanceOf(DailyLimitExceededException.class)
          .hasMessageContaining("Daily messaging limit reached");
    }

    @Test
    public void testSessionMessageCountLimitOf8Reached() {
        // Send and receive back-and-forth messages up to 7
        for (int i = 0; i < 3; i++) {
            messageDispatchService.dispatchMessage(account.getId(), "@amy_smith", "Outbound " + i);
            messageDispatchService.receiveMessage(account.getId(), "@amy_smith", "Inbound " + i);
        }
        // Total so far: 6 messages (3 outbound + 3 inbound)

        // 7th message: dispatch
        messageDispatchService.dispatchMessage(account.getId(), "@amy_smith", "Outbound 3");
        // Total so far: 7 messages

        // 8th message: receive
        messageDispatchService.receiveMessage(account.getId(), "@amy_smith", "Inbound 3");
        // Total is now 8. Subsequent dispatch should be blocked!

        assertThatThrownBy(() -> {
            messageDispatchService.dispatchMessage(account.getId(), "@amy_smith", "Outbound 4");
        }).isInstanceOf(SessionLimitReachedException.class)
          .hasMessageContaining("reached the limit of 8");
    }

    @Test
    public void testControllerDispatchSuccess() throws Exception {
        String jsonPayload = """
                {
                  "tgAccountId": %d,
                  "leadIdentifier": "@jack_sparrow",
                  "message": "Ahoy Captain!"
                }
                """.formatted(account.getId());

        mockMvc.perform(post("/api/messages/dispatch")
                        .contentType("application/json")
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Message dispatched successfully."));
    }

    @Test
    public void testControllerDailyLimitExceeded() throws Exception {
        // Set the limit to 0 to trigger immediate failure
        account.setDailyLimit(0);
        tgAccountRepository.save(account);

        String jsonPayload = """
                {
                  "tgAccountId": %d,
                  "leadIdentifier": "@jack_sparrow",
                  "message": "Ahoy Captain!"
                }
                """.formatted(account.getId());

        mockMvc.perform(post("/api/messages/dispatch")
                        .contentType("application/json")
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("DAILY_LIMIT_EXCEEDED"));
    }
}
