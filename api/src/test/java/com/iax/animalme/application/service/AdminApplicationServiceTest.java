package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class AdminApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private PublicationApplicationService publicationApplicationService;

    @Mock
    private NotificationApplicationService notificationApplicationService;

    @InjectMocks
    private AdminApplicationService service;

    private User adminUser() {
        return User.builder().id(1L).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
    }

    @Test
    void listUsersUsesQueryOrAllDependingOnInput() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(userRepository.findAll()).thenReturn(List.of(new User()));
        when(userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase("ana", "ana", "ana"))
                .thenReturn(List.of(new User(), new User()));

        assertEquals(1, service.listUsers(1L, " ").size());
        assertEquals(2, service.listUsers(1L, " ana ").size());
    }

    @Test
    void banUserRejectsBanningAdmin() {
        User target = User.builder().id(2L).role(UserRole.ADMIN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.banUser(1L, 2L, new AdminBanUserRequestDto()));
        assertEquals("No puedes banear a otro administrador", ex.getMessage());
    }

    @Test
    void banUserPermanentSetsPermanentStatus() {
        User target = User.builder().id(2L).role(UserRole.USER).status(UserStatus.ACTIVE).build();
        AdminBanUserRequestDto dto = new AdminBanUserRequestDto();
        dto.setMode("PERMANENT");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        User saved = service.banUser(1L, 2L, dto);

        assertEquals(UserStatus.BANNED_PERMANENT, saved.getStatus());
        assertNull(saved.getBannedUntil());
    }

    @Test
    void banUserTemporaryUsesDefaultOneDayWhenDurationIsInvalid() {
        User target = User.builder().id(2L).role(UserRole.USER).status(UserStatus.ACTIVE).build();
        AdminBanUserRequestDto dto = new AdminBanUserRequestDto();
        dto.setMode("TEMPORARY");
        dto.setDays(0);
        dto.setHours(0);
        dto.setMinutes(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        LocalDateTime before = LocalDateTime.now().plusMinutes(1439);
        User saved = service.banUser(1L, 2L, dto);
        LocalDateTime after = LocalDateTime.now().plusMinutes(1441);

        assertEquals(UserStatus.BANNED_TEMPORARY, saved.getStatus());
        assertTrue(saved.getBannedUntil().isAfter(before));
        assertTrue(saved.getBannedUntil().isBefore(after));
    }

    @Test
    void listPublicationsAndPetsFilterByNeedle() {
        Publication p1 = new Publication();
        p1.setTitle("Adopta Luna");
        Publication p2 = new Publication();
        p2.setTitle("Otro");

        Pet pet1 = Pet.builder().id(1L).name("Pelusa").build();
        Pet pet2 = Pet.builder().id(2L).name("Rocky").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(publicationRepository.findAll()).thenReturn(List.of(p1, p2));
        when(petRepository.findAll()).thenReturn(List.of(pet1, pet2));

        assertEquals(1, service.listPublications(1L, "luna").size());
        assertEquals(1, service.listPets(1L, "rock").size());
    }

    @Test
    void deletePublicationDelegatesToPublicationService() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        service.deletePublication(1L, 100L);
        verify(publicationApplicationService).deletePublicationAsAdmin(100L);
    }

    @Test
    void deletePetRemovesPetFromPublicationsAndDeletesImages() {
        Pet pet = Pet.builder().id(50L).name("Mishi").build();

        Publication pub = new Publication();
        pub.setId(7L);
        pub.setPets(List.of(pet, Pet.builder().id(9L).name("Other").build()));

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(petRepository.findById(50L)).thenReturn(Optional.of(pet));
        when(publicationRepository.findByPetsId(50L)).thenReturn(List.of(pub));

        service.deletePet(1L, 50L);

        verify(publicationRepository).save(any(Publication.class));
        verify(imageRepository).deleteByPetId(50L);
        verify(petRepository).delete(pet);
    }

    @Test
    void sendNotificationToAllTargetsOnlyActiveNonAdmins() {
        User u1 = User.builder().id(2L).role(UserRole.USER).status(UserStatus.ACTIVE).build();
        User u2 = User.builder().id(3L).role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        User u3 = User.builder().id(4L).role(UserRole.USER).status(UserStatus.BANNED_PERMANENT).build();

        AdminNotificationRequestDto dto = new AdminNotificationRequestDto();
        dto.setMessage("msg");
        dto.setSendToAll(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));
        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        int recipients = service.sendNotification(1L, dto);

        assertEquals(1, recipients);
        verify(notificationApplicationService).createNotification(
                2L,
                NotificationType.ADMIN_MESSAGE,
                "Aviso de AnimalMe",
                "msg",
                null,
                null);
    }

    @Test
    void sendNotificationToListRejectsMissingUsersAndSupportsDistinct() {
        AdminNotificationRequestDto dto = new AdminNotificationRequestDto();
        dto.setTitle("Aviso");
        dto.setMessage("Hola");
        dto.setSendToAll(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sendNotification(1L, dto));
        assertEquals("Debes seleccionar al menos un usuario", ex.getMessage());

        dto.setUserIds(List.of(9L, 9L, 10L));
        int recipients = service.sendNotification(1L, dto);
        assertEquals(2, recipients);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationApplicationService, times(2)).createNotification(idCaptor.capture(), any(), any(), any(), any(), any());
        verify(notificationApplicationService, never()).createNotification(11L, NotificationType.ADMIN_MESSAGE, "Aviso", "Hola", null, null);

        List<Long> notifiedIds = idCaptor.getAllValues();
        assertTrue(notifiedIds.contains(9L));
        assertTrue(notifiedIds.contains(10L));
    }
}
