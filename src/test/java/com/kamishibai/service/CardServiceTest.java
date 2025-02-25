package com.kamishibai.service;

import com.kamishibai.model.*;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.repository.BoardRepository;
import com.kamishibai.repository.CardAuditRepository;
import com.kamishibai.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    @Mock
    private BoardRepository boardRepository;

    private Clock clock;
    private CardService cardService;
    private Board testBoard;
    private Card testCard;

    @BeforeEach
    void setUp() {
        // Set up a fixed clock at 8:30 PM
        clock = Clock.fixed(
            LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 30)).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        cardService = new CardService(cardRepository, cardAuditRepository, clock);

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");

        testCard = new Card();
        testCard.setId(1L);
        testCard.setTitle("Test Card");
        testCard.setState(CardState.GREEN);
        testCard.setBoard(testBoard);
        testCard.setPosition(0);
        testCard.setResetTime(LocalTime.of(20, 0)); // 8 PM
    }

    private CardAudit createAudit(CardState previousState, CardState newState, LocalDateTime timestamp) {
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(previousState);
        audit.setNewState(newState);
        audit.setTimestamp(timestamp);
        return audit;
    }

    @Test
    void resetCards_Success() {
        // Given
        List<Card> cardsToReset = Arrays.asList(testCard);
        
        when(cardRepository.findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class)))
                .thenReturn(cardsToReset);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardAuditRepository.save(any(CardAudit.class))).thenReturn(new CardAudit());
        when(cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(any(Card.class), eq(CardState.GREEN)))
                .thenReturn(Optional.of(createAudit(CardState.RED, CardState.GREEN, LocalDateTime.of(LocalDate.now(), LocalTime.of(19, 0)))));

        // When
        cardService.resetCards();

        // Then
        verify(cardRepository).findByStateAndResetTimeLessThanEqual(eq(CardState.GREEN), any(LocalTime.class));
        verify(cardRepository).save(argThat(card -> card.getState() == CardState.RED));
        verify(cardAuditRepository).save(any(CardAudit.class));
    }

    @Test
    void checkAndResetCardState_ShouldNotResetRedCard() {
        // Given
        testCard.setState(CardState.RED);

        // When
        cardService.checkAndResetCardState(testCard);

        // Then
        assertEquals(CardState.RED, testCard.getState());
        verifyNoInteractions(cardAuditRepository);
    }

    @Test
    void checkAndResetCardState_ShouldNotResetCardWithNullResetTime() {
        // Given
        testCard.setResetTime(null);
        
        // When
        cardService.checkAndResetCardState(testCard);
        
        // Then
        verify(cardRepository, never()).save(any());
        verify(cardAuditRepository, never()).save(any());
        assertEquals(CardState.GREEN, testCard.getState());
    }
}
