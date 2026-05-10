package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.application.dto.PublicationCreateRequestDto;
import com.iax.animalme.domain.enums.AdoptionStatus;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.model.Image;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;
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

    public PublicationApplicationService(PublicationRepository publicationRepository, ImageRepository imageRepository,
            FileStorageService fileStorageService, UserRepository userRepository, PetRepository petRepository) {
        this.publicationRepository = publicationRepository;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
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
        return savedPub;
    }

    public List<Publication> findByStatusOrderByCreatedAtDesc(PublicationStatus status){
        return publicationRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}