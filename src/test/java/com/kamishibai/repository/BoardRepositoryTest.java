package com.kamishibai.repository;

import com.kamishibai.model.Account;
import com.kamishibai.model.Board;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BoardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BoardRepository boardRepository;

    @Test
    void findByOwnerOrSharedWith_ShouldReturnOwnedBoards() {
        // Arrange
        Account owner = new Account();
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setPasswordHash("hashedPassword");
        entityManager.persist(owner);

        Board board = new Board();
        board.setName("Test Board");
        board.setDescription("Test Description");
        board.setOwner(owner);
        entityManager.persist(board);
        entityManager.flush();

        // Act
        List<Board> boards = boardRepository.findByOwnerOrSharedWith(owner, owner);

        // Assert
        assertThat(boards).hasSize(1);
        assertThat(boards.get(0).getName()).isEqualTo("Test Board");
        assertThat(boards.get(0).getOwner()).isEqualTo(owner);
    }

    @Test
    void findByOwnerOrSharedWith_ShouldReturnSharedBoards() {
        // Arrange
        Account owner = new Account();
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setPasswordHash("hashedPassword");
        entityManager.persist(owner);

        Account sharedUser = new Account();
        sharedUser.setEmail("shared@example.com");
        sharedUser.setName("Shared User");
        sharedUser.setPasswordHash("hashedPassword");
        entityManager.persist(sharedUser);

        Board board = new Board();
        board.setName("Shared Board");
        board.setDescription("Shared Description");
        board.setOwner(owner);
        board.getSharedWith().add(sharedUser);
        entityManager.persist(board);
        entityManager.flush();

        // Act
        List<Board> boards = boardRepository.findByOwnerOrSharedWith(sharedUser, sharedUser);

        // Assert
        assertThat(boards).hasSize(1);
        assertThat(boards.get(0).getName()).isEqualTo("Shared Board");
        assertThat(boards.get(0).getSharedWith()).contains(sharedUser);
    }
}
