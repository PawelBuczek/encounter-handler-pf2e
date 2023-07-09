package com.pbuczek.pf.security.controller;

import com.pbuczek.pf.security.User;
import com.pbuczek.pf.security.UserType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    @GetMapping(value = "/{userId}")
    public User getUser(@PathVariable Integer userId) {

        return new User(userId, UserType.STANDARD, "test", "test.mail@com", null, null);
    }

}
