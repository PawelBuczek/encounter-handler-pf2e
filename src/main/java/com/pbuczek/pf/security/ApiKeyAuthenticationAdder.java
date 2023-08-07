package com.pbuczek.pf.security;

import com.pbuczek.pf.apikey.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
public class ApiKeyAuthenticationAdder extends OncePerRequestFilter {

    private static UserDetailsService userDetailsService;

    public ApiKeyAuthenticationAdder(UserDetailsService userDetailsService) {
        ApiKeyAuthenticationAdder.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            try {
                filterChain.doFilter(request, response);
            } catch (ResponseStatusException e) {
                if (e.getMessage().startsWith("401 UNAUTHORIZED ")) {
                    response.sendError(401, e.getMessage().substring(17).replaceAll("\"", ""));
                } else {
                    throw e;
                }
            }
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByApiKey(apiKey);
        } catch (UsernameNotFoundException e) {
            response.sendError(401, e.getMessage());
            return;
        } catch (AuthenticationServiceException e) {
            response.sendError(498, e.getMessage());
            return;
        }

        if (userDetails == null || userDetails.getPassword() == null || userDetails.getAuthorities() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Adding authentication for apiKey with identifier:" + apiKey.substring(0, ApiKey.IDENTIFIER_LENGTH));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                apiKey,
                "apiKey",
                userDetails.getAuthorities()));

        filterChain.doFilter(request, response);
    }

}