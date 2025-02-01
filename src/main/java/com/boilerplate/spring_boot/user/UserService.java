package com.boilerplate.spring_boot.user;

import com.boilerplate.spring_boot.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    public List<User> getUserData() {
        Iterable<User> usersIterable = userRepository.findAll();
        List<User> users = new ArrayList<>();
        usersIterable.forEach(users::add);
        return users;
    }

    public Optional<User> getUserDetailData(UUID id) {
        return userRepository.findById(id);
    }

    public User createUserData(User user) {
        return userRepository.save(user);
    }

    public void deleteUserData(UUID id) {
        userRepository.deleteById(id);
    }
}
