package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.PaymentPlan;
import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.dto.UserDto;
import com.pbuczek.pf.security.repository.UserRepository;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    UserRepository userRepo;
    private final static String emailRegex = "^[a-zA-Z0-9][a-zA-Z0-9.!#$%&'*+-/=?^_`{|}~]*?[a-zA-Z0-9._-]?@[a-zA-Z0-9][a-zA-Z0-9._-]*?[a-zA-Z0-9]?\\.[a-zA-Z]{2,63}$";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping()
    @ResponseBody
    public List<User> readAllUsers() {
        return userRepo.findAll();
    }

    @GetMapping(value = "/{userId}")
    @ResponseBody
    public User readUser(@PathVariable Integer userId) {
        Optional<User> optionalUser = userRepo.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("user with id %d not found", userId));
        }
        return optionalUser.get();
    }

    @PostMapping
    @ResponseBody
    public User createUser(@RequestBody UserDto userDto) {
        if (userRepo.findByEmail(userDto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", userDto.getEmail()));
        }
        if (userRepo.findByUsername(userDto.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already being used by another user.", userDto.getUsername()));
        }
        if (!userDto.getEmail().matches(emailRegex)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("provided user email '%s' is not valid.", userDto.getEmail()));
        }

        return userRepo.save(new User(userDto));
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
            return user;
        }

        if (userRepo.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("email '%s' is already being used by another user.", userId));
        }

        try {
            user.setEmail(email);
            userRepo.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set email '%s' for user with id %d", email, userId));
        }
        return user;
    }

    @PatchMapping(value = "/username/{userId}/{username}")
    @ResponseBody
    public User updateUsername(@PathVariable Integer userId, @PathVariable String username) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", userId)));
        username = username.trim();

        if (user.getUsername().equals(username)) {
            return user;
        }

        if (userRepo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("username '%s' is already being used by another user.", userId));
        }

        try {
            user.setUsername(username);
            userRepo.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set username '%s' for user with id %d", username, userId));
        }
        return user;
    }

    @PatchMapping(value = "/paymentplan/{userId}/{paymentPlan}")
    @ResponseBody
    public User updatePaymentPlan(@PathVariable Integer userId, @PathVariable PaymentPlan paymentPlan) {
        User user = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", userId)));

        if (user.getPaymentPlan().equals(paymentPlan)) {
            return user;
        }

        try {
            user.setPaymentPlan(paymentPlan);
            userRepo.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set paymentPlan '%s' for user with id %d", paymentPlan, userId));
        }
        return user;
    }

    @PatchMapping(value = "/password")
    @ResponseBody
    public User updatePassword(@RequestBody PasswordDto passwordDto) {
        User user;
        try {
            user = userRepo.findByUsername( //how will it know what is 'name'? Need to test this.
                    SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "cannot authenticate current user"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "cannot find current user's data");
        }

        if (!user.getPassword().equals("") && !user.getPassword().equals(passwordDto.getCurrentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provided password is not correct");
        }

        if (!passwordDto.newPassword.matches(
                "^(?=.*[A-Z])(?=.*[0-9])([^A-Za-z0-9])((?!password).)((?!pathfinder).).{8,50}$^")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password doesn't satisfy requirement");
        }

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepo.save(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED,
                    String.format("cannot change password for current user (user's id: '%d')", user.getId()));
        }
        return user;
    }

    @Data
    @NoArgsConstructor
    private static class PasswordDto {
        private String currentPassword;
        private String newPassword;
    }

}
