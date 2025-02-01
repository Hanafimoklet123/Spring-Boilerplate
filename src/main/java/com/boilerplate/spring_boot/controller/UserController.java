package com.boilerplate.spring_boot.controller;


import com.boilerplate.spring_boot.user.User;
import com.boilerplate.spring_boot.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public List<User> getUser() {
        return userService.getUserData();
    }

    @GetMapping("/{id}")
    public Optional<User> getUserDetail(@PathVariable UUID id) {
        return userService.getUserDetailData(id);
    }


    @PostMapping("/create")
    public User createUser(@RequestBody User user) {
        return userService.createUserData(user);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUserData(id);
    }

}
