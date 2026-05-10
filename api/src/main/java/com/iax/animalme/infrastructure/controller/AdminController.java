package com.iax.animalme.infrastructure.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iax.animalme.application.dto.AdminBanUserRequestDto;
import com.iax.animalme.application.dto.AdminNotificationRequestDto;
import com.iax.animalme.application.service.AdminApplicationService;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminApplicationService adminApplicationService;

    public AdminController(AdminApplicationService adminApplicationService) {
        this.adminApplicationService = adminApplicationService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> listUsers(
            @RequestParam("adminId") Long adminId,
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(adminApplicationService.listUsers(adminId, query));
    }

    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<User> banUser(
            @RequestParam("adminId") Long adminId,
            @PathVariable Long userId,
            @RequestBody AdminBanUserRequestDto request) {
        return ResponseEntity.ok(adminApplicationService.banUser(adminId, userId, request));
    }

    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<User> unbanUser(
            @RequestParam("adminId") Long adminId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(adminApplicationService.unbanUser(adminId, userId));
    }

    @GetMapping("/publications")
    public ResponseEntity<List<Publication>> listPublications(@RequestParam("adminId") Long adminId) {
        return ResponseEntity.ok(adminApplicationService.listPublications(adminId));
    }

    @DeleteMapping("/publications/{publicationId}")
    public ResponseEntity<Void> deletePublication(
            @RequestParam("adminId") Long adminId,
            @PathVariable Long publicationId) {
        adminApplicationService.deletePublication(adminId, publicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pets")
    public ResponseEntity<List<Pet>> listPets(@RequestParam("adminId") Long adminId) {
        return ResponseEntity.ok(adminApplicationService.listPets(adminId));
    }

    @DeleteMapping("/pets/{petId}")
    public ResponseEntity<Void> deletePet(
            @RequestParam("adminId") Long adminId,
            @PathVariable Long petId) {
        adminApplicationService.deletePet(adminId, petId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications")
    public ResponseEntity<Map<String, Integer>> sendNotification(
            @RequestParam("adminId") Long adminId,
            @RequestBody AdminNotificationRequestDto request) {
        int recipients = adminApplicationService.sendNotification(adminId, request);
        return ResponseEntity.ok(Map.of("recipients", recipients));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
