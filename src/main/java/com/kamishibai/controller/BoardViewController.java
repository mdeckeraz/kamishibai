package com.kamishibai.controller;

import com.kamishibai.dto.BoardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.model.Card;
import com.kamishibai.model.CardState;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import com.kamishibai.service.CardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/boards")
public class BoardViewController {
    private final BoardService boardService;
    private final AccountService accountService;
    private final CardService cardService;

    public BoardViewController(BoardService boardService, AccountService accountService, CardService cardService) {
        this.boardService = boardService;
        this.accountService = accountService;
        this.cardService = cardService;
    }

    @GetMapping
    public String listBoards(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        model.addAttribute("boards", boardService.getBoardsForUser(account));
        return "boards/list";
    }

    @GetMapping("/{id}")
    public String viewBoard(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        Board board = boardService.getBoardById(id, account)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        // Get all cards for this board
        List<Card> cards = cardService.getCardsByBoardId(id);
        
        // Split cards into red and green lists
        List<Card> redCards = cards.stream()
                .filter(card -> card.getState() == CardState.RED)
                .collect(Collectors.toList());
        
        List<Card> greenCards = cards.stream()
                .filter(card -> card.getState() == CardState.GREEN)
                .collect(Collectors.toList());

        model.addAttribute("board", board);
        model.addAttribute("redCards", redCards);
        model.addAttribute("greenCards", greenCards);
        
        return "boards/view";
    }

    @GetMapping("/create")
    public String createBoardForm(Model model) {
        model.addAttribute("board", new Board());
        return "boards/form";
    }

    @PostMapping("/create")
    public String createBoard(@ModelAttribute BoardRequest boardRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        Board board = new Board();
        board.setName(boardRequest.getName());
        board.setDescription(boardRequest.getDescription());
        boardService.createBoard(board, account);
        return "redirect:/boards";
    }

    @GetMapping("/{id}/edit")
    public String editBoardForm(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        Board board = boardService.getBoardById(id, account)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        // Check if user is the owner
        if (!board.getOwner().equals(account)) {
            return "error/403";
        }

        model.addAttribute("board", board);
        return "boards/form";
    }

    @PostMapping("/{id}/update")
    public String updateBoard(@PathVariable Long id, @ModelAttribute BoardRequest boardRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        boardService.updateBoard(id, boardRequest, account);
        return "redirect:/boards";
    }

    @GetMapping("/{boardId}/cards/create")
    public String createCardForm(@PathVariable Long boardId, Model model) {
        model.addAttribute("boardId", boardId);
        model.addAttribute("card", new Card()); // Add empty card for form binding
        return "cards/form";
    }
}
