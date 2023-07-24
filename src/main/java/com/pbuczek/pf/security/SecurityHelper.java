package com.pbuczek.pf.security;

import com.pbuczek.pf.apikey.ApiKey;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import com.pbuczek.pf.user.UserType;
import com.pbuczek.pf.apikey.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SecurityHelper {

    UserRepository userRepo;
    ApiKeyRepository apiKeyRepo;

    @Autowired
    public SecurityHelper(UserRepository userRepo, ApiKeyRepository apiKeyRepo) {
        this.userRepo = userRepo;
        this.apiKeyRepo = apiKeyRepo;
    }

    private static final List<String> userTypes = Stream.of(UserType.values()).map(Enum::name).toList();
    public static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public User getContextCurrentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        if (name == null || name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "cannot authenticate current user");
        }
        try {
            Optional<User> optionalUser = userRepo.findById(Integer.valueOf(name));
            if (optionalUser.isPresent()) {
                return optionalUser.get();
            }
        } catch (NumberFormatException ignored) {
        }

        Integer userIdByApiKey = userRepo.getUserIdByApiKeyIdentifier(
                name.substring(0, ApiKey.IDENTIFIER_LENGTH)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "cannot authenticate current user"));

        return userRepo.findById(userIdByApiKey).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "cannot authenticate current user"));
    }

    @SuppressWarnings("unused")
    public boolean hasContextAnyAuthorities() {
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
        return getContextCurrentUser().getId().equals(userId);
    }

}
