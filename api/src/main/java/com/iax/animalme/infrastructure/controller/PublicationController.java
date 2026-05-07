package com.iax.animalme.infrastructure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.application.service.PublicationApplicationService;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.model.Publication;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {
    private final PublicationApplicationService publicationService;

    public PublicationController(PublicationApplicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Publication> create(
            @RequestPart("publication") Publication publication,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam("authorId") Long authorId) {
        try {
            return ResponseEntity.ok(publicationService.createPublication(publication, authorId, images));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public List<Publication> getActive() {
        return publicationService.findByStatusOrderByCreatedAtDesc(PublicationStatus.OPEN);
    }
}
