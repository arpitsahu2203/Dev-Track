package com.devtracker.serviceImplementation;

import com.devtracker.entities.User;
import com.devtracker.repositories.UserRepository;
import com.devtracker.services.UserService;
import com.devtracker.support.EmailNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;

    public UserServiceImplementation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User saveUser(User user) {
        user.setEmail(EmailNormalizer.normalize(user.getEmail()));
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(String email, User user) {
        User existingUser = userRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        existingUser.setName(user.getName());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        existingUser.setEmailVerified(user.isEmailVerified());
        existingUser.setPassword(user.getPassword());
        existingUser.setEnabled(user.isEnabled());
        if (user.getProvider() != null) {
            existingUser.setProvider(user.getProvider());
        }
        if (user.getProviderId() != null) {
            existingUser.setProviderId(user.getProviderId());
        }
        if (user.getProfilePic() != null) {
            existingUser.setProfilePic(user.getProfilePic());
        }
        if (user.getRoleList() != null && !user.getRoleList().isEmpty()) {
            existingUser.setRoleList(user.getRoleList());
        }

        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        userRepository.delete(user);
    }

}
