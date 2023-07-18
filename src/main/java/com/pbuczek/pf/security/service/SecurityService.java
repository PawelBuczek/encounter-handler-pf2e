package com.pbuczek.pf.security.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    @SuppressWarnings("unused")
    public boolean hasContextAnyAuthorities() {
        return !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .filter(grantedAuthority -> !grantedAuthority.getAuthority().contains("ANONYMOUS"))
                .toList().isEmpty();
    }

    @SuppressWarnings("unused")
    public boolean isContextAdminOrSpecificUsername(String username) {
        return isContextAdmin() || isContextSpecificUsername(username);
    }

    public boolean isContextAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ADMIN"));
    }

    public boolean isContextSpecificUsername(String username) {
        return SecurityContextHolder.getContext().getAuthentication().getName().equals(username);
    }

}
