package com.kamishibai.repository;

import com.kamishibai.model.CardAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardAuditRepository extends JpaRepository<CardAudit, Long> {
    List<CardAudit> findByCardIdOrderByChangedAtDesc(Long cardId);
}
