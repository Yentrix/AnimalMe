package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.iax.animalme.application.dto.LoginRequestDto;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.enums.UserStatus;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.domain.service.UserDomainService;

@Service
public class AuthApplicationService {
    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;

    public AuthApplicationService(
        UserRepository userRepository,
        UserDomainService userDomainService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.passwordEncoder = passwordEncoder;
    }

    public User login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
            .filter(foundUser -> userDomainService.validatePasswordUser(foundUser, loginRequestDto.getPassword()))
            .map(Optional::of)
            .map(userDomainService::validateLogin)
            .orElseThrow(() -> new RuntimeException("Incorrect email or password"));

        if (user.getStatus() == UserStatus.BANNED_PERMANENT) {
            throw new IllegalArgumentException("Tu cuenta ha sido baneada permanentemente");
        }

        if (user.getStatus() == UserStatus.BANNED_TEMPORARY) {
            if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Tu cuenta esta baneada temporalmente");
            }

            user.setStatus(UserStatus.ACTIVE);
            user.setBannedUntil(null);
            user = userRepository.save(user);
        }

        return user;
    }

    public User registerClient(User user) {
        userDomainService.validateUniqueEmail(user.getEmail());
        userDomainService.validatePassword(user.getPassword());

        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setBannedUntil(null);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setContactEmail(user.getEmail());
        return userRepository.save(user);
    }
}
