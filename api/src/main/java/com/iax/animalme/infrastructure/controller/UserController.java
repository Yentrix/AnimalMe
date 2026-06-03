package com.iax.animalme.infrastructure.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iax.animalme.application.dto.UserPasswordUpdateDto;
import com.iax.animalme.application.dto.UserProfileUpdateDto;
import com.iax.animalme.application.service.UserApplicationService;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserApplicationService userApplicationService;

    public UserController(
            UserApplicationService userApplicationService, UserRepository userRepository) {
        this.userApplicationService = userApplicationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<User>> listByRole(@RequestParam(required = false) UserRole role) {
        System.out.println("DEBUG: El rol recibido es: " + role);

        if (role != null) {
            System.out.println("Filtrando por rol...");
            return ResponseEntity.ok(userApplicationService.getUsersByRole(role));
        }

        System.out.println("No hay rol, devolviendo todos.");
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userApplicationService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/searchByName")
    public List<User> searchByName(@RequestParam String name) {
        return userApplicationService.listByFirstName(name);
    }

    @GetMapping("/searchByEmail")
    public List<User> searchByEmail(@RequestParam String email) {
        return userApplicationService.listByEmail(email);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable Long id, @RequestBody UserProfileUpdateDto profile) {
        return ResponseEntity.ok(userApplicationService.updateProfile(id, profile));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDto passwordUpdate) {
        userApplicationService.updatePassword(id, passwordUpdate);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, RuntimeException.class})
    public ResponseEntity<Map<String, String>> handleException(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

}
