package com.kamishibai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_audit_log")
public class CardAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardState previousState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardState newState;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public CardState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(CardState previousState) {
        this.previousState = previousState;
    }

    public CardState getNewState() {
        return newState;
    }

    public void setNewState(CardState newState) {
        this.newState = newState;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
