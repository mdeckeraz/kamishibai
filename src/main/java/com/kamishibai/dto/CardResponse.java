package com.kamishibai.dto;

import com.kamishibai.model.Card;
import com.kamishibai.model.CardState;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class CardResponse {
    private Long id;
    private String title;
    private String details;
    private Integer position;
    private CardState state;
    private LocalTime resetTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CardResponse() {
    }

    public CardResponse(Long id, CardState state) {
        this.id = id;
        this.state = state;
    }

    public static CardResponse fromEntity(Card card) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setTitle(card.getTitle());
        response.setDetails(card.getDetails());
        response.setPosition(card.getPosition());
        response.setState(card.getState());
        response.setResetTime(card.getResetTime());
        response.setCreatedAt(card.getCreatedAt());
        response.setUpdatedAt(card.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public CardState getState() {
        return state;
    }

    public void setState(CardState state) {
        this.state = state;
    }

    public LocalTime getResetTime() {
        return resetTime;
    }

    public void setResetTime(LocalTime resetTime) {
        this.resetTime = resetTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
