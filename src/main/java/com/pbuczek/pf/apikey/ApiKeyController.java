package com.pbuczek.pf.apikey;

import com.pbuczek.pf.security.SecurityHelper;
import com.pbuczek.pf.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/apikey")
public class ApiKeyController {

    private final UserRepository userRepo;
    private final ApiKeyRepository apiKeyRepo;
    private final SecurityHelper securityHelper;

    @Autowired
    public ApiKeyController(UserRepository userRepo, ApiKeyRepository apiKeyRepo, SecurityHelper securityHelper) {
        this.userRepo = userRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    @PreAuthorize("@securityHelper.hasContextAnyAuthorities()")
    public String createAPIKey() {
        String pass = UUID.randomUUID().toString();
        return apiKeyRepo.save(new ApiKey(pass, securityHelper.getContextCurrentUser().getId()))
                .getIdentifier() + pass; // 35 + 36 chars
    }

    @DeleteMapping(path = "/{userId}/{identifier}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public int deleteAPIKeyById(@PathVariable Integer userId, @PathVariable String identifier) {
        checkIfUserExists(userId);
        return apiKeyRepo.deleteApiKeyByIdentifier(identifier);
    }

    @GetMapping(path = "/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public List<ApiKey> getApiKeysByUserId(@PathVariable Integer userId) {
        checkIfUserExists(userId);
        return apiKeyRepo.findByUserId(userId).stream()
                .map(this::secureApiKey).toList();
    }

    @GetMapping(path = "/valid-till-date/{userId}/{identifier}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public LocalDate getValidTillDate(@PathVariable Integer userId, @PathVariable String identifier) {
        checkIfUserExists(userId);
        ApiKey apiKey = apiKeyRepo.findByIdentifier(identifier).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("apiKey with identifier '%s' not found", identifier)));
        return apiKey.getValidTillDate();
    }

    private void checkIfUserExists(Integer userId) {
        userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("user with id '%d' not found", userId)));
    }

    private ApiKey secureApiKey(ApiKey key) {
        key.setApiKeyValue("[hidden for security reasons]");
        return key;
    }
}
