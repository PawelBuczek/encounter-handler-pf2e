package com.pbuczek.pf.security;

import com.pbuczek.pf.apikey.ApiKey;
import com.pbuczek.pf.apikey.ApiKeyRepository;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Component
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    UserRepository userRepo;
    ApiKeyRepository apiKeyRepo;

    @Autowired
    public UserDetailsService(UserRepository userRepo, ApiKeyRepository apiKeyRepo) {
        this.userRepo = userRepo;
        this.apiKeyRepo = apiKeyRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .map(UserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));
    }

    public UserDetails loadUserByApiKey(String apiKey) {
        LocalDate validTillDate = apiKeyRepo.getValidTillDateByIdentifier(
                apiKey.trim().substring(0, ApiKey.IDENTIFIER_LENGTH)).orElseThrow(() ->
                new AuthenticationServiceException("API Key with provided value not found"));

        if (validTillDate.isBefore(LocalDate.now())) {
            throw new AuthenticationServiceException("API Key has expired. Please generate new one.");
        }

        return apiKeyRepo.getUserIdByApiKeyIdentifier(apiKey.trim().substring(0, ApiKey.IDENTIFIER_LENGTH))
                .map(this::loadUserByUserId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for provided API Key"));
    }

    private UserDetails loadUserByUserId(Integer userId) throws UsernameNotFoundException {
        return userRepo.findById(userId)
                .map(UserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found"));
    }

    @Data
    public static class UserDetails implements org.springframework.security.core.userdetails.UserDetails {

        private Integer userName;
        private String password;
        private List<GrantedAuthority> authorities;
        // yes, this is getting updated for every request
        private Boolean locked;
        private Boolean enabled;

        public UserDetails(User user) {
            userName = user.getId();
            password = user.getPassword();
            authorities = List.of(new SimpleGrantedAuthority(user.getType().toString()));
            locked = user.getLocked();
            enabled = user.getEnabled();
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
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            if (locked) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your account is locked.");
            }
            return true;
        }

        @Override
        public boolean isEnabled() {
            if (!enabled) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your account is not enabled.");
            }
            return true;
        }
    }
}