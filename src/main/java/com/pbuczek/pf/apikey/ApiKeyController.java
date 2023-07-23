package com.pbuczek.pf.apikey;

import com.pbuczek.pf.security.SecurityHelper;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/apiKey")
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
        User user = securityHelper.getContextCurrentUser();

        return apiKeyRepo.save(new ApiKey(user.getId())).getApiKeyValue();
    }

    @DeleteMapping(path = "/by-id/{userId}/{apiKeyId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public int deleteAPIKeyById(@PathVariable Integer userId, @PathVariable Integer apiKeyId) {
        checkIfUserExists(userId);
        return apiKeyRepo.deleteApiKeyById(apiKeyId);
    }

    @DeleteMapping(path = "/by-value/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public int deleteAPIKeyByValue(@PathVariable Integer userId, @RequestBody String apiKeyValue) {
        checkIfUserExists(userId);
        return apiKeyRepo.deleteApiKeyByValue(apiKeyValue);
    }

    @GetMapping(path = "/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public List<ApiKey> getApiKeysByUserId(@PathVariable Integer userId) {
        checkIfUserExists(userId);
        return apiKeyRepo.findByUserId(userId);
    }

    private void checkIfUserExists(Integer userId) {
        userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("user with id '%d' not found", userId)));
    }
}
