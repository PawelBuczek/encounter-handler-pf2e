package com.pbuczek.pf.user;

import com.pbuczek.pf.security.SecurityHelper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    private final static String PASSWORD_REGEX = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[^A-Za-z0-9]).{8,50}$";
    // below regex constant needs to match with sql rule created in the file 'src/main/resources/db/sql-files/add-user-email-validation-constraint.sql'
    private final static String EMAIL_REGEX = "^[a-zA-Z0-9.!#$%&'*+-/=?^_`{|}~]*?[a-zA-Z0-9._-]?@[a-zA-Z0-9][a-zA-Z0-9._-]*?[a-zA-Z0-9]?\\.[a-zA-Z]{2,63}$";

    private final PasswordEncoder passwordEncoder = SecurityHelper.passwordEncoder;

    private final UserRepository userRepo;
    private final SecurityHelper securityHelper;

    @Autowired
    public UserController(UserRepository userRepo, SecurityHelper securityHelper) {
        this.userRepo = userRepo;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    public User createStandardUser(@RequestBody UserDto userDto) {
        checkEmail(userDto.getEmail());
        checkUsername(userDto.getUsername());

        checkPasswordRegex(userDto.getPassword());

        return secureUser(userRepo.save(new User(userDto)));
    }

    @DeleteMapping(path = "/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public int deleteUser(@PathVariable Integer userId) {
        securityHelper.ensureRequestIsNotByApiKey();
        return userRepo.deleteUser(userId);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<User> readAllUsers() {
        return userRepo.findAll().stream().map(this::secureUser).toList();
    }

    @GetMapping(path = "/by-userid/{userId}")
    @PreAuthorize("@securityHelper.hasContextAnyAuthorities()")
    public User readUser(@PathVariable Integer userId) {
        return secureUser(getUserById(userId));
    }

    @GetMapping(path = "/by-username/{username}")
    @PreAuthorize("@securityHelper.hasContextAnyAuthorities()")
    public User readUser(@PathVariable String username) {
        return secureUser(userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", username))
        ));
    }

    @PatchMapping(path = "/email/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public User updateEmail(@PathVariable Integer userId, @RequestBody String email) {
        User user = getUserById(userId);

        if (user.getEmail().equals(email)) {
            return secureUser(user);
        }

        securityHelper.ensureRequestIsNotByApiKey();
        checkEmail(email);

        user.setEmail(email);
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/password")
    public User updateOwnPassword(@RequestBody PasswordDto passwordDto) {
        User user = securityHelper.getContextCurrentUser();

        securityHelper.ensureRequestIsNotByApiKey();
        if (!BCrypt.checkpw(passwordDto.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provided current password is not correct");
        }

        checkPasswordRegex(passwordDto.getNewPassword());

        try {
            user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("cannot change password for current user (user's id: '%d')", user.getId()));
        }

        user.refreshPasswordLastUpdatedDate();
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/paymentplan/{userId}/{paymentPlan}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User updatePaymentPlan(@PathVariable Integer userId, @PathVariable PaymentPlan paymentPlan) {
        User user = getUserById(userId);

        if (user.getPaymentPlan().equals(paymentPlan)) {
            return secureUser(user);
        }

        securityHelper.ensureRequestIsNotByApiKey();
        user.setPaymentPlan(paymentPlan);
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/username/{userId}/{newUsername}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public User updateUsername(@PathVariable Integer userId, @PathVariable String newUsername) {
        User user = getUserById(userId);

        if (user.getUsername().equals(newUsername)) {
            return secureUser(user);
        }

        securityHelper.ensureRequestIsNotByApiKey();
        checkUsername(newUsername);

        user.setUsername(newUsername);
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/enable/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User enableUser(@PathVariable Integer userId) {
        User user = getUserById(userId);

        if (user.getEnabled()) {
            return secureUser(user);
        }

        user.setEnabled(Boolean.TRUE);
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/usertype/{userId}/{userType}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User updateUserType(@PathVariable Integer userId, @PathVariable UserType userType) {
        User user = getUserById(userId);

        if (user.getType().equals(userType)) {
            return secureUser(user);
        }

        user.setType(userType);
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/refresh-password-last-updated-date/{userId}")
    @PreAuthorize("@securityHelper.isContextAdminOrSpecificUserId(#userId)")
    public User refreshPasswordLastUpdatedDate(@PathVariable Integer userId) {
        User user = getUserById(userId);
        user.refreshPasswordLastUpdatedDate();
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/lock-unlock/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User lockUnlock(@PathVariable Integer userId) {
        User user = getUserById(userId);

        user.setLocked(!user.getLocked());
        return secureUser(userRepo.save(user));
    }

    private User secureUser(User u) {
        u.setPassword("[hidden for security reasons]");
        return u;
    }

    private void checkPasswordRegex(String password) {
        if (!password.matches(PASSWORD_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password doesn't satisfy requirement");
        }
    }

    private void checkUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "provided username is empty.");
        }
        if (username.trim().length() < User.MIN_USERNAME_LENGTH || username.trim().length() > User.MAX_USERNAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("username needs to be between %d and %d characters.",
                            User.MIN_USERNAME_LENGTH, User.MAX_USERNAME_LENGTH));
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already being used by another user.", username));
        }
    }

    private void checkEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "provided email is empty.");
        }
        if (userRepo.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", email));
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("provided user email '%s' is not valid.", email));
        }
    }

    private User getUserById(Integer id) {
        return userRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id '%d' not found", id)));
    }

    @Data
    @NoArgsConstructor
    private static class PasswordDto {
        private String currentPassword;
        private String newPassword;
    }

}
