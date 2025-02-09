package com.kamishibai.service;

import com.kamishibai.model.*;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.repository.CardAuditRepository;
import com.kamishibai.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardAuditRepository cardAuditRepository;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private Board testBoard;
    private Account testAccount;

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
    }

    @Test
    void createCard_Success() {
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card createdCard = cardService.createCard(testCard);

        assertNotNull(createdCard);
        assertEquals(CardState.RED, createdCard.getState());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void updateCard_Success() {
        Card updatedCard = new Card();
        updatedCard.setTitle("Updated Title");
        updatedCard.setDetails("Updated Details");
        updatedCard.setPosition(1);
        updatedCard.setResetTime(LocalTime.of(10, 0));

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.updateCard(1L, updatedCard);

        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void toggleCardState_Success() {
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardAuditRepository.save(any(CardAudit.class))).thenReturn(new CardAudit());

        CardResponse response = cardService.toggleCardState(testCard);

        assertNotNull(response);
        assertEquals(CardState.GREEN, response.getState());
        verify(cardRepository).save(testCard);
        verify(cardAuditRepository).save(any(CardAudit.class));
    }

    @Test
    void getCardsByBoardId_Success() {
        List<Card> cards = Arrays.asList(testCard);
        when(cardRepository.findByBoardIdOrderByPosition(1L)).thenReturn(cards);

        List<Card> result = cardService.getCardsByBoardId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(cardRepository).findByBoardIdOrderByPosition(1L);
    }

    @Test
    void getCardAuditLog_Success() {
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);

        List<CardAudit> auditLog = Arrays.asList(audit);
        when(cardAuditRepository.findByCardIdOrderByChangedAtDesc(testCard.getId())).thenReturn(auditLog);

        List<CardAudit> result = cardService.getCardAuditLog(testCard);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CardState.RED, result.get(0).getPreviousState());
        assertEquals(CardState.GREEN, result.get(0).getNewState());
        verify(cardAuditRepository).findByCardIdOrderByChangedAtDesc(testCard.getId());
    }

    @Test
    void resetCards_Success() {
        List<Card> cardsToReset = Arrays.asList(testCard);
        
        when(cardRepository.findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class)))
                .thenReturn(cardsToReset);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardAuditRepository.save(any(CardAudit.class))).thenReturn(new CardAudit());

        cardService.resetCards();

        verify(cardRepository).findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class));
        verify(cardRepository).save(testCard);
        verify(cardAuditRepository).save(any(CardAudit.class));
    }
}
