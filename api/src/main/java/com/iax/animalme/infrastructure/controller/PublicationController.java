package com.iax.animalme.infrastructure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.application.dto.PublicationCreateRequestDto;
import com.iax.animalme.application.service.PublicationApplicationService;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.model.Publication;

import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {
    private final PublicationApplicationService publicationService;
    private final ObjectMapper objectMapper;

    public PublicationController(PublicationApplicationService publicationService, ObjectMapper objectMapper) {
        this.publicationService = publicationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Publication> create(
            @RequestPart("publication") String publicationJson,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam("authorId") Long authorId) {
        try {
            PublicationCreateRequestDto request = objectMapper.readValue(publicationJson, PublicationCreateRequestDto.class);
            return ResponseEntity.ok(publicationService.createPublication(request, authorId, images));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public List<Publication> getActive() {
        return publicationService.findByStatusOrderByCreatedAtDesc(PublicationStatus.OPEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgument() {
        return ResponseEntity.badRequest().build();
    }
}
