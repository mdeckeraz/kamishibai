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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardAuditRepository cardAuditRepository;

    public CardService(CardRepository cardRepository, CardAuditRepository cardAuditRepository) {
        this.cardRepository = cardRepository;
        this.cardAuditRepository = cardAuditRepository;
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
            card.setState(updatedCard.getState());
            cardAuditRepository.save(audit);
        }

        return cardRepository.save(card);
    }

    @Transactional
    public CardResponse toggleCardState(Card card) {
        CardState newState = (card.getState() == CardState.RED) ? CardState.GREEN : CardState.RED;
        
        CardAudit audit = new CardAudit();
        audit.setCard(card);
        audit.setPreviousState(card.getState());
        audit.setNewState(newState);
        card.setState(newState);
        
        cardAuditRepository.save(audit);
        card = cardRepository.save(card);

        return new CardResponse(card.getId(), card.getState());
    }

    @Transactional(readOnly = true)
    public List<Card> getCardsByBoardId(Long boardId) {
        return cardRepository.findByBoardIdOrderByPosition(boardId);
    }

    @Transactional(readOnly = true)
    public List<CardAudit> getCardAuditLog(Card card) {
        return cardAuditRepository.findByCardIdOrderByChangedAtDesc(card.getId());
    }

    // Scheduled task to reset cards to red state at their reset time
    @Scheduled(cron = "0 * * * * *") // Runs every minute
    @Transactional
    public void resetCards() {
        LocalTime now = LocalTime.now();
        List<Card> cards = cardRepository.findByStateAndResetTimeLessThanEqual(CardState.GREEN, now);
        
        for (Card card : cards) {
            CardAudit audit = new CardAudit();
            audit.setCard(card);
            audit.setPreviousState(CardState.GREEN);
            audit.setNewState(CardState.RED);
            card.setState(CardState.RED);
            
            cardAuditRepository.save(audit);
            cardRepository.save(card);
        }
    }
}
