package com.kamishibai.service;

import com.kamishibai.dto.BoardRequest;
import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import com.kamishibai.repository.BoardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BoardService {
    private static final Logger logger = LoggerFactory.getLogger(BoardService.class);
    
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @Transactional
    public Board createBoard(Board board, Account owner) {
        logger.info("Creating board with name: {} for owner: {}", board.getName(), owner);
        board.setOwner(owner);
        Board savedBoard = boardRepository.save(board);
        logger.info("Created board with ID: {}", savedBoard.getId());
        return savedBoard;
    }

    @Transactional(readOnly = true)
    public List<Board> getBoardsForUser(Account account) {
        logger.info("Getting boards for account ID: {}", account.getId());
        List<Board> boards = boardRepository.findByOwnerOrSharedWith(account, account);
        logger.info("Found {} boards", boards.size());
        return boards;
    }

    @Transactional(readOnly = true)
    public Optional<Board> getBoardById(Long id, Account account) {
        logger.info("Getting board with ID: {} for account ID: {}", id, account.getId());
        Optional<Board> board = boardRepository.findById(id)
            .filter(b -> hasAccess(b, account));
        logger.info("Found board: {}", board.isPresent());
        return board;
    }

    @Transactional
    public Board updateBoard(Long id, BoardRequest request, Account account) {
        logger.info("Updating board with ID: {} for account ID: {}", id, account.getId());
        Board board = getBoardById(id, account)
            .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        if (!board.getOwner().equals(account)) {
            logger.warn("Account {} attempted to update board {} owned by {}", 
                account.getId(), id, board.getOwner().getId());
            throw new IllegalArgumentException("Only the owner can update the board");
        }

        board.setName(request.getName());
        board.setDescription(request.getDescription());

        Board updatedBoard = boardRepository.save(board);
        logger.info("Updated board: {}", updatedBoard);
        return updatedBoard;
    }

    @Transactional
    public void deleteBoard(Long id, Account account) {
        logger.info("Deleting board with ID: {} for account ID: {}", id, account.getId());
        Board board = getBoardById(id, account)
            .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        if (!board.getOwner().equals(account)) {
            logger.warn("Account {} attempted to delete board {} owned by {}", 
                account.getId(), id, board.getOwner().getId());
            throw new IllegalArgumentException("Only the owner can delete the board");
        }

        boardRepository.delete(board);
        logger.info("Deleted board with ID: {}", id);
    }

    @Transactional
    public Board shareBoard(Long id, Account owner, Account shareWith) {
        logger.info("Sharing board with ID: {} from owner ID: {} to account ID: {}", 
            id, owner.getId(), shareWith.getId());
        Board board = getBoardById(id, owner)
            .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        if (!board.getOwner().equals(owner)) {
            logger.warn("Account {} attempted to share board {} owned by {}", 
                owner.getId(), id, board.getOwner().getId());
            throw new IllegalArgumentException("Only the owner can share the board");
        }

        board.getSharedWith().add(shareWith);
        Board sharedBoard = boardRepository.save(board);
        logger.info("Shared board: {}", sharedBoard);
        return sharedBoard;
    }

    private boolean hasAccess(Board board, Account account) {
        return board.getOwner().equals(account) || board.getSharedWith().contains(account);
    }
}
