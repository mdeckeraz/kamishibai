package com.kamishibai.controller;

import com.kamishibai.config.TestDatabaseConfig;
import com.kamishibai.config.TestSecurityConfig;
import com.kamishibai.dto.BoardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@Import({TestSecurityConfig.class, TestDatabaseConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

    @MockBean
    private AccountService accountService;

    private Board testBoard;
    private Account testAccount;
    private CustomUserDetails userDetails;
    private BoardRequest testBoardRequest;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setName("Test User");

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");
        testBoard.setDescription("Test Description");
        testBoard.setOwner(testAccount);

        testBoardRequest = new BoardRequest();
        testBoardRequest.setName("Test Board");
        testBoardRequest.setDescription("Test Description");

        userDetails = new CustomUserDetails(testAccount);

        when(accountService.getAccount(1L)).thenReturn(testAccount);
        when(accountService.getAccountByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
    }

    @Test
    void createBoard_ShouldCreateBoard_WhenAuthenticated() throws Exception {
        when(boardService.createBoard(any(Board.class), any(Account.class))).thenReturn(testBoard);

        mockMvc.perform(post("/api/boards")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testBoardRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Board"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void getBoards_ShouldReturnUserBoards_WhenAuthenticated() throws Exception {
        when(boardService.getBoardsForUser(any(Account.class))).thenReturn(Arrays.asList(testBoard));

        mockMvc.perform(get("/api/boards")
                .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Board"))
                .andExpect(jsonPath("$[0].description").value("Test Description"));
    }
}
