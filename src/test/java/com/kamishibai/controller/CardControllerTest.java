package com.kamishibai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kamishibai.config.TestSecurityConfig;
import com.kamishibai.dto.CardRequest;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.model.*;
import com.kamishibai.repository.BoardRepository;
import com.kamishibai.repository.CardRepository;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@Import(TestSecurityConfig.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private CardRepository cardRepository;

    @MockBean
    private BoardRepository boardRepository;

    private Account testAccount;
    private Board testBoard;
    private Card testCard;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("hashedPassword");

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");
        testBoard.setOwner(testAccount);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setTitle("Test Card");
        testCard.setDetails("Test Details");
        testCard.setPosition(0);
        testCard.setState(CardState.RED);
        testCard.setBoard(testBoard);
        testCard.setResetTime(LocalTime.of(9, 0));

        userDetails = new CustomUserDetails(testAccount);

        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
    }

    @Test
    @WithMockUser
    void createCard_Success() throws Exception {
        CardRequest request = new CardRequest();
        request.setTitle("Test Card");
        request.setDetails("Test Details");
        request.setState(CardState.RED);
        request.setPosition(0);
        request.setResetTime(LocalTime.of(9, 0));

        when(cardService.createCard(any(Card.class))).thenReturn(testCard);

        mockMvc.perform(post("/api/boards/1/cards")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard.getId()))
                .andExpect(jsonPath("$.message").value("Card created successfully"));

        verify(cardService).createCard(any(Card.class));
    }

    @Test
    @WithMockUser
    void toggleCardState_Success() throws Exception {
        CardResponse cardResponse = new CardResponse(1L, CardState.GREEN);
        when(cardService.toggleCardState(any(Card.class))).thenReturn(cardResponse);

        mockMvc.perform(post("/api/boards/1/cards/1/toggle")
                .with(csrf())
                .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.state").value(CardState.GREEN.toString()));

        verify(cardService).toggleCardState(any(Card.class));
    }

    @Test
    @WithMockUser
    void getCardAuditLog_Success() throws Exception {
        CardAudit audit = new CardAudit();
        audit.setId(1L);
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);

        when(cardService.getCardAuditLog(any(Card.class)))
                .thenReturn(Arrays.asList(audit));

        mockMvc.perform(get("/api/boards/1/cards/1/audit")
                .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousState").value("RED"))
                .andExpect(jsonPath("$[0].newState").value("GREEN"));

        verify(cardService).getCardAuditLog(any(Card.class));
    }

    @Test
    @WithMockUser
    void updateCard_Success() throws Exception {
        CardRequest request = new CardRequest();
        request.setTitle("Updated Card");
        request.setDetails("Updated Details");
        request.setPosition(1);
        request.setResetTime(LocalTime.of(10, 0));

        when(cardService.updateCard(eq(1L), any(Card.class))).thenReturn(testCard);

        mockMvc.perform(put("/api/boards/1/cards/1")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cardService).updateCard(eq(1L), any(Card.class));
    }

    @Test
    @WithMockUser
    void accessDenied_WhenUserIsNotBoardOwner() throws Exception {
        Account otherAccount = new Account();
        otherAccount.setId(2L);
        otherAccount.setEmail("other@example.com");
        
        Board board = new Board();
        board.setId(1L);
        board.setOwner(otherAccount);
        
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        mockMvc.perform(get("/api/boards/1/cards")
                .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void accessAllowed_WhenBoardIsShared() throws Exception {
        Board board = new Board();
        board.setId(1L);
        Account owner = new Account();
        owner.setId(2L);
        board.setOwner(owner);
        board.getSharedWith().add(testAccount);
        
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        mockMvc.perform(get("/api/boards/1/cards")
                .with(user(userDetails)))
                .andExpect(status().isOk());
    }
}
