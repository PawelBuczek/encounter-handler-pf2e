package com.pbuczek.pf.security.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    public boolean isContextAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ADMIN"));
    }

    public boolean doesContextUsernameMatch(String username) {
        return SecurityContextHolder.getContext().getAuthentication().getName().equals(username);
    }

    @SuppressWarnings("unused")  //it is actually being used in controllers, through annotations
    public boolean isContextAdminOrSpecificUsername(String username) {
        return isContextAdmin() || doesContextUsernameMatch(username);
    }

}
