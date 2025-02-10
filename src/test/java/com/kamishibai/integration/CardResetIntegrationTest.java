package com.kamishibai.integration;

import com.kamishibai.model.*;
import com.kamishibai.repository.*;
import com.kamishibai.service.CardService;
import com.kamishibai.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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

    @BeforeEach
    void setUp() {
        // Create a test account
        testAccount = new Account();
        testAccount.setEmail("test_" + System.currentTimeMillis() + "@example.com");
        testAccount.setName("Test User");
        testAccount.setPasswordHash("hashedPassword");
        testAccount = accountRepository.save(testAccount);

        // Create a test board
        testBoard = new Board();
        testBoard.setName("Test Board");
        testBoard.setOwner(testAccount);
        testBoard = boardRepository.save(testBoard);

        // Create a test card with reset time before current time (23:15)
        testCard = new Card();
        testCard.setTitle("Test Card");
        testCard.setDetails("Test Details");
        testCard.setBoard(testBoard);
        testCard.setState(CardState.GREEN);
        testCard.setResetTime(LocalTime.of(23, 0)); // 11:00 PM
        testCard.setPosition(0);
        testCard = cardRepository.save(testCard);

        // Create an audit entry for the card
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit);
    }

    @Test
    void shouldNotResetCardState_BeforeResetTime() {
        // Create an audit entry for 30 minutes ago
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.now(clock).minusMinutes(30));
        cardAuditRepository.save(audit);

        // Get the card
        Card retrievedCard = cardService.getCard(testCard.getId());

        // Verify the card was not reset
        assertEquals(CardState.GREEN, retrievedCard.getState());

        // Verify no new audit entry was created
        List<CardAudit> auditEntries = cardAuditRepository.findByCardOrderByTimestampDesc(testCard);
        assertEquals(2, auditEntries.size());
    }

    @Test
    void shouldResetCardState_WhenAccessedAfterResetTime() {
        // Set reset time to 30 minutes ago
        testCard.setResetTime(LocalTime.now(clock).minusMinutes(30));
        cardRepository.save(testCard);

        // Create an audit entry for 1 hour ago (before reset time)
        CardAudit audit = new CardAudit();
        audit.setCard(testCard);
        audit.setPreviousState(CardState.RED);
        audit.setNewState(CardState.GREEN);
        audit.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit);

        // Get the card
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
        card2.setResetTime(LocalTime.now(clock).minusMinutes(30)); // Set reset time to 30 minutes ago
        card2 = cardRepository.save(card2);

        // Create audit entries for 1 hour ago
        CardAudit audit1 = new CardAudit();
        audit1.setCard(testCard);
        audit1.setPreviousState(CardState.RED);
        audit1.setNewState(CardState.GREEN);
        audit1.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit1);

        CardAudit audit2 = new CardAudit();
        audit2.setCard(card2);
        audit2.setPreviousState(CardState.RED);
        audit2.setNewState(CardState.GREEN);
        audit2.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit2);

        // Set testCard's reset time to 30 minutes ago
        testCard.setResetTime(LocalTime.now(clock).minusMinutes(30));
        cardRepository.save(testCard);

        // Get all cards from the board
        List<Card> cards = cardService.getCardsByBoardId(testBoard.getId());

        // Verify all cards were reset
        assertTrue(cards.stream().allMatch(card -> card.getState() == CardState.RED));

        // Verify audit entries were created
        List<CardAudit> auditEntries = cardAuditRepository.findByCardInOrderByTimestampDesc(cards);
        assertEquals(5, auditEntries.size()); // Updated to 5 to account for initial audit entry from setUp
        assertTrue(auditEntries.stream()
            .filter(audit -> audit.getTimestamp().isAfter(LocalDateTime.now(clock).minusMinutes(1)))
            .allMatch(audit -> audit.getNewState() == CardState.RED));
    }

    @Test
    void shouldPreserveCardOrder_WhenResettingMultipleCards() {
        // Create two more cards
        Card card2 = new Card();
        card2.setTitle("Test Card 2");
        card2.setState(CardState.GREEN);
        card2.setBoard(testBoard);
        card2.setResetTime(LocalTime.now(clock).minusMinutes(30)); // Set reset time to 30 minutes ago
        card2.setPosition(1);
        card2 = cardRepository.save(card2);

        Card card3 = new Card();
        card3.setTitle("Test Card 3");
        card3.setState(CardState.GREEN);
        card3.setBoard(testBoard);
        card3.setResetTime(LocalTime.now(clock).minusMinutes(30)); // Set reset time to 30 minutes ago
        card3.setPosition(2);
        card3 = cardRepository.save(card3);

        // Create audit entries for 1 hour ago
        CardAudit audit1 = new CardAudit();
        audit1.setCard(testCard);
        audit1.setPreviousState(CardState.RED);
        audit1.setNewState(CardState.GREEN);
        audit1.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit1);

        CardAudit audit2 = new CardAudit();
        audit2.setCard(card2);
        audit2.setPreviousState(CardState.RED);
        audit2.setNewState(CardState.GREEN);
        audit2.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit2);

        CardAudit audit3 = new CardAudit();
        audit3.setCard(card3);
        audit3.setPreviousState(CardState.RED);
        audit3.setNewState(CardState.GREEN);
        audit3.setTimestamp(LocalDateTime.now(clock).minusHours(1));
        cardAuditRepository.save(audit3);

        // Set testCard's reset time to 30 minutes ago
        testCard.setResetTime(LocalTime.now(clock).minusMinutes(30));
        cardRepository.save(testCard);

        // Get all cards from the board
        List<Card> cards = cardService.getCardsByBoardId(testBoard.getId());

        // Verify all cards were reset and order is preserved
        assertTrue(cards.stream().allMatch(card -> card.getState() == CardState.RED));
        assertEquals(0, cards.get(0).getPosition());
        assertEquals(1, cards.get(1).getPosition());
        assertEquals(2, cards.get(2).getPosition());
    }
}
