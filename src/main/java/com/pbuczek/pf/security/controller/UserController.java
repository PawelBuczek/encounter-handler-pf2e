package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.PaymentPlan;
import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.dto.UserDto;
import com.pbuczek.pf.security.repository.UserRepository;
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
import java.util.Optional;

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
    public User createUser(@RequestBody UserDto userDto) {
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

    @GetMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<User> readAllUsers() {
        return userRepo.findAll().stream().map(this::secureUser).toList();
    }

    @GetMapping(value = "/{username}")
    @ResponseBody
    @PreAuthorize("@securityService.hasContextAnyAuthorities()")
    public User readUser(@PathVariable String username) {
        return secureUser(userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", username))
        ));
    }

    @DeleteMapping(path = "/{username}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUsername(#username)")
    public int deleteUser(@PathVariable("username") String username) {
        return userRepo.deleteUserByUsername(username);
    }

    @PatchMapping(value = "/email/{username}/{email}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUsername(#username)")
    public User updateEmail(@PathVariable String username, @PathVariable String email) {
        User user = userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", username)));

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
                    String.format("cannot set email '%s' for username '%s'", email, username));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(value = "/username/{currentUsername}/{newUsername}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUsername(#currentUsername)")
    public User updateUsername(@PathVariable String currentUsername, @PathVariable String newUsername) {
        User user = userRepo.findByUsername(currentUsername).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", currentUsername)));
        newUsername = newUsername.trim();

        if (currentUsername.equals(newUsername)) {
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
                    String.format("cannot change username from '%s' to '%s'", currentUsername, newUsername));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(value = "/paymentplan/{username}/{paymentPlan}")
    @ResponseBody
    @PreAuthorize("@securityService.isContextAdminOrSpecificUsername(#username)")
    public User updatePaymentPlan(@PathVariable String username, @PathVariable PaymentPlan paymentPlan) {
        User user = userRepo.findByUsername(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("username '%s' not found", username)));

        if (user.getPaymentPlan().equals(paymentPlan)) {
            return secureUser(user);
        }

        try {
            user.setPaymentPlan(paymentPlan);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set paymentPlan '%s' for username '%s'", paymentPlan.toString(), username));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(value = "/password")
    @ResponseBody
    public User updatePassword(@RequestBody PasswordDto passwordDto) {
        User user;
        try {
            user = userRepo.findByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() ->
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
}
