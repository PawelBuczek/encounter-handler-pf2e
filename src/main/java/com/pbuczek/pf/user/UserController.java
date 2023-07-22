package com.pbuczek.pf.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    private final static String passwordRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[^A-Za-z0-9]).{8,50}$";
    // below regex constant needs to match with sql rule created in the file 'src/main/resources/db/sql-files/add-user-email-validation-constraint.sql'
    private final static String emailRegex = "^[a-zA-Z0-9][a-zA-Z0-9.!#$%&'*+-/=?^_`{|}~]*?[a-zA-Z0-9._-]?@[a-zA-Z0-9][a-zA-Z0-9._-]*?[a-zA-Z0-9]?\\.[a-zA-Z]{2,63}$";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepo;

    @Autowired
    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping
    @ResponseBody
    public User createStandardUser(@RequestBody UserDto userDto) {
        if (userRepo.findByEmail(userDto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", userDto.getEmail()));
        }
        userDto.setUsername(userDto.getUsername().trim());
        if (userRepo.findByUsername(userDto.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already being used by another user.", userDto.getUsername()));
        }
        userDto.setEmail(userDto.getEmail().trim());
        if (!userDto.getEmail().matches(emailRegex)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("provided user email '%s' is not valid.", userDto.getEmail()));
        }

        checkPasswordRegex(userDto.getPassword());
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

        return secureUser(userRepo.save(new User(userDto)));
    }

//    @PostMapping(path = "/apikey")
//    @ResponseBody
//    @PreAuthorize("@securityService.hasContextAnyAuthorities()")
//    public User createAPIKeyForUser(@RequestBody ApiKeyDto apiKeyDto) {
//        apiKeyDto.setName(apiKeyDto.getName().trim());
//        String providedName = apiKeyDto.getName();
//        String realName = SecurityContextHolder.getContext().getAuthentication().getName() + "%APIKEY%" + providedName;
//        if (userRepo.findByUsername(realName).isPresent()) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT,
//                    String.format("api key with name '%s' already exists for your account.", providedName));
//        }
//
//        checkPasswordRegex(apiKeyDto.getKey());
//
//        User userForApiKey = new User();
//        userForApiKey.setUsername(realName);
//        userForApiKey.setEmail(realName); // that is one strange logic, and doesn't work :D, maybe this can be changed?
//        userForApiKey.setPassword(passwordEncoder.encode(apiKeyDto.getKey()));
//
//        return secureUser(userRepo.save(userForApiKey));
//    }

    @DeleteMapping(path = "/{userId}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUserId(#userId)")
    public int deleteUser(@PathVariable("userId") Integer userId) {
        return userRepo.deleteUserByUserId(userId);
    }

    @GetMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<User> readAllUsers() {
        return userRepo.findAll().stream().map(this::secureUser).toList();
    }

    @GetMapping(path = "/by-userid/{userId}")
    @ResponseBody
    @PreAuthorize("@securityService.hasContextAnyAuthorities()")
    public User readUser(@PathVariable Integer userId) {
        return secureUser(userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id '%d' not found", userId))
        ));
    }

    @GetMapping(path = "/by-username/{username}")
    @ResponseBody
    @PreAuthorize("@securityService.hasContextAnyAuthorities()")
    public User readUser(@PathVariable String username) {
        return secureUser(userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", username))
        ));
    }

    @PatchMapping(path = "/email/{userId}/{email}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUserId(#userId)")
    public User updateEmail(@PathVariable Integer userId, @PathVariable String email) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%d' not found", userId)));

        if (user.getEmail().equals(email)) {
            return secureUser(user);
        }

        if (userRepo.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", email));
        }

        try {
            user.setEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set email '%s' for user with id '%d'", email, userId));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/password")
    @ResponseBody
    public User updateOwnPassword(@RequestBody PasswordDto passwordDto) {
        User user;
        try {
            user = userRepo.findById(
                    Integer.valueOf(SecurityContextHolder.getContext().getAuthentication().getName())).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "cannot authenticate current user"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "cannot find current user's data");
        }

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
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/paymentplan/{userId}/{paymentPlan}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public User updatePaymentPlan(@PathVariable Integer userId, @PathVariable PaymentPlan paymentPlan) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%d' not found", userId)));

        if (user.getPaymentPlan().equals(paymentPlan)) {
            return secureUser(user);
        }

        try {
            user.setPaymentPlan(paymentPlan);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set paymentPlan '%s' for username '%d'", paymentPlan.toString(), userId));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/username/{userId}/{newUsername}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUserId(#userId)")
    public User updateUsername(@PathVariable Integer userId, @PathVariable String newUsername) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%d' not found", userId)));
        newUsername = newUsername.trim();

        if (user.getUsername().equals(newUsername)) {
            return secureUser(user);
        }

        if (userRepo.findByUsername(newUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already in use.", newUsername));
        }

        try {
            user.setUsername(newUsername);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot change username from '%s' to '%s'", user.getUsername(), newUsername));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(path = "/usertype/{userId}/{userType}")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public User updateUserType(@PathVariable Integer userId, @PathVariable UserType userType) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%d' not found", userId)));

        if (user.getType().equals(userType)) {
            return secureUser(user);
        }

        try {
            user.setType(userType);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot change userType from '%s' to '%s'", user.getType(), userType));
        }
        return secureUser(userRepo.save(user));
    }


    private User secureUser(User u) {
        u.setPassword("[hidden for security reasons]");
        return u;
    }

    private void checkPasswordRegex(String password) {
        if (!password.matches(passwordRegex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password doesn't satisfy requirement");
        }
    }


    @Data
    @NoArgsConstructor
    private static class PasswordDto {
        private String currentPassword;
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    private static class ApiKeyDto {
        private String name;
        private String key;
    }
}
