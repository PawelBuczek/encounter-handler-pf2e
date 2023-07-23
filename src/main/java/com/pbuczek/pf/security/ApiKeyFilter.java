package com.pbuczek.pf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

public class ApiKeyFilter extends OncePerRequestFilter {
    //actually not a filter! It adds authentication.
    // I just have no idea how else to get the request in this strange setup

    private static UserDetailsService userDetailsService;

    public ApiKeyFilter(UserDetailsService userDetailsService) {
        ApiKeyFilter.userDetailsService = userDetailsService;
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

        apiKey = apiKey.trim();
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadByApiKey(apiKey);
        } catch (UsernameNotFoundException e) {
            response.sendError(401, e.getMessage());
            return;
        }

        if (userDetails == null || userDetails.getPassword() == null || userDetails.getAuthorities() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                apiKey,
                null,
                userDetails.getAuthorities()));

        filterChain.doFilter(request, response);
    }

}