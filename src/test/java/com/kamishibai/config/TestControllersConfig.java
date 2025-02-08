package com.kamishibai.config;

import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.BoardController;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestControllersConfig {

    @Bean
    @Primary
    public AccountController accountController(AccountService accountService) {
        return new AccountController(accountService);
    }

    @Bean
    @Primary
    public BoardController boardController(BoardService boardService, AccountService accountService) {
        return new BoardController(boardService, accountService);
    }
}
