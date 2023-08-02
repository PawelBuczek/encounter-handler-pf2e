package com.pbuczek.pf.apikey;

import com.pbuczek.pf.security.SecurityHelper;
import com.pbuczek.pf.user.PaymentPlan;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import com.pbuczek.pf.user.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/apikey")
public class ApiKeyController {

    public final static Map<PaymentPlan, Integer> API_KEY_LIMITS = Map.of(
            PaymentPlan.FREE, 0,
            PaymentPlan.ADVENTURER, 2,
            PaymentPlan.HERO, 5);

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
        Integer limit = API_KEY_LIMITS.get(user.getPaymentPlan());
        if(!user.getType().equals(UserType.ADMIN) && apiKeyRepo.getCountOfApiKeysByUserId(user.getId()) >= limit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Cannot create. Reached limit of API Keys: %d", limit));
        }

        String pass = UUID.randomUUID().toString();
        return apiKeyRepo.save(new ApiKey(pass, user.getId()))
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
