package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.PaymentPlan;
import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.dto.UserDto;
import com.pbuczek.pf.security.repository.UserRepository;
import com.pbuczek.pf.security.service.SecurityService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private final SecurityService securityService;

    @Autowired
    public UserController(UserRepository userRepo, SecurityService securityService) {
        this.userRepo = userRepo;
        this.securityService = securityService;
    }

    @GetMapping()
    @ResponseBody
    public List<User> readAllUsers() {
        return userRepo.findAll().stream()
                .map(this::secureUser).toList();
    }

    @GetMapping(value = "/{username}")
    @ResponseBody
    public User readUser(@PathVariable String username) {
        if (!securityService.isContextAdminOrSpecificUsername(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("No acccess to user with username: %s", username));
        }
        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("username %s not found", username));
        }
        return secureUser(optionalUser.get());
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

    @DeleteMapping(value = "/{userId}")
    @ResponseBody
    public int deleteUser(@PathVariable Integer userId) {
        return userRepo.deleteUser(userId);
    }

    @PatchMapping(value = "/email/{userId}/{email}")
    @ResponseBody
    public User updateEmail(@PathVariable Integer userId, @PathVariable String email) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", userId)));
        email = email.trim();

        if (user.getEmail().equals(email)) {
            return secureUser(user);
        }

        if (userRepo.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", userId));
        }

        try {
            user.setEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set email '%s' for user with id %d", email, userId));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(value = "/username/{userId}/{username}")
    @ResponseBody
    public User updateUsername(@PathVariable Integer userId, @PathVariable String username) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", userId)));
        username = username.trim();

        if (user.getUsername().equals(username)) {
            return secureUser(user);
        }

        if (userRepo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already being used by another user.", userId));
        }

        try {
            user.setUsername(username);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set username '%s' for user with id %d", username, userId));
        }
        return secureUser(userRepo.save(user));
    }

    @PatchMapping(value = "/paymentplan/{userId}/{paymentPlan}")
    @ResponseBody
    public User updatePaymentPlan(@PathVariable Integer userId, @PathVariable PaymentPlan paymentPlan) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", userId)));

        if (user.getPaymentPlan().equals(paymentPlan)) {
            return secureUser(user);
        }

        try {
            user.setPaymentPlan(paymentPlan);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set paymentPlan '%s' for user with id %d", paymentPlan, userId));
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
