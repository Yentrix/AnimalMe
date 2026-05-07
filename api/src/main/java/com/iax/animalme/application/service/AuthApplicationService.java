package com.iax.animalme.application.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.iax.animalme.application.dto.LoginRequestDto;
import com.iax.animalme.domain.enums.UserRole;
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
        return userRepository.findByEmail(loginRequestDto.getEmail())
            .filter(user -> userDomainService.validatePasswordUser(user, loginRequestDto.getPassword()))
            .map(Optional::of)
            .map(userDomainService::validateLogin)
            .orElseThrow(() -> new RuntimeException("Incorrect email or password"));
    }

    public User registerClient(User user) {
        userDomainService.validateUniqueEmail(user.getEmail());
        userDomainService.validatePassword(user.getPassword());

        user.setRole(UserRole.USER);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setContactEmail(user.getEmail());
        return userRepository.save(user);
    }
}
