package com.kamishibai.integration;

import com.kamishibai.model.*;
import com.kamishibai.repository.*;
import com.kamishibai.service.CardService;
import com.kamishibai.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false"
})
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
public class CardResetIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardAuditRepository cardAuditRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Clock clock;

    private Board testBoard;
    private Card testCard;
    private Account testAccount;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock clock() {
            // Set up a fixed clock at 8:30 PM
            return Clock.fixed(
                LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 30)).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
            );
        }
    }

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        cardAuditRepository.deleteAll();
        cardRepository.deleteAll();
        boardRepository.deleteAll();
        accountRepository.deleteAll();

        // Create test account
        testAccount = new Account();
        testAccount.setEmail("test_" + System.currentTimeMillis() + "@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"); // Test password hash
        testAccount = accountRepository.save(testAccount);

        // Create test board
        testBoard = new Board();
        testBoard.setName("Test Board");
        testBoard.setOwner(testAccount);
        testBoard = boardRepository.save(testBoard);

        // Create test card
        testCard = new Card();
        testCard.setTitle("Test Card");
        testCard.setState(CardState.GREEN);
        testCard.setPosition(0);
        testCard.setBoard(testBoard);
        testCard.setResetTime(LocalTime.of(20, 0)); // 8 PM
        testCard = cardRepository.save(testCard);

        // Create an initial audit entry at 6 PM
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 0))); // 6 PM
        cardAuditRepository.save(audit);
    }

    @Test
    void shouldNotResetCardState_BeforeResetTime() {
        // Create an audit entry at 7:30 PM (30 minutes before reset time)
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 30))); // 7:30 PM
        cardAuditRepository.save(audit);

        // Temporarily set the clock to 7:45 PM
        Clock tempClock = Clock.fixed(
            LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 45)).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        cardService = new CardService(cardRepository, cardAuditRepository, tempClock);

        // Get the card at 7:45 PM (before reset time)
        Card retrievedCard = cardService.getCard(testCard.getId());

        // Verify the card was not reset
        assertEquals(CardState.GREEN, retrievedCard.getState());

        // Verify no new audit entry was created
        List<CardAudit> auditEntries = cardAuditRepository.findByCardOrderByTimestampDesc(testCard);
        assertEquals(2, auditEntries.size());
    }

    @Test
    void shouldResetCardState_WhenAccessedAfterResetTime() {
        // Set reset time to 8 PM
        testCard.setResetTime(LocalTime.of(20, 0)); // 8 PM
        cardRepository.save(testCard);

        // Create an audit entry at 7 PM (1 hour before reset time)
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 0))); // 7 PM
        cardAuditRepository.save(audit);

        // Get the card at 8:30 PM (after reset time)
        Card retrievedCard = cardService.getCard(testCard.getId());

        // Verify the card was reset to RED
        assertEquals(CardState.RED, retrievedCard.getState());

        // Verify a new audit entry was created
        List<CardAudit> auditEntries = cardAuditRepository.findByCardOrderByTimestampDesc(testCard);
        assertEquals(3, auditEntries.size());
        assertEquals(CardState.RED, auditEntries.get(0).getNewState());
    }

    @Test
    void shouldResetMultipleCards_WhenAccessingBoard() {
        // Create another card
        Card card2 = new Card();
        card2.setTitle("Test Card 2");
        card2.setState(CardState.GREEN);
        card2.setPosition(1);
        card2.setBoard(testBoard);
        card2.setResetTime(LocalTime.of(20, 0)); // Set reset time to 8 PM
        card2 = cardRepository.save(card2);

        // Create audit entries at 7 PM for both cards
        CardAudit audit1 = new CardAudit();
        audit1.setCard(testCard);
        audit1.setPreviousState(CardState.RED);
        audit1.setNewState(CardState.GREEN);
        audit1.setTimestamp(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 0))); // 7 PM
        cardAuditRepository.save(audit1);

        CardAudit audit2 = new CardAudit();
        audit2.setCard(card2);
        audit2.setPreviousState(CardState.RED);
        audit2.setNewState(CardState.GREEN);
        audit2.setTimestamp(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 0))); // 7 PM
        cardAuditRepository.save(audit2);

        // Get all cards at 8:30 PM (after reset time)
        List<Card> retrievedCards = cardService.getCardsByBoardId(testBoard.getId());

        // Verify both cards were reset to RED
        assertEquals(2, retrievedCards.size());
        assertEquals(CardState.RED, retrievedCards.get(0).getState());
        assertEquals(CardState.RED, retrievedCards.get(1).getState());

        // Verify new audit entries were created
        List<CardAudit> auditEntries1 = cardAuditRepository.findByCardOrderByTimestampDesc(testCard);
        List<CardAudit> auditEntries2 = cardAuditRepository.findByCardOrderByTimestampDesc(card2);
        assertEquals(3, auditEntries1.size());
        assertEquals(2, auditEntries2.size());
        assertEquals(CardState.RED, auditEntries1.get(0).getNewState());
        assertEquals(CardState.RED, auditEntries2.get(0).getNewState());
    }

    @Test
    void shouldPreserveCardOrder_WhenResettingMultipleCards() {
        // Create two more cards with different positions
        Card card2 = new Card();
        card2.setTitle("Test Card 2");
        card2.setState(CardState.GREEN);
        card2.setPosition(1);
        card2.setBoard(testBoard);
        card2.setResetTime(LocalTime.of(20, 0)); // Set reset time to 8 PM
        card2 = cardRepository.save(card2);

        Card card3 = new Card();
        card3.setTitle("Test Card 3");
        card3.setState(CardState.GREEN);
        card3.setPosition(2);
        card3.setBoard(testBoard);
        card3.setResetTime(LocalTime.of(20, 0)); // Set reset time to 8 PM
        card3 = cardRepository.save(card3);

        // Create audit entries at 7 PM for all cards
        LocalDateTime auditTime = LocalDateTime.of(LocalDate.now(clock), LocalTime.of(19, 0)); // 7 PM
        for (Card card : Arrays.asList(testCard, card2, card3)) {
            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(CardState.RED);
            audit.setNewState(CardState.GREEN);
            audit.setTimestamp(auditTime);
            cardAuditRepository.save(audit);
        }

        // Get all cards at 8:30 PM (after reset time)
        List<Card> retrievedCards = cardService.getCardsByBoardId(testBoard.getId());

        // Verify cards were reset but maintained their order
        assertEquals(3, retrievedCards.size());
        assertEquals(0, retrievedCards.get(0).getPosition());
        assertEquals(1, retrievedCards.get(1).getPosition());
        assertEquals(2, retrievedCards.get(2).getPosition());
        assertEquals(CardState.RED, retrievedCards.get(0).getState());
        assertEquals(CardState.RED, retrievedCards.get(1).getState());
        assertEquals(CardState.RED, retrievedCards.get(2).getState());
    }
}
