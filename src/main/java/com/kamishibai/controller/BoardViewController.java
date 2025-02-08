package com.kamishibai.controller;

import com.kamishibai.dto.BoardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/boards")
public class BoardViewController {
    private final BoardService boardService;
    private final AccountService accountService;

    public BoardViewController(BoardService boardService, AccountService accountService) {
        this.boardService = boardService;
        this.accountService = accountService;
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

        model.addAttribute("board", board);
        return "boards/view";
    }

    @GetMapping("/create")
    public String createBoardForm(Model model) {
        model.addAttribute("board", new Board());
        return "boards/form";
    }

    @PostMapping
    public String createBoard(@ModelAttribute Board board, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
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

    @PostMapping("/{id}")
    public String updateBoard(@PathVariable Long id, @ModelAttribute Board updatedBoard, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Account account = accountService.getAccount(userDetails.getId());
        Board existingBoard = boardService.getBoardById(id, account)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        // Check if user is the owner
        if (!existingBoard.getOwner().equals(account)) {
            return "error/403";
        }

        BoardRequest request = new BoardRequest();
        request.setName(updatedBoard.getName());
        request.setDescription(updatedBoard.getDescription());
        boardService.updateBoard(id, request, account);
        return "redirect:/boards";
    }
}
