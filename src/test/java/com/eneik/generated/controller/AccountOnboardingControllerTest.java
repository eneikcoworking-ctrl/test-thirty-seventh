package com.eneik.generated.controller;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import com.eneik.generated.repository.ProxyRepository;
import com.eneik.generated.repository.TgAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @BeforeEach
    public void setup() {
        tgAccountRepository.deleteAll();
        proxyRepository.deleteAll();
    }

    @Test
    public void testOnboardWithOtp_Success() throws Exception {
        String jsonPayload = """
                {
                    "phoneNumber": "+1234567890",
                    "otpCode": "54321",
                    "proxyIp": "127.0.0.1",
                    "proxyPort": 1080,
                    "proxyProtocol": "SOCKS5",
                    "proxyUsername": "user",
                    "proxyPassword": "pass"
                }
                """;

        mockMvc.perform(post("/api/accounts/onboard/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account successfully registered"))
                .andExpect(jsonPath("$.status").value("Active"));

        // Verify database state
        Optional<TgAccount> accountOpt = tgAccountRepository.findByPhoneNumber("+1234567890");
        assertThat(accountOpt).isPresent();
        TgAccount account = accountOpt.get();
        assertThat(account.getStatus()).isEqualTo("Active");
        assertThat(account.getSessionData()).isEqualTo("otp-auth-session-code:54321");
        assertThat(account.getProxy()).isNotNull();
        assertThat(account.getProxy().getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(account.getProxy().getPort()).isEqualTo(1080);
        assertThat(account.getProxy().getProtocol()).isEqualTo("SOCKS5");
    }

    @Test
    public void testOnboardWithOtp_InvalidProxy_Rejects() throws Exception {
        String jsonPayload = """
                {
                    "phoneNumber": "+1234567890",
                    "otpCode": "54321",
                    "proxyIp": "invalid-ip",
                    "proxyPort": 999999,
                    "proxyProtocol": "SOCKS5"
                }
                """;

        mockMvc.perform(post("/api/accounts/onboard/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid proxy configuration or connectivity check failed"));

        // Verify DB contains nothing
        assertThat(tgAccountRepository.findAll()).isEmpty();
        assertThat(proxyRepository.findAll()).isEmpty();
    }

    @Test
    public void testOnboardWithOtp_InvalidOtp_Rejects() throws Exception {
        String jsonPayload = """
                {
                    "phoneNumber": "+1234567890",
                    "otpCode": "invalid-otp",
                    "proxyIp": "127.0.0.1",
                    "proxyPort": 1080,
                    "proxyProtocol": "SOCKS5"
                }
                """;

        mockMvc.perform(post("/api/accounts/onboard/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid OTP code provided"));
    }

    @Test
    public void testOnboardWithSessionFile_Success() throws Exception {
        MockMultipartFile sessionFile = new MockMultipartFile(
                "sessionFile",
                "tele.session",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "my-telegram-session-file-binary-data".getBytes()
        );

        mockMvc.perform(multipart("/api/accounts/onboard/session")
                        .file(sessionFile)
                        .param("phoneNumber", "+9876543210")
                        .param("proxyIp", "127.0.0.1")
                        .param("proxyPort", "8080")
                        .param("proxyProtocol", "HTTP")
                        .param("proxyUsername", "testUser")
                        .param("proxyPassword", "testPass"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account successfully registered from session file"))
                .andExpect(jsonPath("$.status").value("Active"));

        // Verify database state
        Optional<TgAccount> accountOpt = tgAccountRepository.findByPhoneNumber("+9876543210");
        assertThat(accountOpt).isPresent();
        TgAccount account = accountOpt.get();
        assertThat(account.getStatus()).isEqualTo("Active");
        assertThat(account.getSessionData()).isEqualTo("my-telegram-session-file-binary-data");
        assertThat(account.getProxy()).isNotNull();
        assertThat(account.getProxy().getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(account.getProxy().getProtocol()).isEqualTo("HTTP");
    }

    @Test
    public void testOnboardWithSessionFile_InvalidProxy_Rejects() throws Exception {
        MockMultipartFile sessionFile = new MockMultipartFile(
                "sessionFile",
                "tele.session",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "data".getBytes()
        );

        mockMvc.perform(multipart("/api/accounts/onboard/session")
                        .file(sessionFile)
                        .param("phoneNumber", "+9876543210")
                        .param("proxyIp", "invalid")
                        .param("proxyPort", "8080")
                        .param("proxyProtocol", "UNKNOWN_PROTOCOL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid proxy configuration or connectivity check failed"));

        assertThat(tgAccountRepository.findAll()).isEmpty();
    }
}
