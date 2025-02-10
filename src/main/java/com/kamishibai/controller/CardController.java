package com.kamishibai.controller;

import com.kamishibai.dto.CardRequest;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.dto.CardListResponse;
import com.kamishibai.model.*;
import com.kamishibai.repository.BoardRepository;
import com.kamishibai.repository.CardRepository;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards/{boardId}/cards")
public class CardController {
    private static final Logger logger = LoggerFactory.getLogger(CardController.class);
    
    private final CardService cardService;
    private final CardRepository cardRepository;
    private final BoardRepository boardRepository;

    public CardController(CardService cardService, CardRepository cardRepository, BoardRepository boardRepository) {
        this.cardService = cardService;
        this.cardRepository = cardRepository;
        this.boardRepository = boardRepository;
    }

    private boolean hasAccess(Board board, Account account) {
        return board.getOwner().equals(account) || board.getSharedWith().contains(account);
    }

    private Board getBoardAndCheckAccess(Long boardId, Account account) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        if (!hasAccess(board, account)) {
            throw new IllegalStateException("Access denied");
        }

        return board;
    }

    @PostMapping("/{cardId}/toggle")
    public ResponseEntity<?> toggleCardState(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!card.getBoard().getId().equals(boardId)) {
                throw new IllegalStateException("Card does not belong to this board");
            }

            CardResponse response = cardService.toggleCardState(card);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to toggle card state");
        }
    }

    @GetMapping
    public ResponseEntity<List<CardListResponse>> getCards(@PathVariable Long boardId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            List<Card> cards = cardService.getCardsByBoardId(boardId);
            List<CardListResponse> response = cards.stream()
                .map(CardListResponse::new)
                .toList();
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCard(
            @PathVariable Long boardId,
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            logger.info("Creating card for board {} with request: {}", boardId, request);
            
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            
            Card card = new Card();
            card.setTitle(request.getTitle());
            card.setDetails(request.getDetails());
            card.setState(CardState.RED); // Always start with RED
            card.setResetTime(request.getResetTime() != null ? request.getResetTime() : LocalTime.MIDNIGHT);
            card.setBoard(board);
            card.setPosition(request.getPosition() != null ? request.getPosition() : 0);
            
            Card createdCard = cardService.createCard(card);
            logger.info("Successfully created card {} for board {}", createdCard.getId(), boardId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdCard.getId());
            response.put("message", "Card created successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Access denied while creating card for board {}: {}", boardId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request while creating card for board {}: {}", boardId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Error creating card for board {}: {}", boardId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to create card: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<Map<String, Object>> updateCard(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card existingCard = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!existingCard.getBoard().getId().equals(boardId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            existingCard.setTitle(request.getTitle());
            existingCard.setDetails(request.getDetails());
            existingCard.setState(request.getState());
            existingCard.setResetTime(request.getResetTime());
            existingCard.setPosition(request.getPosition());

            Card updatedCard = cardService.updateCard(cardId, existingCard);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedCard.getId());
            response.put("message", "Card updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to update card: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{cardId}/audit")
    public ResponseEntity<List<CardAudit>> getCardAuditLog(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            logger.info("Getting audit log for card {} in board {}", cardId, boardId);
            
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!card.getBoard().getId().equals(boardId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<CardAudit> auditLog = cardService.getCardAuditLog(card);
            logger.info("Successfully retrieved audit log for card {} in board {}", cardId, boardId);
            return ResponseEntity.ok(auditLog);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error getting audit log for card {} in board {}: {}", cardId, boardId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
