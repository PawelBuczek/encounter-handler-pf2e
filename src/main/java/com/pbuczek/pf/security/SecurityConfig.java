package com.pbuczek.pf.security;

import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityConfig.UserDetailsService userDetailsService() {
        return new UserDetailsService();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults())
                .addFilterBefore(new ApiKeyFilter(userDetailsService()), UsernamePasswordAuthenticationFilter.class)
                .build();
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
            return true;
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

    @Component
    public static class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

        @Autowired
        private UserRepository userRepo;

        @Override
        public SecurityConfig.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            return userRepo.findByUsername(username)
                    .map(UserDetails::new)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found"));
        }

        public SecurityConfig.UserDetails loadUserByUserId(Integer userId) throws UsernameNotFoundException {
            return userRepo.findById(userId)
                    .map(UserDetails::new)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found"));
        }

        public SecurityConfig.UserDetails loadByApiKey(String apiKey) {
            return userRepo.getUserIdByApiKey(apiKey)
                    .map(this::loadUserByUserId)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found for provided API Key"));
        }
    }
}