package com.iax.animalme.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.utilities.ErrorConstants;

@Service
public class UserApplicationService {
    private final UserRepository userRepository;

    public UserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User findById(Long id) throws RuntimeException {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ErrorConstants.USER_NOT_FOUND));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void deleteUser(@PathVariable("id") Long id) {
        userRepository.deleteById(id);
    }

    public List<User> listByFirstName(String firstName) {
        return userRepository.findByFirstNameContainingIgnoreCase(firstName);
    }

    public List<User> listByNameAndLastName(String firstName, String lastName) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(firstName, lastName);
    }

    public List<User> listByEmail(String email) {
        return userRepository.findByEmailContainingIgnoreCase(email);
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
}
