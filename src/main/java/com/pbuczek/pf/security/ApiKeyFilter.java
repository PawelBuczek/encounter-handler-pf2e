package com.pbuczek.pf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ApiKeyFilter extends GenericFilterBean {
    //actually not a filter! I just have no idea how else to get the request in this strange setup

    static UserDetailsService userDetailsService;

    public ApiKeyFilter(UserDetailsService userDetailsService) {
        ApiKeyFilter.userDetailsService = userDetailsService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            authenticateWithApiKey((HttpServletRequest) request);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }

        filterChain.doFilter(request, response);
    }


    public static void authenticateWithApiKey(HttpServletRequest request) {
        System.out.println("I was here 0");
        String apiKey = request.getHeader("X-API-KEY");
        System.out.println("first auth = " + SecurityContextHolder.getContext().getAuthentication());

        if (apiKey.equals("Pathfinder")) {
            Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("current auth = " + currentAuthentication);
            if (currentAuthentication == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername("johndoe");
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        apiKey,
                        userDetails.getPassword(),
                        List.of(new SimpleGrantedAuthority("STANDARD"))));
            }
            System.out.println("new auth= " + SecurityContextHolder.getContext().getAuthentication());
        }
    }
}