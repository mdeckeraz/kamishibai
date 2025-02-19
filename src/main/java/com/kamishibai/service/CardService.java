package com.kamishibai.service;

import com.kamishibai.dto.CardResponse;
import com.kamishibai.model.Card;
import com.kamishibai.model.CardAudit;
import com.kamishibai.model.CardState;
import com.kamishibai.repository.CardAuditRepository;
import com.kamishibai.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CardService {
    private final Logger logger = LoggerFactory.getLogger(CardService.class);
    private final CardRepository cardRepository;
    private final CardAuditRepository cardAuditRepository;
    private final Clock clock;

    public CardService(CardRepository cardRepository, CardAuditRepository cardAuditRepository, Clock clock) {
        this.cardRepository = cardRepository;
        this.cardAuditRepository = cardAuditRepository;
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Transactional
    public Card createCard(Card card) {
        card.setState(CardState.RED); // Default state
        return cardRepository.save(card);
    }

    @Transactional
    public Card updateCard(Long id, Card updatedCard) {
        logger.debug("Updating card {} with new values: {}", id, updatedCard);
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        // Store the current state
        CardState currentState = card.getState();
        logger.debug("Current card state before update: {}", currentState);

        card.setTitle(updatedCard.getTitle());
        card.setDetails(updatedCard.getDetails());
        card.setResetTime(updatedCard.getResetTime());

        // If state is explicitly being changed, create an audit log entry
        if (updatedCard.getState() != null && updatedCard.getState() != currentState) {
            logger.info("Card {} state changing from {} to {}", id, currentState, updatedCard.getState());
            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(currentState);
            audit.setNewState(updatedCard.getState());
            audit.setTimestamp(now());
            card.setState(updatedCard.getState());
            cardAuditRepository.save(audit);
        } else {
            // Preserve the current state if not explicitly changed
            card.setState(currentState);
            logger.debug("Preserving card {} state as {}", id, currentState);
        }

        card = cardRepository.save(card);
        logger.debug("Card {} updated successfully, final state: {}", id, card.getState());
        return card;
    }

    @Transactional(readOnly = true)
    public Card getCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        checkAndResetCardState(card);
        return card;
    }

    @Transactional(readOnly = true)
    public List<Card> getAllCards() {
        List<Card> cards = cardRepository.findAll();
        cards.forEach(this::checkAndResetCardState);
        return cards;
    }

    @Transactional(readOnly = true)
    public List<Card> getCardsByBoardId(Long boardId) {
        logger.debug("Getting cards for board {}", boardId);
        List<Card> cards = cardRepository.findByBoardIdOrderByPosition(boardId);
        // Don't check for resets here, let the scheduled task handle it
        return cards;
    }

    @Transactional
    public void checkAndResetCardState(Card card) {
        if (shouldResetCard(card)) {
            logger.info("Resetting card {} from GREEN to RED", card.getId());
            CardState previousState = card.getState();
            card.setState(CardState.RED);
            cardRepository.save(card);

            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(previousState);
            audit.setNewState(CardState.RED);
            audit.setTimestamp(now());
            cardAuditRepository.save(audit);
        }
    }

    private boolean shouldResetCard(Card card) {
        if (card.getState() != CardState.GREEN || card.getResetTime() == null) {
            logger.debug("Card {} not eligible for reset: state={}, resetTime={}", 
                card.getId(), card.getState(), card.getResetTime());
            return false;
        }

        LocalDateTime lastStateChange = cardAuditRepository.findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN)
                .map(CardAudit::getTimestamp)
                .orElse(null);

        if (lastStateChange == null) {
            logger.debug("Card {} has no last state change to GREEN", card.getId());
            return false;
        }

        LocalTime resetTime = card.getResetTime();
        LocalTime currentTime = now().toLocalTime();

        logger.debug("Card {} checking reset: currentTime={}, resetTime={}, lastStateChange={}", 
            card.getId(), currentTime, resetTime, lastStateChange);

        // If the current time is before the reset time, don't reset
        if (currentTime.isBefore(resetTime)) {
            logger.debug("Card {} not resetting: current time {} is before reset time {}", 
                card.getId(), currentTime, resetTime);
            return false;
        }

        // If the last state change was today and after the reset time, don't reset
        if (lastStateChange.toLocalDate().equals(now().toLocalDate()) &&
            lastStateChange.toLocalTime().isAfter(resetTime)) {
            logger.debug("Card {} not resetting: last state change {} was today after reset time {}", 
                card.getId(), lastStateChange, resetTime);
            return false;
        }

        logger.info("Card {} will be reset: currentTime={}, resetTime={}, lastStateChange={}", 
            card.getId(), currentTime, resetTime, lastStateChange);
        return true;
    }

    @Transactional
    public CardResponse toggleCardState(Card card) {
        CardState newState = (card.getState() == CardState.RED) ? CardState.GREEN : CardState.RED;
        logger.info("Toggling card {} state from {} to {}", card.getId(), card.getState(), newState);
        
        CardAudit audit = new CardAudit();
        audit.setCard(card);
        audit.setPreviousState(card.getState());
        audit.setNewState(newState);
        audit.setTimestamp(now());
        card.setState(newState);
        
        cardAuditRepository.save(audit);
        card = cardRepository.save(card);
        logger.debug("Card {} state updated successfully", card.getId());

        return new CardResponse(card.getId(), card.getState());
    }

    @Transactional(readOnly = true)
    public List<CardAudit> getCardAuditLog(Card card) {
        return cardAuditRepository.findByCardOrderByTimestampDesc(card);
    }

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void resetCards() {
        logger.debug("Running scheduled card reset check");
        LocalTime now = now().toLocalTime();
        List<Card> cards = cardRepository.findByStateAndResetTimeLessThanEqual(CardState.GREEN, now);
        
        for (Card card : cards) {
            checkAndResetCardState(card);
        }
    }
}
