package com.kamishibai.service;

import com.kamishibai.model.*;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.repository.CardAuditRepository;
import com.kamishibai.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardAuditRepository cardAuditRepository;

    private CardService cardService;
    private Card testCard;
    private Board testBoard;
    private Account testAccount;
    private Clock clock;

    @BeforeEach
    void setUp() {
        // Fix the time to 10 PM for all tests
        LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 18, 22, 0);
        clock = Clock.fixed(
            fixedTime.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        cardService = new CardService(cardRepository, cardAuditRepository, clock);

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
        testCard.setState(CardState.GREEN);
        testCard.setBoard(testBoard);
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
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        // Set reset time to 2 hours before current time
        LocalTime resetTime = currentTime.minusHours(2);
        
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(resetTime);

        // Set last state change to 3 hours ago (before reset time)
        LocalDateTime lastStateChange = now.minusHours(3);
        
        CardAudit lastAudit = new CardAudit();
        lastAudit.setCard(card);
        lastAudit.setTimestamp(lastStateChange);
        lastAudit.setNewState(CardState.GREEN);

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
    void checkAndResetCardState_ShouldNotResetGreenCardBeforeResetTime() {
        // Given
        LocalDateTime now = LocalDateTime.now(clock);
        LocalTime currentTime = now.toLocalTime();
        
        // Set reset time to 1 hour after current time
        LocalTime resetTime = currentTime.plusHours(1);
        
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(resetTime);

        // Set last state change to 30 minutes ago
        LocalDateTime lastStateChange = now.minusMinutes(30);
        
        CardAudit lastAudit = new CardAudit();
        lastAudit.setCard(card);
        lastAudit.setTimestamp(lastStateChange);
        lastAudit.setNewState(CardState.GREEN);

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
    void checkAndResetCardState_ShouldNotResetRedCard() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.RED);
        card.setResetTime(currentTime.minusHours(1)); // Even with past reset time, RED cards don't reset

        // When
        cardService.checkAndResetCardState(card);

        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.RED, card.getState());
    }

    @Test
    void shouldNotResetCard_WhenStateIsRed() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        testCard.setState(CardState.RED);
        testCard.setResetTime(currentTime.minusHours(1)); // Even with past reset time, RED cards don't reset
        
        // When
        cardService.checkAndResetCardState(testCard);
        
        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.RED, testCard.getState());
    }

    @Test
    void shouldNotResetCard_WhenResetTimeIsNull() {
        // Given
        testCard.setState(CardState.GREEN);
        testCard.setResetTime(null);
        
        // When
        cardService.checkAndResetCardState(testCard);
        
        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.GREEN, testCard.getState());
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
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        // Set up a card that needs resetting
        testCard.setState(CardState.GREEN);
        testCard.setResetTime(currentTime.minusHours(1)); // Reset time was 1 hour ago
        
        List<Card> cardsToReset = Arrays.asList(testCard);
        
        when(cardRepository.findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class)))
                .thenReturn(cardsToReset);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardAuditRepository.save(any(CardAudit.class))).thenReturn(new CardAudit());

        // When
        cardService.resetCards();

        // Then
        verify(cardRepository).findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class));
        verify(cardRepository).save(testCard);
        verify(cardAuditRepository).save(any(CardAudit.class));
    }

    @Test
    void shouldResetAllCards_WhenGettingCardsByBoard() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        // Set up a card that needs resetting
        Card cardToReset = new Card();
        cardToReset.setId(2L);
        cardToReset.setState(CardState.GREEN);
        cardToReset.setResetTime(currentTime.minusHours(1)); // Reset time was 1 hour ago
        
        // Last change was 2 hours ago (before reset time)
        LocalDateTime lastChange = now.minusHours(2);
        CardAudit testAudit = new CardAudit();
        testAudit.setCard(cardToReset);
        testAudit.setPreviousState(CardState.RED);
        testAudit.setNewState(CardState.GREEN);
        testAudit.setTimestamp(lastChange);
        
        when(cardRepository.findByBoardIdOrderByPosition(1L))
            .thenReturn(Arrays.asList(cardToReset));
        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(cardToReset, CardState.GREEN))
            .thenReturn(Optional.of(testAudit));
        
        // When
        List<Card> cards = cardService.getCardsByBoardId(1L);
        
        // Then
        verify(cardRepository).save(cardToReset);
        assertEquals(CardState.RED, cardToReset.getState());
    }

    @Test
    void shouldCreateAuditEntry_WhenResettingCard() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        
        Card card = new Card();
        card.setId(1L);
        card.setState(CardState.GREEN);
        card.setResetTime(currentTime.minusHours(1)); // Reset time was 1 hour ago

        // Last state change was 2 hours ago
        LocalDateTime lastStateChange = now.minusHours(2);
        CardAudit lastAudit = new CardAudit();
        lastAudit.setCard(card);
        lastAudit.setTimestamp(lastStateChange);
        lastAudit.setNewState(CardState.GREEN);

        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN))
            .thenReturn(Optional.of(lastAudit));

        // When
        cardService.checkAndResetCardState(card);

        // Then
        ArgumentCaptor<CardAudit> auditCaptor = ArgumentCaptor.forClass(CardAudit.class);
        verify(cardAuditRepository).save(auditCaptor.capture());
        
        CardAudit savedAudit = auditCaptor.getValue();
        assertEquals(CardState.GREEN, savedAudit.getPreviousState());
        assertEquals(CardState.RED, savedAudit.getNewState());
        assertEquals(card, savedAudit.getCard());
        assertNotNull(savedAudit.getTimestamp());
    }
}
