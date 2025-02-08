package com.kamishibai.controller;

import com.kamishibai.dto.CardRequest;
import com.kamishibai.dto.CardResponse;
import com.kamishibai.model.*;
import com.kamishibai.repository.BoardRepository;
import com.kamishibai.repository.CardRepository;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/boards/{boardId}/cards")
public class CardController {
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

    @GetMapping
    public String listCards(@PathVariable Long boardId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            List<Card> cards = cardService.getCardsByBoardId(boardId);
            model.addAttribute("cards", cards);
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "cards/list";
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    @GetMapping("/create")
    public String createCardForm(@PathVariable Long boardId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            model.addAttribute("card", new Card());
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "cards/form";
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    @GetMapping("/{cardId}/edit")
    public String editCardForm(@PathVariable Long boardId, @PathVariable Long cardId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            
            if (!card.getBoard().getId().equals(boardId)) {
                return "error/403";
            }

            model.addAttribute("card", card);
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "cards/form";
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    @PostMapping
    public String createCard(
            @PathVariable Long boardId,
            @Valid @ModelAttribute("card") Card card,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());

            if (bindingResult.hasErrors()) {
                model.addAttribute("boardId", boardId);
                model.addAttribute("board", board);
                return "cards/form";
            }

            card.setBoard(board);
            cardService.createCard(card);

            return "redirect:/boards/" + boardId;
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    @PutMapping("/{cardId}")
    public String updateCard(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @Valid @ModelAttribute("card") Card updatedCard,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card existingCard = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!existingCard.getBoard().getId().equals(boardId)) {
                return "error/403";
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("boardId", boardId);
                model.addAttribute("board", board);
                return "cards/form";
            }

            existingCard.setTitle(updatedCard.getTitle());
            existingCard.setDetails(updatedCard.getDetails());
            existingCard.setState(updatedCard.getState());
            existingCard.setResetTime(updatedCard.getResetTime());
            cardRepository.save(existingCard);

            return "redirect:/boards/" + boardId;
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    // REST endpoints for AJAX calls
    @PostMapping("/{cardId}/toggle")
    @ResponseBody
    public ResponseEntity<CardResponse> toggleCardState(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!card.getBoard().getId().equals(boardId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            CardResponse response = cardService.toggleCardState(card);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{cardId}/audit")
    @ResponseBody
    public ResponseEntity<List<CardAudit>> getCardAuditLog(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));

            if (!card.getBoard().getId().equals(boardId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<CardAudit> auditLog = cardService.getCardAuditLog(card);
            return ResponseEntity.ok(auditLog);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
