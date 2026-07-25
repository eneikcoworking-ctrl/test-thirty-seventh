package com.eneik.generated.controller;

import com.eneik.generated.domain.User;
import com.eneik.generated.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        // Setup default admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        userRepository.save(admin);
    }

    @Test
    public void testUnauthorizedAccessToSecureEndpoint_Returns401() throws Exception {
        // Given an unauthenticated client invoking secure endpoints
        // Then the system returns an HTTP 401 Unauthorized response
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Access denied. Please log in.")));
    }

    @Test
    public void testLoginWithValidCredentials_Returns200AndSetsSession() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "admin");
        credentials.put("password", "admin");

        // When client authenticates with valid credentials
        // Then access is allowed and session is established
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.message", is("Successfully authenticated")));
    }

    @Test
    public void testLoginWithInvalidCredentials_Returns401() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "admin");
        credentials.put("password", "wrongpassword");

        // When user provides invalid credentials
        // Then access is denied and a 401 response is returned with error
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Invalid username or password.")));
    }

    @Test
    public void testAuthStatusAndSessionLifecycle() throws Exception {
        // 1. Initial status when unauthenticated
        mockMvc.perform(get("/api/v1/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        // 2. Perform successful login and capture session
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "admin");
        credentials.put("password", "admin");

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

        // 3. Get status with authenticated session
        mockMvc.perform(get("/api/v1/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.username", is("admin")));

        // 4. Access secure endpoint with authenticated session
        mockMvc.perform(get("/api/v1/conversations").session(session))
                .andExpect(status().isOk()); // Interceptor should allow

        // 5. Log out
        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isOk());

        // 6. Verify status is now unauthenticated (forces re-authentication)
        mockMvc.perform(get("/api/v1/auth/status").session(session))
                .andExpect(status().isUnauthorized());
    }
}
