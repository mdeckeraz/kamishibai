package com.kamishibai.controller;

import com.kamishibai.dto.BoardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.security.CustomUserDetails;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
public class BoardController {
    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    private final BoardService boardService;
    private final AccountService accountService;

    public BoardController(BoardService boardService, AccountService accountService) {
        this.boardService = boardService;
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<Board>> getBoards(@AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("Getting boards for user ID: {}", userDetails.getId());
        Account account = accountService.getAccount(userDetails.getId());
        logger.info("Found account: {}", account);
        List<Board> boards = boardService.getBoardsForUser(account);
        logger.info("Found {} boards", boards.size());
        return ResponseEntity.ok(boards);
    }

    @PostMapping
    public ResponseEntity<?> createBoard(@Valid @RequestBody BoardRequest request,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }

        logger.info("Creating board for user ID: {}", userDetails.getId());
        Account account = accountService.getAccount(userDetails.getId());
        logger.info("Found account: {}", account);
        
        Board board = new Board();
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        
        Board createdBoard = boardService.createBoard(board, account);
        logger.info("Created board: {}", createdBoard);
        return ResponseEntity.ok(createdBoard);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBoard(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("Getting board with ID: {}", id);
        Account account = accountService.getAccount(userDetails.getId());
        Optional<Board> board = boardService.getBoardById(id, account);
        return board.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id,
                                       @Valid @RequestBody BoardRequest request,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }

        logger.info("Updating board with ID: {}", id);
        try {
            Account account = accountService.getAccount(userDetails.getId());
            Board updatedBoard = boardService.updateBoard(id, request, account);
            logger.info("Updated board: {}", updatedBoard);
            return ResponseEntity.ok(updatedBoard);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("Deleting board with ID: {}", id);
        try {
            Account account = accountService.getAccount(userDetails.getId());
            boardService.deleteBoard(id, account);
            logger.info("Deleted board with ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareBoard(@PathVariable Long id,
                                      @RequestParam String email,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("Sharing board with ID: {} with user: {}", id, email);
        try {
            Account owner = accountService.getAccount(userDetails.getId());
            Optional<Account> sharedWithAccount = accountService.getAccountByEmail(email);
            if (sharedWithAccount.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            Board sharedBoard = boardService.shareBoard(id, owner, sharedWithAccount.get());
            logger.info("Shared board: {}", sharedBoard);
            return ResponseEntity.ok(sharedBoard);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }
}
