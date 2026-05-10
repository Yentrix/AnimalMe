package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

import jakarta.transaction.Transactional;

@Service
public class PublicationApplicationService {
    private final PublicationRepository publicationRepository;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AdoptionRequestRepository adoptionRequestRepository;
    private final CommentRepository commentRepository;
    private final NotificationApplicationService notificationApplicationService;

    public PublicationApplicationService(PublicationRepository publicationRepository, ImageRepository imageRepository,
            FileStorageService fileStorageService, UserRepository userRepository, PetRepository petRepository,
            AdoptionRequestRepository adoptionRequestRepository, CommentRepository commentRepository,
            NotificationApplicationService notificationApplicationService) {
        this.publicationRepository = publicationRepository;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.adoptionRequestRepository = adoptionRequestRepository;
        this.commentRepository = commentRepository;
        this.notificationApplicationService = notificationApplicationService;
    }

    @Transactional
    public Publication createPublication(PublicationCreateRequestDto request, Long authorId, MultipartFile[] images) throws Exception {
        if (request.getPetIds() == null || request.getPetIds().isEmpty()) {
            throw new IllegalArgumentException("La publicacion debe incluir al menos una mascota");
        }

        User author = userRepository.findById(authorId).orElseThrow();

        List<Pet> pets = petRepository.findAllById(request.getPetIds());
        if (pets.size() != request.getPetIds().size()) {
            throw new IllegalArgumentException("Hay mascotas seleccionadas que no existen");
        }

        for (Pet pet : pets) {
            if (pet.getOwner() == null || !authorId.equals(pet.getOwner().getId())) {
                throw new IllegalArgumentException("Solo puedes publicar mascotas que te pertenecen");
            }
        }

        Publication pub = new Publication();
        pub.setPets(pets);
        pub.setAuthor(author);
        pub.setCreatedAt(LocalDateTime.now());
        pub.setStatus(PublicationStatus.OPEN);
        pub.setDescription(request.getDescription());
        pub.setAdoptionStatus(request.getAdoptionStatus() != null ? request.getAdoptionStatus() : AdoptionStatus.AVAILABLE);

        String generatedTitle = pets.stream().map(Pet::getName).collect(Collectors.joining(", "));
        pub.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : generatedTitle);

        Publication savedPub = publicationRepository.save(pub);

        Set<String> copiedUrls = new HashSet<>();
        for (Pet pet : pets) {
            if (pet.getImages() == null) {
                continue;
            }
            for (Image petImage : pet.getImages()) {
                if (petImage.getUrl() == null || !copiedUrls.add(petImage.getUrl())) {
                    continue;
                }

                Image img = new Image();
                img.setUrl(petImage.getUrl());
                img.setPublication(savedPub);
                imageRepository.save(img);
            }
        }

        if (images != null) {
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file, "publications", savedPub.getId().toString());
                    Image img = new Image();
                    img.setUrl(fileName);
                    img.setPublication(savedPub);
                    imageRepository.save(img);
                }
            }
        }
        savedPub.setPendingRequestsCount(0L);
        return savedPub;
    }

    public List<Publication> findByStatusOrderByCreatedAtDesc(PublicationStatus status){
        List<Publication> publications = publicationRepository.findByStatusOrderByCreatedAtDesc(status);
        enrichPendingRequestCount(publications);
        return publications;
    }

    public List<Publication> findByAuthorIdOrderByCreatedAtDesc(Long authorId) {
        List<Publication> publications = publicationRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        enrichPendingRequestCount(publications);
        return publications;
    }

    @Transactional
    public Publication updatePublication(Long publicationId, Long authorId, PublicationUpdateRequestDto request, MultipartFile[] images) throws Exception {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));
        ensurePublicationAuthor(publication, authorId);

        if (request.getPetIds() == null || request.getPetIds().isEmpty()) {
            throw new IllegalArgumentException("La publicacion debe incluir al menos una mascota");
        }

        List<Pet> selectedPets = petRepository.findAllById(request.getPetIds());
        if (selectedPets.size() != request.getPetIds().size()) {
            throw new IllegalArgumentException("Hay mascotas seleccionadas que no existen");
        }

        for (Pet pet : selectedPets) {
            if (pet.getOwner() == null || !authorId.equals(pet.getOwner().getId())) {
                throw new IllegalArgumentException("Solo puedes publicar mascotas que te pertenecen");
            }
        }

        Set<Long> previousPetIds = publication.getPets() == null
                ? new HashSet<>()
                : publication.getPets().stream().map(Pet::getId).collect(Collectors.toSet());
        Set<Long> selectedPetIds = selectedPets.stream().map(Pet::getId).collect(Collectors.toSet());
        Set<Long> newlyAddedPetIds = new HashSet<>(selectedPetIds);
        newlyAddedPetIds.removeAll(previousPetIds);

        publication.setPets(selectedPets);

        if (StringUtils.hasText(request.getTitle())) {
            publication.setTitle(request.getTitle().trim());
        } else {
            String generatedTitle = selectedPets.stream().map(Pet::getName).collect(Collectors.joining(", "));
            publication.setTitle(generatedTitle);
        }
        publication.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());

        if (request.getAdoptionStatus() != null) {
            publication.setAdoptionStatus(request.getAdoptionStatus());
            if (request.getAdoptionStatus() == AdoptionStatus.ADOPTED) {
                publication.setStatus(PublicationStatus.CLOSED);
            } else if (publication.getStatus() == PublicationStatus.CLOSED) {
                publication.setStatus(PublicationStatus.OPEN);
            }
        }

        List<Image> publicationImages = imageRepository.findByPublicationId(publicationId);
        Set<Long> removableIds = request.getRemovedImageIds() == null
                ? new HashSet<>()
                : new HashSet<>(request.getRemovedImageIds());

        if (!removableIds.isEmpty()) {
            List<Image> removableImages = publicationImages.stream()
                    .filter(img -> img.getId() != null && removableIds.contains(img.getId()))
                    .toList();
            imageRepository.deleteAll(removableImages);
            publicationImages = publicationImages.stream()
                    .filter(img -> img.getId() == null || !removableIds.contains(img.getId()))
                    .toList();
        }

        Set<String> currentUrls = publicationImages.stream()
                .map(Image::getUrl)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        for (Pet pet : selectedPets) {
            if (!newlyAddedPetIds.contains(pet.getId()) || pet.getImages() == null) {
                continue;
            }
            for (Image petImage : pet.getImages()) {
                if (!StringUtils.hasText(petImage.getUrl()) || !currentUrls.add(petImage.getUrl())) {
                    continue;
                }
                Image img = new Image();
                img.setUrl(petImage.getUrl());
                img.setPublication(publication);
                imageRepository.save(img);
            }
        }

        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String fileName = fileStorageService.storeFile(file, "publications", publication.getId().toString());
                if (!currentUrls.add(fileName)) {
                    continue;
                }
                Image img = new Image();
                img.setUrl(fileName);
                img.setPublication(publication);
                imageRepository.save(img);
            }
        }

        Publication saved = publicationRepository.save(publication);
        saved.setPendingRequestsCount(adoptionRequestRepository.countByPublicationIdAndStatus(saved.getId(), RequestStatus.PENDING));
        return saved;
    }

    @Transactional
    public AdoptionRequest createAdoptionRequest(Long publicationId, Long applicantId, String message) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));

        if (publication.getStatus() != PublicationStatus.OPEN || publication.getAdoptionStatus() == AdoptionStatus.ADOPTED) {
            throw new IllegalArgumentException("La publicacion ya no acepta solicitudes");
        }

        if (publication.getAuthor() != null && publication.getAuthor().getId().equals(applicantId)) {
            throw new IllegalArgumentException("No puedes solicitar adopcion en tu propia publicacion");
        }

        if (adoptionRequestRepository.existsByPublicationIdAndApplicantIdAndStatus(publicationId, applicantId, RequestStatus.PENDING)) {
            throw new IllegalArgumentException("Ya tienes una solicitud pendiente para esta publicacion");
        }

        User applicant = userRepository.findById(applicantId).orElseThrow();

        AdoptionRequest adoptionRequest = new AdoptionRequest();
        adoptionRequest.setPublication(publication);
        adoptionRequest.setApplicant(applicant);
        adoptionRequest.setMessage(StringUtils.hasText(message) ? message.trim() : "");
        adoptionRequest.setCreatedAt(LocalDateTime.now());
        adoptionRequest.setStatus(RequestStatus.PENDING);

        AdoptionRequest saved = adoptionRequestRepository.save(adoptionRequest);

        if (publication.getAuthor() != null && publication.getAuthor().getId() != null) {
            String applicantName = (applicant.getFirstName() == null ? "" : applicant.getFirstName()).trim();
            String title = "Nueva solicitud de adopcion";
                String notificationMessage = (applicantName.isBlank() ? "Un usuario" : applicantName)
                + " ha enviado una solicitud para tu publicacion '"
                + publication.getTitle()
                + "'.";

            notificationApplicationService.createNotification(
                publication.getAuthor().getId(),
                NotificationType.ADOPTION_REQUEST_RECEIVED,
                title,
                    notificationMessage,
                publicationId,
                saved.getId());
        }

        return saved;
    }

    public List<AdoptionRequest> getPublicationRequests(Long publicationId, Long authorId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));
        ensurePublicationAuthor(publication, authorId);

        List<AdoptionRequest> requests = new ArrayList<>(adoptionRequestRepository.findByPublicationIdOrderByCreatedAtDesc(publicationId));
        requests.sort(Comparator
                .comparingInt((AdoptionRequest req) -> requestStatusWeight(req.getStatus()))
                .thenComparing(AdoptionRequest::getCreatedAt, Comparator.reverseOrder()));
        return requests;
    }

    @Transactional
    public AdoptionRequest changeAdoptionRequestStatus(Long publicationId, Long requestId, Long authorId, RequestStatus status) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));
        ensurePublicationAuthor(publication, authorId);

        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));

        if (request.getPublication() == null || !publicationId.equals(request.getPublication().getId())) {
            throw new IllegalArgumentException("La solicitud no pertenece a esta publicacion");
        }

        request.setStatus(status);
        AdoptionRequest savedRequest = adoptionRequestRepository.save(request);

        if (status == RequestStatus.ACCEPTED) {
            publication.setStatus(PublicationStatus.CLOSED);
            publication.setAdoptionStatus(AdoptionStatus.ADOPTED);
            publicationRepository.save(publication);

            if (savedRequest.getApplicant() != null && savedRequest.getApplicant().getId() != null) {
                notificationApplicationService.createNotification(
                        savedRequest.getApplicant().getId(),
                        NotificationType.ADOPTION_REQUEST_ACCEPTED,
                        "Tu solicitud fue aceptada",
                        "Tu solicitud para la publicacion '" + publication.getTitle() + "' ha sido aceptada.",
                        publicationId,
                        savedRequest.getId());
            }

            List<AdoptionRequest> pendingRequests = adoptionRequestRepository.findByPublicationIdAndStatus(publicationId, RequestStatus.PENDING);
            for (AdoptionRequest pending : pendingRequests) {
                if (!pending.getId().equals(savedRequest.getId())) {
                    pending.setStatus(RequestStatus.ARCHIVED);
                    adoptionRequestRepository.save(pending);
                }
            }
        }

        return savedRequest;
    }

    @Transactional
    public void deleteAdoptionRequest(Long publicationId, Long requestId, Long authorId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));
        ensurePublicationAuthor(publication, authorId);

        AdoptionRequest request = adoptionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));

        if (request.getPublication() == null || !publicationId.equals(request.getPublication().getId())) {
            throw new IllegalArgumentException("La solicitud no pertenece a esta publicacion");
        }

        adoptionRequestRepository.delete(request);
    }

    @Transactional
    public void deletePublication(Long publicationId, Long authorId) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));
        ensurePublicationAuthor(publication, authorId);

        deletePublicationInternal(publicationId, publication);
        }

        @Transactional
        public void deletePublicationAsAdmin(Long publicationId) {
        Publication publication = publicationRepository.findById(publicationId)
            .orElseThrow(() -> new IllegalArgumentException("La publicacion no existe"));

        deletePublicationInternal(publicationId, publication);
        }

        private void deletePublicationInternal(Long publicationId, Publication publication) {

        publication.setPets(new ArrayList<>());
        publicationRepository.save(publication);

        commentRepository.deleteByPublicationId(publicationId);
        adoptionRequestRepository.deleteByPublicationId(publicationId);
        imageRepository.deleteByPublicationId(publicationId);
        publicationRepository.delete(publication);
    }

    private void ensurePublicationAuthor(Publication publication, Long authorId) {
        if (publication.getAuthor() == null || !publication.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("No tienes permisos sobre esta publicacion");
        }
    }

    private int requestStatusWeight(RequestStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case ACCEPTED -> 1;
            case REJECTED -> 2;
            case ARCHIVED -> 3;
        };
    }

    private void enrichPendingRequestCount(List<Publication> publications) {
        for (Publication publication : publications) {
            Long pendingCount = adoptionRequestRepository.countByPublicationIdAndStatus(publication.getId(), RequestStatus.PENDING);
            publication.setPendingRequestsCount(pendingCount);
        }
    }
}