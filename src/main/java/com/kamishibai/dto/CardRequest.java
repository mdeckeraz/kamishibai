package com.kamishibai.dto;

import com.kamishibai.model.CardState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public class CardRequest {
    @NotBlank(message = "Card title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Details cannot exceed 5000 characters")
    private String details;

    @NotNull(message = "Position is required")
    private Integer position;

    @NotNull(message = "Reset time is required")
    private LocalTime resetTime;

    private CardState state;

    // Getters and Setters
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

    public LocalTime getResetTime() {
        return resetTime;
    }

    public void setResetTime(LocalTime resetTime) {
        this.resetTime = resetTime;
    }

    public CardState getState() {
        return state;
    }

    public void setState(CardState state) {
        this.state = state;
    }
}
