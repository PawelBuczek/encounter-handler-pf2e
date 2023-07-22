package com.pbuczek.pf.security;

import com.pbuczek.pf.user.UserType;
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
        System.out.println(SecurityContextHolder.getContext().getAuthentication());

        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(grantedAuthority -> userTypes.contains(grantedAuthority.toString()));
    }

    public boolean isContextAdminOrSpecificUserId(Integer userId) {
        return isContextAdmin() || isContextSpecificUserId(userId);
    }

    private boolean isContextAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("ADMIN"));
    }

    private boolean isContextSpecificUserId(Integer userId) {
        if (userId == null) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getName().equals(String.valueOf(userId));
    }

}
