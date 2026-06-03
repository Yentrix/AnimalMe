package com.iax.animalme.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.iax.animalme.application.dto.UserPasswordUpdateDto;
import com.iax.animalme.application.dto.UserProfileUpdateDto;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.utilities.ErrorConstants;

@Service
public class UserApplicationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public User updateProfile(Long id, UserProfileUpdateDto profile) {
        User user = findById(id);

        String newEmail = profile.getEmail() == null ? "" : profile.getEmail().trim();
        if (newEmail.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacio");
        }

        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS);
            }
        });

        user.setFirstName(profile.getFirstName() == null ? "" : profile.getFirstName().trim());
        user.setEmail(newEmail);
        user.setContactEmail(profile.getContactEmail() == null ? "" : profile.getContactEmail().trim());
        user.setContactPhone(profile.getContactPhone() == null ? "" : profile.getContactPhone().trim());

        return userRepository.save(user);
    }

    public void updatePassword(Long id, UserPasswordUpdateDto passwordUpdate) {
        User user = findById(id);

        String currentPassword = passwordUpdate.getCurrentPassword();
        String newPassword = passwordUpdate.getNewPassword();
        String confirmPassword = passwordUpdate.getConfirmNewPassword();

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Debes escribir tu contraseña actual");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_EMPTY);
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_TOO_SHORT);
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("La confirmacion de la contraseña no coincide");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
