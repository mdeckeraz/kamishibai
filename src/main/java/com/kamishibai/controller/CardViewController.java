package com.kamishibai.controller;

import com.kamishibai.dto.CardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.model.Card;
import com.kamishibai.model.CardState;
import com.kamishibai.repository.BoardRepository;
import com.kamishibai.repository.CardRepository;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/boards/{boardId}/cards")
public class CardViewController {
    private final CardService cardService;
    private final CardRepository cardRepository;
    private final BoardRepository boardRepository;

    @Autowired
    public CardViewController(CardService cardService, CardRepository cardRepository, BoardRepository boardRepository) {
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
        System.out.println("DEBUG: Received request to list cards for board " + boardId);
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            System.out.println("DEBUG: Found board: " + board.getId());
            model.addAttribute("cards", cardService.getCardsByBoardId(boardId));
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "cards/list";
        } catch (IllegalStateException e) {
            System.out.println("DEBUG: Access denied for board " + boardId);
            return "error/403";
        } catch (Exception e) {
            System.out.println("DEBUG: Error listing cards: " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/create")
    public String createCardForm(@PathVariable Long boardId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = new Card();
            card.setBoard(board);
            card.setResetTime(LocalTime.MIDNIGHT);
            model.addAttribute("card", card);
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", board);
            return "cards/form";
        } catch (IllegalStateException e) {
            return "error/403";
        }
    }

    @PostMapping("/create")
    public String createCard(
            @PathVariable Long boardId,
            @Valid @ModelAttribute("card") Card card,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            
            if (bindingResult.hasErrors()) {
                model.addAttribute("boardId", boardId);
                model.addAttribute("board", board);
                return "cards/form";
            }
            
            // Set the board and default values
            card.setBoard(board);
            if (card.getState() == null) {
                card.setState(CardState.RED);
            }
            if (card.getResetTime() == null) {
                card.setResetTime(LocalTime.MIDNIGHT);
            }
            if (card.getPosition() == null) {
                card.setPosition(0);
            }
            
            cardService.createCard(card);
            redirectAttributes.addFlashAttribute("message", "Card created successfully!");
            
            return "redirect:/boards/" + boardId;
        } catch (IllegalStateException e) {
            return "error/403";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create card: " + e.getMessage());
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", boardRepository.findById(boardId).orElse(null));
            return "cards/form";
        }
    }

    @PostMapping("/create-json")
    public String createCardJson(
            @PathVariable Long boardId,
            @Valid @ModelAttribute("card") CardRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            
            if (bindingResult.hasErrors()) {
                model.addAttribute("boardId", boardId);
                model.addAttribute("board", board);
                return "cards/form";
            }
            
            Card card = new Card();
            card.setTitle(request.getTitle());
            card.setDetails(request.getDetails());
            card.setState(CardState.RED); // Always start with RED
            card.setResetTime(request.getResetTime() != null ? request.getResetTime() : LocalTime.MIDNIGHT);
            card.setBoard(board);
            card.setPosition(request.getPosition() != null ? request.getPosition() : 0);
            
            cardService.createCard(card);
            redirectAttributes.addFlashAttribute("message", "Card created successfully!");
            
            return "redirect:/boards/" + boardId;
        } catch (IllegalStateException e) {
            return "error/403";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create card: " + e.getMessage());
            return "cards/form";
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

    @PostMapping("/{cardId}/edit")
    public String updateCard(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @Valid @ModelAttribute("card") Card updatedCard,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
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
            
            // Update card fields
            existingCard.setTitle(updatedCard.getTitle());
            existingCard.setDetails(updatedCard.getDetails());
            existingCard.setState(updatedCard.getState());
            existingCard.setResetTime(updatedCard.getResetTime());
            
            cardService.updateCard(cardId, existingCard);
            redirectAttributes.addFlashAttribute("message", "Card updated successfully!");
            
            return "redirect:/boards/" + boardId;
        } catch (IllegalStateException e) {
            return "error/403";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update card: " + e.getMessage());
            model.addAttribute("boardId", boardId);
            model.addAttribute("board", boardRepository.findById(boardId).orElse(null));
            return "cards/form";
        }
    }

    @PostMapping("/{cardId}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleCardState(
            @PathVariable Long boardId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Board board = getBoardAndCheckAccess(boardId, userDetails.getAccount());
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            
            if (!card.getBoard().getId().equals(boardId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CardState newState = (card.getState() == CardState.RED) ? CardState.GREEN : CardState.RED;
            card.setState(newState);
            cardService.updateCard(cardId, card);
            
            Map<String, Object> response = new HashMap<>();
            response.put("state", newState);
            response.put("cardId", cardId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
