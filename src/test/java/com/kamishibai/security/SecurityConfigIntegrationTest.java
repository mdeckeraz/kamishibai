package com.kamishibai.security;

import com.kamishibai.config.SecurityConfig;
import com.kamishibai.config.TestDatabaseConfig;
import com.kamishibai.controller.AccountController;
import com.kamishibai.controller.BoardController;
import com.kamishibai.controller.HomeController;
import com.kamishibai.service.AccountService;
import com.kamishibai.service.BoardService;
import com.kamishibai.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AccountController.class, BoardController.class, HomeController.class})
@Import({SecurityConfig.class, TestDatabaseConfig.class})
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void publicEndpoints_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        // Test public endpoints
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test protected endpoints
        mockMvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/boards/create")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void protectedEndpoints_ShouldBeAccessible_WithAuthentication() throws Exception {
        // Test protected endpoints with authentication
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/boards/create")).andExpect(status().isOk());
    }

    @Test
    void staticResources_ShouldBeAccessible_WithoutAuthentication() throws Exception {
        // Test static resource access
        mockMvc.perform(get("/css/styles.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/dashboard.js")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void homepage_ShouldRedirectToDashboard_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/dashboard"));
    }
}
