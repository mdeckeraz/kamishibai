package com.kamishibai.dto;

import com.kamishibai.model.Card;
import com.kamishibai.model.CardState;

import java.time.LocalTime;

public class CardListResponse {
    private Long id;
    private String title;
    private String details;
    private CardState state;
    private LocalTime resetTime;
    private Integer position;

    public CardListResponse(Card card) {
        this.id = card.getId();
        this.title = card.getTitle();
        this.details = card.getDetails();
        this.state = card.getState();
        this.resetTime = card.getResetTime();
        this.position = card.getPosition();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public CardState getState() {
        return state;
    }

    public LocalTime getResetTime() {
        return resetTime;
    }

    public Integer getPosition() {
        return position;
    }
}
