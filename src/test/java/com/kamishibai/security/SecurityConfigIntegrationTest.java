package com.kamishibai.security;

import com.kamishibai.config.TestSecurityConfig;
import com.kamishibai.config.TestWebConfig;
import com.kamishibai.controller.HomeController;
import com.kamishibai.controller.RegisterController;
import com.kamishibai.controller.BoardViewController;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import com.kamishibai.service.CardService;
import com.kamishibai.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
    HomeController.class,
    RegisterController.class,
    BoardViewController.class
})
@Import({TestSecurityConfig.class, TestWebConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CardService cardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithAnonymousUser
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test protected endpoints
        mockMvc.perform(get("/dashboard"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/boards/form"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser
    void authenticatedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(model().hasNoErrors());
    }
}
