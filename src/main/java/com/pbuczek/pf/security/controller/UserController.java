package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    UserRepository userRepo;

    @Autowired
    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping(value = "/{userId}")
    @ResponseBody
    public User getUser(@PathVariable Integer userId) {
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

}
