package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.iax.animalme.application.dto.AdminBanUserRequestDto;
import com.iax.animalme.application.dto.AdminNotificationRequestDto;
import com.iax.animalme.domain.enums.NotificationType;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.enums.UserStatus;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.ImageRepository;
import com.iax.animalme.domain.repository.PetRepository;
import com.iax.animalme.domain.repository.PublicationRepository;
import com.iax.animalme.domain.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AdminApplicationService {
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final PetRepository petRepository;
    private final ImageRepository imageRepository;
    private final PublicationApplicationService publicationApplicationService;
    private final NotificationApplicationService notificationApplicationService;

    public AdminApplicationService(
            UserRepository userRepository,
            PublicationRepository publicationRepository,
            PetRepository petRepository,
            ImageRepository imageRepository,
            PublicationApplicationService publicationApplicationService,
            NotificationApplicationService notificationApplicationService) {
        this.userRepository = userRepository;
        this.publicationRepository = publicationRepository;
        this.petRepository = petRepository;
        this.imageRepository = imageRepository;
        this.publicationApplicationService = publicationApplicationService;
        this.notificationApplicationService = notificationApplicationService;
    }

    public List<User> listUsers(Long adminId, String query) {
        validateAdmin(adminId);

        String needle = query == null ? "" : query.trim();
        if (needle.isBlank()) {
            return userRepository.findAll();
        }

        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                needle, needle, needle);
    }

    public User banUser(Long adminId, Long userId, AdminBanUserRequestDto request) {
        validateAdmin(adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("No puedes banear a otro administrador");
        }

        String mode = request == null || request.getMode() == null
                ? "TEMPORARY"
                : request.getMode().trim().toUpperCase();
        if ("PERMANENT".equals(mode)) {
            user.setStatus(UserStatus.BANNED_PERMANENT);
            user.setBannedUntil(null);
        } else {
            Integer requestedHours = request == null ? null : request.getHours();
            int hours = (requestedHours != null && requestedHours > 0) ? requestedHours : 24;
            user.setStatus(UserStatus.BANNED_TEMPORARY);
            user.setBannedUntil(LocalDateTime.now().plusHours(hours));
        }

        return userRepository.save(user);
    }

    public User unbanUser(Long adminId, Long userId) {
        validateAdmin(adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        user.setStatus(UserStatus.ACTIVE);
        user.setBannedUntil(null);
        return userRepository.save(user);
    }

    public List<Publication> listPublications(Long adminId) {
        validateAdmin(adminId);
        return publicationRepository.findAll();
    }

    @Transactional
    public void deletePublication(Long adminId, Long publicationId) {
        validateAdmin(adminId);
        publicationApplicationService.deletePublicationAsAdmin(publicationId);
    }

    public List<Pet> listPets(Long adminId) {
        validateAdmin(adminId);
        return petRepository.findAll();
    }

    @Transactional
    public void deletePet(Long adminId, Long petId) {
        validateAdmin(adminId);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("La mascota no existe"));

        List<Publication> publications = publicationRepository.findByPetsId(petId);
        publications.forEach(pub -> {
            List<Pet> pets = new ArrayList<>(pub.getPets());
            pets.removeIf(existing -> existing.getId().equals(petId));
            pub.setPets(pets);
            publicationRepository.save(pub);
        });

        imageRepository.deleteByPetId(petId);
        petRepository.delete(pet);
    }

    public int sendNotification(Long adminId, AdminNotificationRequestDto request) {
        validateAdmin(adminId);

        String title = request.getTitle() == null ? "Aviso de AnimalMe" : request.getTitle().trim();
        String message = request.getMessage() == null ? "" : request.getMessage().trim();

        if (message.isBlank()) {
            throw new IllegalArgumentException("El mensaje de notificacion es obligatorio");
        }

        List<Long> targetUserIds = new ArrayList<>();

        if (Boolean.TRUE.equals(request.getSendToAll())) {
            userRepository.findAll().stream()
                    .filter(user -> user.getRole() != UserRole.ADMIN)
                    .filter(user -> user.getStatus() == null || user.getStatus() == UserStatus.ACTIVE)
                    .forEach(user -> targetUserIds.add(user.getId()));
        } else {
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                throw new IllegalArgumentException("Debes seleccionar al menos un usuario");
            }
            targetUserIds.addAll(request.getUserIds());
        }

        targetUserIds.stream()
                .distinct()
                .forEach(userId -> notificationApplicationService.createNotification(
                        userId,
                        NotificationType.ADMIN_MESSAGE,
                        title,
                        message,
                        null,
                        null));

        return (int) targetUserIds.stream().distinct().count();
    }

    private User validateAdmin(Long adminId) {
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("No tienes permisos de administrador");
        }

        return user;
    }
}
