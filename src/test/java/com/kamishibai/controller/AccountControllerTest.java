package com.kamishibai.controller;

import com.kamishibai.config.TestDatabaseConfig;
import com.kamishibai.config.TestSecurityConfig;
import com.kamishibai.config.TestWebConfig;
import com.kamishibai.dto.AccountRequest;
import com.kamishibai.model.Account;
import com.kamishibai.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({TestSecurityConfig.class, TestDatabaseConfig.class, TestWebConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountRequest testAccountRequest;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccountRequest = new AccountRequest();
        testAccountRequest.setEmail("test@example.com");
        testAccountRequest.setName("Test User");
        testAccountRequest.setPassword("password123");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("hashedPassword");
    }

    @Test
    void createAccount_ShouldCreateAccount_WhenValidRequest() throws Exception {
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAccountRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void createAccount_ShouldReturnBadRequest_WhenEmailExists() throws Exception {
        when(accountService.createAccount(any(AccountRequest.class)))
            .thenThrow(new IllegalArgumentException("An account with this email already exists"));

        mockMvc.perform(post("/api/accounts/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAccountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }
}
