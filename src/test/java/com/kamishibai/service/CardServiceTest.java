package com.kamishibai.service;

import com.kamishibai.model.*;
import com.kamishibai.repository.*;
import com.kamishibai.dto.CardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardAuditRepository cardAuditRepository;

    private CardService cardService;
    private Card testCard;
    private Board testBoard;
    private Account testAccount;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        // Fix the clock to 2025-02-09 23:15:00 PST
        fixedClock = Clock.fixed(
            LocalDateTime.of(2025, 2, 9, 23, 15, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        cardService = new CardService(cardRepository, cardAuditRepository, fixedClock);

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setEmail("test@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"); // "password" hashed

        testBoard.setOwner(testAccount);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setTitle("Test Card");
        testCard.setDetails("Test Details");
        testCard.setBoard(testBoard);
        testCard.setState(CardState.GREEN);
        testCard.setResetTime(LocalTime.of(23, 0)); // 11:00 PM
    }

    @Test
    void createCard_ShouldSetDefaultStateToRed() {
        // Given
        Card card = new Card();
        card.setTitle("Test Card");
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        // When
        Card createdCard = cardService.createCard(card);

        // Then
        assertEquals(CardState.RED, createdCard.getState());
        verify(cardRepository).save(card);
    }

    @Test
    void updateCard_ShouldCreateAuditLogWhenStateChanges() {
        // Given
        Card existingCard = new Card();
        existingCard.setId(1L);
        existingCard.setState(CardState.RED);

        Card updatedCard = new Card();
        updatedCard.setState(CardState.GREEN);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(any(Card.class))).thenReturn(existingCard);
        when(cardAuditRepository.save(any(CardAudit.class))).thenReturn(new CardAudit());

        // When
        cardService.updateCard(1L, updatedCard);

        // Then
        verify(cardAuditRepository).save(any(CardAudit.class));
        verify(cardRepository).save(existingCard);
        assertEquals(CardState.GREEN, existingCard.getState());
    }

    @Test
    void checkAndResetCardState_ShouldResetGreenCardAfterResetTime() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(LocalTime.of(10, 0)); // 10:00 AM

        CardAudit lastAudit = new CardAudit();
        lastAudit.setTimestamp(LocalDateTime.of(2025, 2, 9, 9, 0)); // 9:00 AM today

        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN))
            .thenReturn(Optional.of(lastAudit));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository).save(card);
        verify(cardAuditRepository).save(any(CardAudit.class));
        assertEquals(CardState.RED, card.getState());
    }

    @Test
    void checkAndResetCardState_ShouldNotResetRedCard() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.RED);
        card.setResetTime(LocalTime.of(10, 0));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.RED, card.getState());
    }

    @Test
    void checkAndResetCardState_ShouldNotResetGreenCardBeforeResetTime() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(LocalTime.of(23, 30)); // 11:30 PM

        CardAudit lastAudit = new CardAudit();
        lastAudit.setTimestamp(LocalDateTime.of(2025, 2, 9, 23, 12)); // 11:12 PM today

        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN))
            .thenReturn(Optional.of(lastAudit));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.GREEN, card.getState());
    }

    @Test
    void createCard_Success() {
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        
        Card createdCard = cardService.createCard(testCard);

        assertNotNull(createdCard);
        assertEquals(CardState.RED, createdCard.getState()); // New cards should start as RED
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
        assertEquals(CardState.RED, response.getState());
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
        List<CardAudit> auditLog = Arrays.asList(new CardAudit(), new CardAudit());
        when(cardAuditRepository.findByCardOrderByTimestampDesc(testCard)).thenReturn(auditLog);

        List<CardAudit> result = cardService.getCardAuditLog(testCard);

        assertNotNull(result);
        assertEquals(auditLog.size(), result.size());
        verify(cardAuditRepository).findByCardOrderByTimestampDesc(testCard);
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

    @Test
    void shouldNotResetCard_WhenStateIsRed() {
        testCard.setState(CardState.RED);
        
        cardService.checkAndResetCardState(testCard);
        
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.RED, testCard.getState());
    }

    @Test
    void shouldNotResetCard_WhenResetTimeIsNull() {
        testCard.setResetTime(null);
        
        cardService.checkAndResetCardState(testCard);
        
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.GREEN, testCard.getState());
    }

    @Test
    void shouldNotResetCard_BeforeResetTime() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(LocalTime.of(23, 30)); // 11:30 PM

        CardAudit lastAudit = new CardAudit();
        lastAudit.setTimestamp(LocalDateTime.of(2025, 2, 9, 23, 12)); // 11:12 PM today

        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN))
            .thenReturn(Optional.of(lastAudit));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.GREEN, card.getState());
    }

    @Test
    void shouldResetCard_AfterResetTime() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(LocalTime.of(10, 0)); // 10:00 AM

        // Set up last state change to be yesterday at 11:00 AM
        LocalDateTime lastStateChange = LocalDateTime.now().minusDays(1).withHour(11).withMinute(0);
        CardAudit lastAudit = new CardAudit();
        lastAudit.setTimestamp(lastStateChange);
        lastAudit.setNewState(CardState.GREEN);

        // Current time is 23:15, which is after the reset time of 23:00
        when(cardRepository.findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any()))
            .thenReturn(Arrays.asList(card));

        // When
        cardService.resetCards();

        // Then
        // Verify card was saved with RED state
        ArgumentCaptor<Card> savedCardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(savedCardCaptor.capture());
        Card savedCard = savedCardCaptor.getValue();
        assertEquals(CardState.RED, savedCard.getState());

        // Verify audit entry was created
        ArgumentCaptor<CardAudit> auditCaptor = ArgumentCaptor.forClass(CardAudit.class);
        verify(cardAuditRepository).save(auditCaptor.capture());
        CardAudit savedAudit = auditCaptor.getValue();
        assertEquals(CardState.GREEN, savedAudit.getPreviousState());
        assertEquals(CardState.RED, savedAudit.getNewState());
        assertEquals(card, savedAudit.getCard());
        
        // Verify timestamp is from our fixed clock
        LocalDateTime expectedTime = LocalDateTime.of(2025, 2, 9, 23, 15, 0);
        assertEquals(expectedTime, savedAudit.getTimestamp());
    }

    @Test
    void shouldResetAllCards_WhenGettingCardsByBoard() {
        // Set up a card that needs resetting
        Card cardToReset = new Card();
        cardToReset.setId(2L);
        cardToReset.setState(CardState.GREEN);
        cardToReset.setResetTime(LocalTime.of(10, 0)); // 10:00 AM
        
        // Last change was at 9:00 AM (before reset time)
        LocalDateTime lastChange = LocalDateTime.of(2025, 2, 9, 9, 0); // 9:00 AM
        CardAudit testAudit = new CardAudit();
        testAudit.setCard(cardToReset);
        testAudit.setPreviousState(CardState.RED);
        testAudit.setNewState(CardState.GREEN);
        testAudit.setTimestamp(lastChange);
        
        when(cardRepository.findByBoardIdOrderByPosition(1L))
            .thenReturn(Arrays.asList(cardToReset));
        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(cardToReset, CardState.GREEN))
            .thenReturn(Optional.of(testAudit));
        
        List<Card> cards = cardService.getCardsByBoardId(1L);
        
        verify(cardRepository).save(cardToReset);
        assertEquals(CardState.RED, cardToReset.getState());
    }

    @Test
    void shouldCreateAuditEntry_WhenResettingCard() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(LocalTime.of(10, 0)); // 10:00 AM

        CardAudit lastAudit = new CardAudit();
        lastAudit.setTimestamp(LocalDateTime.of(2025, 2, 9, 9, 0)); // 9:00 AM today

        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN))
            .thenReturn(Optional.of(lastAudit));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository).save(card);
        verify(cardAuditRepository).save(any(CardAudit.class));
        assertEquals(CardState.RED, card.getState());
    }
}
