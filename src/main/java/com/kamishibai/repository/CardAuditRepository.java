package com.kamishibai.repository;

import com.kamishibai.model.Card;
import com.kamishibai.model.CardAudit;
import com.kamishibai.model.CardState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardAuditRepository extends JpaRepository<CardAudit, Long> {
    Optional<CardAudit> findTopByCardAndNewStateOrderByTimestampDesc(Card card, CardState newState);
    List<CardAudit> findByCardOrderByTimestampDesc(Card card);
    List<CardAudit> findByCardInOrderByTimestampDesc(List<Card> cards);
}
