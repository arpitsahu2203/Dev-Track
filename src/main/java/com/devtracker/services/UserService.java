package com.devtracker.services;

import com.devtracker.entities.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User saveUser(User user);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers();

    User updateUser(String email, User user);

    void deleteUser(String email);
}
