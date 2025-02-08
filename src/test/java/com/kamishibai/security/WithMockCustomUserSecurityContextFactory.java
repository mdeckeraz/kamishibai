package com.kamishibai.security;

import com.kamishibai.model.Account;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Account account = new Account();
        account.setId(annotation.id());
        account.setEmail(annotation.email());
        account.setName(annotation.name());
        account.setPasswordHash("hashedPassword");

        CustomUserDetails principal = new CustomUserDetails(account);

        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            "password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        context.setAuthentication(auth);
        return context;
    }
}
