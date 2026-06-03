package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.application.dto.PublicationCreateRequestDto;
import com.iax.animalme.application.dto.PublicationUpdateRequestDto;
import com.iax.animalme.domain.enums.AdoptionStatus;
import com.iax.animalme.domain.enums.NotificationType;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.enums.RequestStatus;
import com.iax.animalme.domain.model.AdoptionRequest;
import com.iax.animalme.domain.model.Image;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.AdoptionRequestRepository;
import com.iax.animalme.domain.repository.CommentRepository;
import com.iax.animalme.domain.repository.ImageRepository;
import com.iax.animalme.domain.repository.PetRepository;
import com.iax.animalme.domain.repository.PublicationRepository;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.service.FileStorageService;

@ExtendWith(MockitoExtension.class)
class PublicationApplicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private AdoptionRequestRepository adoptionRequestRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationApplicationService notificationApplicationService;

    @InjectMocks
    private PublicationApplicationService service;

    @Test
    void createPublicationRequiresAtLeastOnePet() {
        PublicationCreateRequestDto request = new PublicationCreateRequestDto();
        request.setPetIds(new ArrayList<>());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createPublication(request, 1L, null));
        assertEquals("La publicacion debe incluir al menos una mascota", ex.getMessage());
    }

    @Test
    void createPublicationRejectsPetsNotOwnedByAuthor() {
        PublicationCreateRequestDto request = new PublicationCreateRequestDto();
        request.setPetIds(List.of(1L));

        User author = User.builder().id(50L).build();
        Pet pet = Pet.builder().id(1L).name("Luna").owner(User.builder().id(999L).build()).build();

        when(userRepository.findById(50L)).thenReturn(Optional.of(author));
        when(petRepository.findAllById(List.of(1L))).thenReturn(List.of(pet));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createPublication(request, 50L, null));
        assertEquals("Solo puedes publicar mascotas que te pertenecen", ex.getMessage());
    }

    @Test
    void createPublicationBuildsTitleCopiesPetImagesAndUploadsFiles() throws Exception {
        PublicationCreateRequestDto request = new PublicationCreateRequestDto();
        request.setTitle(" ");
        request.setDescription("desc");
        request.setAdoptionStatus(null);
        request.setPetIds(List.of(1L, 2L));

        User author = User.builder().id(50L).build();

        Image pet1ImageA = new Image();
        pet1ImageA.setUrl("u1");
        Image pet1ImageB = new Image();
        pet1ImageB.setUrl("u2");

        Pet pet1 = Pet.builder().id(1L).name("Luna").owner(User.builder().id(50L).build()).build();
        pet1.setImages(List.of(pet1ImageA, pet1ImageB));

        Image pet2ImageDuplicate = new Image();
        pet2ImageDuplicate.setUrl("u2");
        Image pet2ImageNew = new Image();
        pet2ImageNew.setUrl("u3");
        Pet pet2 = Pet.builder().id(2L).name("Milo").owner(User.builder().id(50L).build()).build();
        pet2.setImages(List.of(pet2ImageDuplicate, pet2ImageNew));

        MockMultipartFile file1 = new MockMultipartFile("images", "one.jpg", "image/jpeg", "1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("images", "two.jpg", "image/jpeg", new byte[0]);

        when(userRepository.findById(50L)).thenReturn(Optional.of(author));
        when(petRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(pet1, pet2));
        when(publicationRepository.save(any(Publication.class))).thenAnswer(invocation -> {
            Publication p = invocation.getArgument(0);
            p.setId(999L);
            return p;
        });
        when(fileStorageService.storeFile(file1, "publications", "999")).thenReturn("uploaded");

        Publication result = service.createPublication(request, 50L, new MultipartFile[] { file1, file2 });

        assertEquals("Luna, Milo", result.getTitle());
        assertEquals(AdoptionStatus.AVAILABLE, result.getAdoptionStatus());
        assertEquals(0L, result.getPendingRequestsCount());
        verify(imageRepository, times(4)).save(any(Image.class));
    }

    @Test
    void findByStatusAndAuthorEnrichPendingCount() {
        Publication p1 = new Publication();
        p1.setId(1L);
        Publication p2 = new Publication();
        p2.setId(2L);

        when(publicationRepository.findByStatusOrderByCreatedAtDesc(PublicationStatus.OPEN)).thenReturn(List.of(p1));
        when(publicationRepository.findByAuthorIdOrderByCreatedAtDesc(99L)).thenReturn(List.of(p2));
        when(adoptionRequestRepository.countByPublicationIdAndStatus(1L, RequestStatus.PENDING)).thenReturn(3L);
        when(adoptionRequestRepository.countByPublicationIdAndStatus(2L, RequestStatus.PENDING)).thenReturn(5L);

        List<Publication> byStatus = service.findByStatusOrderByCreatedAtDesc(PublicationStatus.OPEN);
        List<Publication> byAuthor = service.findByAuthorIdOrderByCreatedAtDesc(99L);

        assertEquals(3L, byStatus.get(0).getPendingRequestsCount());
        assertEquals(5L, byAuthor.get(0).getPendingRequestsCount());
    }

    @Test
    void updatePublicationRejectsUnauthorizedAuthor() {
        Publication publication = new Publication();
        publication.setId(7L);
        publication.setAuthor(User.builder().id(5L).build());

        PublicationUpdateRequestDto dto = new PublicationUpdateRequestDto();
        dto.setPetIds(List.of(1L));

        when(publicationRepository.findById(7L)).thenReturn(Optional.of(publication));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updatePublication(7L, 99L, dto, null));
        assertEquals("No tienes permisos sobre esta publicacion", ex.getMessage());
    }

    @Test
    void createAdoptionRequestValidatesRulesAndNotifiesAuthor() {
        Publication publication = new Publication();
        publication.setId(5L);
        publication.setTitle("Adopcion de Misha");
        publication.setStatus(PublicationStatus.OPEN);
        publication.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        publication.setAuthor(User.builder().id(100L).build());

        User applicant = User.builder().id(200L).firstName("Ana").build();

        when(publicationRepository.findById(5L)).thenReturn(Optional.of(publication));
        when(adoptionRequestRepository.existsByPublicationIdAndApplicantIdAndStatus(5L, 200L, RequestStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findById(200L)).thenReturn(Optional.of(applicant));
        when(adoptionRequestRepository.save(any(AdoptionRequest.class))).thenAnswer(invocation -> {
            AdoptionRequest req = invocation.getArgument(0);
            req.setId(999L);
            return req;
        });

        AdoptionRequest saved = service.createAdoptionRequest(5L, 200L, "quiero adoptar");

        assertNotNull(saved.getCreatedAt());
        assertEquals(RequestStatus.PENDING, saved.getStatus());
        verify(notificationApplicationService).createNotification(
                eq(100L),
                eq(NotificationType.ADOPTION_REQUEST_RECEIVED),
                eq("Nueva solicitud de adopcion"),
                any(String.class),
                eq(5L),
                eq(999L));
    }

            @Test
            void createAdoptionRequestRejectsClosedSelfOrDuplicated() {
            Publication closed = new Publication();
            closed.setId(5L);
            closed.setStatus(PublicationStatus.CLOSED);
            closed.setAdoptionStatus(AdoptionStatus.AVAILABLE);
            closed.setAuthor(User.builder().id(100L).build());

            when(publicationRepository.findById(5L)).thenReturn(Optional.of(closed));
            IllegalArgumentException closedEx = assertThrows(IllegalArgumentException.class,
                () -> service.createAdoptionRequest(5L, 200L, "x"));
            assertEquals("La publicacion ya no acepta solicitudes", closedEx.getMessage());

            Publication own = new Publication();
            own.setId(6L);
            own.setStatus(PublicationStatus.OPEN);
            own.setAdoptionStatus(AdoptionStatus.AVAILABLE);
            own.setAuthor(User.builder().id(200L).build());

            when(publicationRepository.findById(6L)).thenReturn(Optional.of(own));
            IllegalArgumentException ownEx = assertThrows(IllegalArgumentException.class,
                () -> service.createAdoptionRequest(6L, 200L, "x"));
            assertEquals("No puedes solicitar adopcion en tu propia publicacion", ownEx.getMessage());

            Publication duplicated = new Publication();
            duplicated.setId(7L);
            duplicated.setStatus(PublicationStatus.OPEN);
            duplicated.setAdoptionStatus(AdoptionStatus.AVAILABLE);
            duplicated.setAuthor(User.builder().id(100L).build());

            when(publicationRepository.findById(7L)).thenReturn(Optional.of(duplicated));
            when(adoptionRequestRepository.existsByPublicationIdAndApplicantIdAndStatus(7L, 200L, RequestStatus.PENDING))
                .thenReturn(true);

            IllegalArgumentException dupEx = assertThrows(IllegalArgumentException.class,
                () -> service.createAdoptionRequest(7L, 200L, "x"));
            assertEquals("Ya tienes una solicitud pendiente para esta publicacion", dupEx.getMessage());
            }

            @Test
            void getPublicationRequestsSortsPendingFirstThenRecent() {
            Publication publication = new Publication();
            publication.setId(8L);
            publication.setAuthor(User.builder().id(1L).build());

            AdoptionRequest archived = new AdoptionRequest();
            archived.setId(1L);
            archived.setStatus(RequestStatus.ARCHIVED);
            archived.setCreatedAt(LocalDateTime.now().minusDays(1));

            AdoptionRequest pendingOld = new AdoptionRequest();
            pendingOld.setId(2L);
            pendingOld.setStatus(RequestStatus.PENDING);
            pendingOld.setCreatedAt(LocalDateTime.now().minusHours(2));

            AdoptionRequest pendingNew = new AdoptionRequest();
            pendingNew.setId(3L);
            pendingNew.setStatus(RequestStatus.PENDING);
            pendingNew.setCreatedAt(LocalDateTime.now().minusMinutes(10));

            when(publicationRepository.findById(8L)).thenReturn(Optional.of(publication));
            when(adoptionRequestRepository.findByPublicationIdOrderByCreatedAtDesc(8L))
                .thenReturn(List.of(archived, pendingOld, pendingNew));

            List<AdoptionRequest> sorted = service.getPublicationRequests(8L, 1L);
            assertEquals(3L, sorted.get(0).getId());
            assertEquals(2L, sorted.get(1).getId());
            assertEquals(1L, sorted.get(2).getId());
            }

    @Test
    void changeAdoptionRequestStatusAcceptedClosesPublicationAndArchivesPending() {
        Publication publication = new Publication();
        publication.setId(7L);
        publication.setTitle("Pub");
        publication.setAuthor(User.builder().id(1L).build());

        AdoptionRequest accepted = new AdoptionRequest();
        accepted.setId(20L);
        accepted.setPublication(publication);
        accepted.setApplicant(User.builder().id(3L).build());
        accepted.setCreatedAt(LocalDateTime.now());

        AdoptionRequest pending = new AdoptionRequest();
        pending.setId(21L);
        pending.setStatus(RequestStatus.PENDING);

        when(publicationRepository.findById(7L)).thenReturn(Optional.of(publication));
        when(adoptionRequestRepository.findById(20L)).thenReturn(Optional.of(accepted));
        when(adoptionRequestRepository.save(accepted)).thenReturn(accepted);
        when(adoptionRequestRepository.findByPublicationIdAndStatus(7L, RequestStatus.PENDING)).thenReturn(List.of(pending));

        AdoptionRequest result = service.changeAdoptionRequestStatus(7L, 20L, 1L, RequestStatus.ACCEPTED);

        assertEquals(RequestStatus.ACCEPTED, result.getStatus());
        assertEquals(PublicationStatus.CLOSED, publication.getStatus());
        assertEquals(AdoptionStatus.ADOPTED, publication.getAdoptionStatus());
        assertEquals(RequestStatus.ARCHIVED, pending.getStatus());
        verify(publicationRepository).save(publication);
        verify(notificationApplicationService).createNotification(
                eq(3L),
                eq(NotificationType.ADOPTION_REQUEST_ACCEPTED),
                eq("Tu solicitud fue aceptada"),
                any(String.class),
                eq(7L),
                eq(20L));
    }

    @Test
    void changeAdoptionRequestStatusRejectsRequestFromDifferentPublication() {
        Publication publication = new Publication();
        publication.setId(9L);
        publication.setAuthor(User.builder().id(1L).build());

        Publication otherPublication = new Publication();
        otherPublication.setId(99L);

        AdoptionRequest request = new AdoptionRequest();
        request.setId(10L);
        request.setPublication(otherPublication);

        when(publicationRepository.findById(9L)).thenReturn(Optional.of(publication));
        when(adoptionRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changeAdoptionRequestStatus(9L, 10L, 1L, RequestStatus.REJECTED));
        assertEquals("La solicitud no pertenece a esta publicacion", ex.getMessage());
    }

    @Test
    void deleteAdoptionRequestValidatesOwnershipAndPublication() {
        Publication publication = new Publication();
        publication.setId(11L);
        publication.setAuthor(User.builder().id(1L).build());

        Publication otherPublication = new Publication();
        otherPublication.setId(99L);

        AdoptionRequest request = new AdoptionRequest();
        request.setId(12L);
        request.setPublication(otherPublication);

        when(publicationRepository.findById(11L)).thenReturn(Optional.of(publication));
        when(adoptionRequestRepository.findById(12L)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteAdoptionRequest(11L, 12L, 1L));
        assertEquals("La solicitud no pertenece a esta publicacion", ex.getMessage());

        request.setPublication(publication);
        service.deleteAdoptionRequest(11L, 12L, 1L);
        verify(adoptionRequestRepository).delete(request);
    }

    @Test
    void deletePublicationRequiresAuthorOwnership() {
        Publication publication = new Publication();
        publication.setId(13L);
        publication.setAuthor(User.builder().id(8L).build());

        when(publicationRepository.findById(13L)).thenReturn(Optional.of(publication));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deletePublication(13L, 1L));
        assertEquals("No tienes permisos sobre esta publicacion", ex.getMessage());

        publication.setAuthor(User.builder().id(1L).build());
        publication.setPets(new ArrayList<>());
        service.deletePublication(13L, 1L);
        verify(commentRepository).deleteByPublicationId(13L);
        verify(adoptionRequestRepository).deleteByPublicationId(13L);
        verify(imageRepository).deleteByPublicationId(13L);
    }

    @Test
    void updatePublicationRejectsWhenPetListDiffersFromRequested() {
        Publication publication = new Publication();
        publication.setId(15L);
        publication.setAuthor(User.builder().id(1L).build());

        PublicationUpdateRequestDto dto = new PublicationUpdateRequestDto();
        dto.setPetIds(List.of(1L, 2L));

        when(publicationRepository.findById(15L)).thenReturn(Optional.of(publication));
        when(petRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(Pet.builder().id(1L).build()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updatePublication(15L, 1L, dto, null));
        assertEquals("Hay mascotas seleccionadas que no existen", ex.getMessage());
        assertTrue(ex.getMessage().contains("mascotas"));
    }

    @Test
    void updatePublicationAppliesImageRemovalsAndStatusTransitions() throws Exception {
        User author = User.builder().id(1L).build();

        Pet oldPet = Pet.builder().id(1L).name("Old").owner(author).build();
        Pet newPet = Pet.builder().id(2L).name("New").owner(author).build();

        Image newPetImage = new Image();
        newPetImage.setUrl("from-pet");
        newPet.setImages(List.of(newPetImage));

        Publication publication = new Publication();
        publication.setId(20L);
        publication.setAuthor(author);
        publication.setStatus(PublicationStatus.CLOSED);
        publication.setPets(List.of(oldPet));

        Image existing1 = new Image();
        existing1.setId(100L);
        existing1.setUrl("existing-1");
        Image existing2 = new Image();
        existing2.setId(101L);
        existing2.setUrl("existing-2");

        PublicationUpdateRequestDto dto = new PublicationUpdateRequestDto();
        dto.setTitle(" ");
        dto.setDescription("  updated description  ");
        dto.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        dto.setPetIds(List.of(2L));
        dto.setRemovedImageIds(List.of(100L));

        MockMultipartFile upload = new MockMultipartFile("images", "x.jpg", "image/jpeg", "x".getBytes());

        when(publicationRepository.findById(20L)).thenReturn(Optional.of(publication));
        when(petRepository.findAllById(List.of(2L))).thenReturn(List.of(newPet));
        when(imageRepository.findByPublicationId(20L)).thenReturn(List.of(existing1, existing2));
        when(fileStorageService.storeFile(upload, "publications", "20")).thenReturn("uploaded-url");
        when(publicationRepository.save(publication)).thenReturn(publication);
        when(adoptionRequestRepository.countByPublicationIdAndStatus(20L, RequestStatus.PENDING)).thenReturn(2L);

        Publication updated = service.updatePublication(20L, 1L, dto, new MultipartFile[] { upload });

        assertEquals("New", updated.getTitle());
        assertEquals("updated description", updated.getDescription());
        assertEquals(PublicationStatus.OPEN, updated.getStatus());
        assertEquals(2L, updated.getPendingRequestsCount());

        verify(imageRepository).deleteAll(any());
        verify(imageRepository, times(2)).save(any(Image.class));
    }

    @Test
    void changeAdoptionRequestStatusRejectedDoesNotClosePublication() {
        Publication publication = new Publication();
        publication.setId(30L);
        publication.setAuthor(User.builder().id(1L).build());
        publication.setStatus(PublicationStatus.OPEN);

        AdoptionRequest request = new AdoptionRequest();
        request.setId(31L);
        request.setPublication(publication);

        when(publicationRepository.findById(30L)).thenReturn(Optional.of(publication));
        when(adoptionRequestRepository.findById(31L)).thenReturn(Optional.of(request));
        when(adoptionRequestRepository.save(request)).thenReturn(request);

        AdoptionRequest saved = service.changeAdoptionRequestStatus(30L, 31L, 1L, RequestStatus.REJECTED);

        assertEquals(RequestStatus.REJECTED, saved.getStatus());
        assertEquals(PublicationStatus.OPEN, publication.getStatus());
    }

    @Test
    void deletePublicationAsAdminFailsWhenPublicationNotFound() {
        when(publicationRepository.findById(123L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deletePublicationAsAdmin(123L));
        assertEquals("La publicacion no existe", ex.getMessage());
    }

    @Test
    void deletePublicationAsAdminRemovesAssociationsAndChildren() {
        Publication publication = new Publication();
        publication.setId(9L);
        publication.setPets(new ArrayList<>());

        when(publicationRepository.findById(9L)).thenReturn(Optional.of(publication));

        service.deletePublicationAsAdmin(9L);

        verify(commentRepository).deleteByPublicationId(9L);
        verify(adoptionRequestRepository).deleteByPublicationId(9L);
        verify(imageRepository).deleteByPublicationId(9L);
        verify(publicationRepository).delete(publication);
    }
}
