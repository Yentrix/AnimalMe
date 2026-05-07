package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.model.Image;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.ImageRepository;
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

    public PublicationApplicationService(PublicationRepository publicationRepository, ImageRepository imageRepository,
            FileStorageService fileStorageService, UserRepository userRepository) {
        this.publicationRepository = publicationRepository;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Publication createPublication(Publication pub, Long authorId, MultipartFile[] images) throws Exception {
        User author = userRepository.findById(authorId).orElseThrow();
        pub.setAuthor(author);
        pub.setCreatedAt(LocalDateTime.now());

        Publication savedPub = publicationRepository.save(pub);

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