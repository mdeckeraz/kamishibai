package com.kamishibai.security;

import com.kamishibai.config.SecurityConfig;
import com.kamishibai.config.TestSecurityConfig;
import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.BoardController;
import com.kamishibai.controller.BoardViewController;
import com.kamishibai.controller.HomeController;
import com.kamishibai.controller.RegisterController;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import com.kamishibai.service.CardService;
import com.kamishibai.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {HomeController.class, BoardController.class, BoardViewController.class, AccountController.class, RegisterController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CardService cardService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("{noop}password123");

        CustomUserDetails userDetails = new CustomUserDetails(testAccount);
        when(customUserDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(accountService.getAccount(1L)).thenReturn(testAccount);
    }

    @Test
    void publicEndpoints_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        // Test public endpoints
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test protected endpoints
        mockMvc.perform(get("/boards")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/boards")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/boards/1")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithUserDetails(value = "test@example.com", userDetailsServiceBeanName = "customUserDetailsService")
    void protectedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        // Set up test board
        Board testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");
        testBoard.setOwner(testAccount);

        when(boardService.getBoardsForUser(any())).thenReturn(Arrays.asList(testBoard));
        when(boardService.getBoardById(eq(1L), any())).thenReturn(Optional.of(testBoard));

        // Test protected endpoints with authentication
        mockMvc.perform(get("/boards"))
            .andExpect(status().isOk())
            .andExpect(view().name("boards/list"));

        mockMvc.perform(get("/api/boards"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/boards/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "test@example.com", userDetailsServiceBeanName = "customUserDetailsService")
    void homepage_ShouldRedirectToBoards_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/boards"));
    }

    @Test
    void staticResources_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/css/styles.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/dashboard.js")).andExpect(status().isOk());
    }
}
