package com.kamishibai.security;

import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.HomeController;
import com.kamishibai.dto.AccountRequest;
import com.kamishibai.model.Account;
import com.kamishibai.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AccountController.class, HomeController.class})
@Import(com.kamishibai.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application.properties")
class SecurityTest {

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
    void publicEndpoints_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        when(accountService.createAccount(any())).thenReturn(testAccount);

        // Test /api/accounts/register endpoint
        mockMvc.perform(post("/api/accounts/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAccountRequest)))
                .andExpect(status().isOk());

        // Test /login endpoint
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        // Test /register endpoint
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test /api/accounts/{id} endpoint
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void protectedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(testAccount);

        // Test /api/accounts/{id} endpoint
        mockMvc.perform(get("/api/accounts/1")
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
