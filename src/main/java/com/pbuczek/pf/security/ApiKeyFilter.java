package com.pbuczek.pf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyFilter extends OncePerRequestFilter {
    //actually not a filter! I just have no idea how else to get the request in this strange setup

    private static SecurityConfig.UserDetailsService userDetailsService;

    public ApiKeyFilter(SecurityConfig.UserDetailsService userDetailsService) {
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
            filterChain.doFilter(request, response);
            return;
        }

        apiKey = apiKey.trim();
        UserDetails userDetails = userDetailsService.loadByApiKey(apiKey);

        if (userDetails == null || userDetails.getPassword() == null || userDetails.getAuthorities() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                apiKey,
                "",
                userDetails.getAuthorities()));

        filterChain.doFilter(request, response);
    }

}