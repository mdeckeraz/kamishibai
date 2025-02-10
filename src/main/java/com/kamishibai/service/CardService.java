package com.kamishibai.service;

import com.kamishibai.dto.CardResponse;
import com.kamishibai.model.Card;
import com.kamishibai.model.CardAudit;
import com.kamishibai.model.CardState;
import com.kamishibai.repository.CardAuditRepository;
import com.kamishibai.repository.CardRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardAuditRepository cardAuditRepository;
    private final Clock clock;

    public CardService(CardRepository cardRepository, CardAuditRepository cardAuditRepository, Clock clock) {
        this.cardRepository = cardRepository;
        this.cardAuditRepository = cardAuditRepository;
        this.clock = clock;
    }

    @Transactional
    public Card createCard(Card card) {
        card.setState(CardState.RED); // Default state
        return cardRepository.save(card);
    }

    @Transactional
    public Card updateCard(Long id, Card updatedCard) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        card.setTitle(updatedCard.getTitle());
        card.setDetails(updatedCard.getDetails());
        card.setResetTime(updatedCard.getResetTime());

        // If state is being changed, create an audit log entry
        if (updatedCard.getState() != null && updatedCard.getState() != card.getState()) {
            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(card.getState());
            audit.setNewState(updatedCard.getState());
            audit.setTimestamp(LocalDateTime.now(clock));
            card.setState(updatedCard.getState());
            cardAuditRepository.save(audit);
        }

        return cardRepository.save(card);
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
        List<Card> cards = cardRepository.findByBoardIdOrderByPosition(boardId);
        cards.forEach(this::checkAndResetCardState);
        return cards;
    }

    @Transactional
    public void checkAndResetCardState(Card card) {
        if (shouldResetCard(card)) {
            CardState previousState = card.getState();
            card.setState(CardState.RED);
            cardRepository.save(card);

            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(previousState);
            audit.setNewState(CardState.RED);
            audit.setTimestamp(LocalDateTime.now(clock));
            cardAuditRepository.save(audit);
        }
    }

    private boolean shouldResetCard(Card card) {
        // Only reset GREEN cards that have a reset time
        if (card.getState() != CardState.GREEN || card.getResetTime() == null) {
            return false;
        }

        // Get the last time this card was set to GREEN
        LocalDateTime lastStateChange = cardAuditRepository
                .findTopByCardAndNewStateOrderByTimestampDesc(card, CardState.GREEN)
                .map(CardAudit::getTimestamp)
                .orElse(null);

        // If we can't find when it was last set to GREEN, don't reset it
        if (lastStateChange == null) {
            return false;
        }

        LocalTime resetTime = card.getResetTime();
        LocalTime currentTime = LocalTime.now(clock);
        LocalDate currentDate = LocalDate.now(clock);

        // If the current time is before reset time, don't reset
        if (currentTime.isBefore(resetTime)) {
            return false;
        }

        // If the last state change was today and after reset time, don't reset
        if (lastStateChange.toLocalDate().equals(currentDate) &&
            lastStateChange.toLocalTime().isAfter(resetTime)) {
            return false;
        }

        // If we get here, it means:
        // 1. Current time is after reset time
        // 2. Either:
        //    a. Last state change was yesterday or earlier, or
        //    b. Last state change was today but before reset time
        return true;
    }

    @Transactional
    public CardResponse toggleCardState(Card card) {
        CardState previousState = card.getState();
        CardState newState = (previousState == CardState.RED) ? CardState.GREEN : CardState.RED;
        
        card.setState(newState);
        card = cardRepository.save(card);
        
        CardAudit audit = new CardAudit();
        audit.setCard(card);
        audit.setPreviousState(previousState);
        audit.setNewState(newState);
        cardAuditRepository.save(audit);

        return new CardResponse(card.getId(), card.getState());
    }

    @Transactional(readOnly = true)
    public List<CardAudit> getCardAuditLog(Card card) {
        return cardAuditRepository.findByCardOrderByTimestampDesc(card);
    }

    // Scheduled task to reset cards to red state at their reset time
    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void resetCards() {
        LocalTime now = LocalTime.now(clock);
        List<Card> cards = cardRepository.findByStateAndResetTimeLessThanEqual(CardState.GREEN, now);
        
        for (Card card : cards) {
            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(CardState.GREEN);
            audit.setNewState(CardState.RED);
            audit.setTimestamp(LocalDateTime.now(clock));
            card.setState(CardState.RED);
            
            cardAuditRepository.save(audit);
            cardRepository.save(card);
        }
    }
}
