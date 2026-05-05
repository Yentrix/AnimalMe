package com.iax.animalme.domain.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.service.FileStorageService;
import com.iax.animalme.infrastructure.utilities.ErrorConstants;

@Service
public class UserDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileService;

    public UserDomainService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
    }

    public void validateUniqueEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS);
        }
    }

    public User validateLogin(Optional<User> user) {
        return user.orElseThrow(() -> new RuntimeException(ErrorConstants.USER_LOGIN_INVALID));
    }

    public void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_EMPTY);
        }

        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_TOO_SHORT);
        }
    }

    public boolean validatePasswordUser(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public User updateDataUser(User ogUser, User newUser, String imageURL) {
        User updatedUser = ogUser.toBuilder()
                .firstName(newUser.getFirstName())
                .lastName(newUser.getLastName())
                .email(newUser.getEmail())
                .profilePictureUrl(imageURL)
                .country(newUser.getCountry())
                .city(newUser.getCity())
                .build();

        return userRepository.save(updatedUser);
    }

    public String saveImage(MultipartFile image, Long userId) throws Exception {
        String url = null;
        if (image != null && !image.isEmpty()) {
            url = fileService.storeFile(image, "users", userId.toString());
        }
        return url;
    }

    public User updateUser(Long id, User data, MultipartFile image) throws Exception {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(ErrorConstants.USER_NOT_FOUND));
        
        String imageUrl = saveImage(image, id);

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = user.getProfilePictureUrl();
        }

        return updateDataUser(user, data, imageUrl);
    }

    public Boolean verifyEmptyPassword(String rawPassword) {
        return rawPassword == null || rawPassword.isBlank();
    }

    public Boolean verifyPasswordDifference(String rawPassword, String encodedPassword) {
        return !passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Boolean verifyPasswordSize(String rawPassword) {
        return rawPassword.length() >= 8;
    }

    public void updatePassword(Long id, String rawPassword) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(ErrorConstants.USER_NOT_FOUND));
        
        if (verifyEmptyPassword(rawPassword)) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_EMPTY);
        }

        if (!verifyPasswordDifference(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_SAME_AS_OLD);
        }

        if (!verifyPasswordSize(rawPassword)) {
            throw new IllegalArgumentException(ErrorConstants.USER_PASSWORD_TOO_SHORT);
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}
