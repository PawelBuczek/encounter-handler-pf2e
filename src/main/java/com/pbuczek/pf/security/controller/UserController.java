package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    UserRepository userRepo;

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
    public User createUser(@RequestBody User newUser) {
        return userRepo.save(newUser);
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

    @PatchMapping(value = "/username/{userId}/{email}")
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

}
