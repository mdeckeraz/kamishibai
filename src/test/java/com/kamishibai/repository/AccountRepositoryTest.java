package com.kamishibai.repository;

import com.kamishibai.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByEmail_ShouldReturnAccount_WhenEmailExists() {
        // Arrange
        Account account = new Account();
        account.setEmail("test@example.com");
        account.setName("Test User");
        account.setPasswordHash("hashedPassword");
        entityManager.persist(account);
        entityManager.flush();

        // Act
        Optional<Account> found = accountRepository.findByEmail("test@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // Act
        Optional<Account> found = accountRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }
}
