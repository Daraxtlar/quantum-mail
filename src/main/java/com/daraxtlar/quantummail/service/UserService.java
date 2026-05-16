package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.User;
import com.daraxtlar.quantummail.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String username, String password) {

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        userRepository.save(user);
    }
}
