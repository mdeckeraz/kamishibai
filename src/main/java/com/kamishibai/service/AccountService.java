package com.kamishibai.service;

import com.kamishibai.dto.AccountRequest;
import com.kamishibai.model.Account;
import com.kamishibai.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account createAccount(AccountRequest accountRequest) {
        if (accountRepository.findByEmail(accountRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        Account account = new Account();
        account.setEmail(accountRequest.getEmail());
        account.setName(accountRequest.getName());
        account.setPasswordHash(passwordEncoder.encode(accountRequest.getPassword()));

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Account> getAccountByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    @Transactional
    public Account updateAccount(Long id, AccountRequest request) {
        Account account = getAccount(id);

        // Check if new email is already taken by another account
        accountRepository.findByEmail(request.getEmail())
            .filter(existingAccount -> !existingAccount.getId().equals(id))
            .ifPresent(existingAccount -> {
                throw new IllegalArgumentException("Email already taken");
            });

        account.setEmail(request.getEmail());
        account.setName(request.getName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = getAccount(id);
        accountRepository.delete(account);
    }
}
