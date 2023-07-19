package com.pbuczek.pf.security.service;

import com.pbuczek.pf.security.UserType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class SecurityService {

    private static final List<String> userTypes = Stream.of(UserType.values()).map(Enum::name).toList();

    @SuppressWarnings("unused")
    public boolean hasContextAnyAuthorities() {
        return !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .filter(grantedAuthority -> userTypes.contains(grantedAuthority.toString()))
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
