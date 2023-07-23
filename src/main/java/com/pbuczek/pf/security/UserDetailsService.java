package com.pbuczek.pf.security;

import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .map(UserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));
    }

    public UserDetails loadUserByUserId(Integer userId) throws UsernameNotFoundException {
        return userRepo.findById(userId)
                .map(UserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));
    }

    public UserDetails loadByApiKey(String apiKey) {
        return userRepo.getUserIdByApiKey(apiKey)
                .map(this::loadUserByUserId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for provided API Key"));
    }

    @Data
    public static class UserDetails implements org.springframework.security.core.userdetails.UserDetails {

        private Integer userName;
        private String password;
        private List<GrantedAuthority> authorities;

        public UserDetails(User user) {
            userName = user.getId();
            password = user.getPassword();
            authorities = List.of(new SimpleGrantedAuthority(user.getType().toString()));
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return String.valueOf(userName);
        }

        @Override
        public boolean isAccountNonExpired() {
            return isCredentialsNonExpired();
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}