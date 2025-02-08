package com.kamishibai.dto;

import com.kamishibai.model.Board;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BoardResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private List<String> sharedWithEmails = new ArrayList<>();
    private List<CardResponse> cards = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BoardResponse() {}

    public BoardResponse(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BoardResponse fromEntity(Board board) {
        BoardResponse response = new BoardResponse(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
        response.setOwnerId(board.getOwner().getId());
        response.setOwnerName(board.getOwner().getName());
        response.setOwnerEmail(board.getOwner().getEmail());
        response.setSharedWithEmails(
            board.getSharedWith().stream()
                .map(account -> account.getEmail())
                .collect(Collectors.toList())
        );
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public List<String> getSharedWithEmails() {
        return sharedWithEmails;
    }

    public void setSharedWithEmails(List<String> sharedWithEmails) {
        this.sharedWithEmails = sharedWithEmails;
    }

    public List<CardResponse> getCards() {
        return cards;
    }

    public void setCards(List<CardResponse> cards) {
        this.cards = cards;
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
