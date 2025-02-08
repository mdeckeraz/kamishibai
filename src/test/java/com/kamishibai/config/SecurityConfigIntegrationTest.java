package com.kamishibai.config;

import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.BoardController;
import com.kamishibai.controller.HomeController;
import com.kamishibai.dto.AccountRequest;
import com.kamishibai.dto.BoardResponse;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.security.WithMockCustomUser;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        // Test /api/accounts/{id} endpoint
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "test@example.com", name = "Test User")
    void protectedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(testAccount);
        when(boardService.getBoardsForUser(any())).thenReturn(List.of(testBoard));

        // Test /api/boards endpoint
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Test /api/accounts/{id} endpoint
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void staticResources_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        // Test CSS resource
        mockMvc.perform(get("/css/styles.css")
                .accept(MediaType.parseMediaType("text/css")))
                .andExpect(status().isOk());

        // Test JavaScript resource
        mockMvc.perform(get("/js/main.js")
                .accept(MediaType.parseMediaType("application/javascript")))
                .andExpect(status().isOk());

        // Test image resource
        mockMvc.perform(get("/images/logo.png")
                .accept(MediaType.parseMediaType("image/png")))
                .andExpect(status().isOk());
    }
}
