package com.kamishibai.config;

import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.BoardController;
import com.kamishibai.controller.HomeController;
import com.kamishibai.dto.AccountRequest;
import com.kamishibai.dto.BoardResponse;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest({AccountController.class, BoardController.class, HomeController.class})
@Import({TestSecurityConfig.class, TestWebMvcConfig.class})
@TestPropertySource(locations = "classpath:application.properties")
@ActiveProfiles("test")
public class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private BoardService boardService;

    private AccountRequest testAccountRequest;
    private Account testAccount;
    private Board testBoard;
    private BoardResponse testBoardResponse;

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

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");
        testBoard.setOwner(testAccount);

        testBoardResponse = new BoardResponse();
        testBoardResponse.setId(1L);
        testBoardResponse.setName("Test Board");
        testBoardResponse.setOwnerId(1L);
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
        // Test /api/boards endpoint
        mockMvc.perform(get("/api/boards")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        // Test /api/boards/{id} endpoint
        mockMvc.perform(get("/api/boards/1")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        // Test /dashboard endpoint
        mockMvc.perform(get("/dashboard")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithUserDetails("test@example.com")
    void protectedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        // Mock accountService to return the test account for both ID and email lookups
        when(accountService.getAccount(1L)).thenReturn(testAccount);
        when(accountService.getAccountByEmail("test@example.com")).thenReturn(java.util.Optional.of(testAccount));
        
        // Mock boardService responses
        when(boardService.getBoardsForUser(testAccount)).thenReturn(List.of(testBoard));
        when(boardService.getBoardById(1L, testAccount)).thenReturn(java.util.Optional.of(testBoard));

        // Test /dashboard endpoint
        mockMvc.perform(get("/dashboard")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        // Test /api/boards endpoint
        mockMvc.perform(get("/api/boards")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Board"));

        // Test /api/boards/{id} endpoint
        mockMvc.perform(get("/api/boards/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Board"));
    }

    @Test
    void staticResources_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        // Test CSS resource
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk());

        // Test JS resource
        mockMvc.perform(get("/js/main.js"))
                .andExpect(status().isOk());

        // Test images resource
        mockMvc.perform(get("/images/logo.png"))
                .andExpect(status().isOk());
    }
}
