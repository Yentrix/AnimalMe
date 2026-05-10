package com.iax.animalme.infrastructure.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.application.dto.AdoptionRequestCreateDto;
import com.iax.animalme.application.dto.PublicationCreateRequestDto;
import com.iax.animalme.application.dto.PublicationUpdateRequestDto;
import com.iax.animalme.application.service.PublicationApplicationService;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.enums.RequestStatus;
import com.iax.animalme.domain.model.AdoptionRequest;
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

    @GetMapping("/author/{authorId}")
    public List<Publication> getByAuthor(@PathVariable Long authorId) {
        return publicationService.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    @PutMapping("/{publicationId}")
    public ResponseEntity<Publication> updatePublication(
            @PathVariable Long publicationId,
            @RequestParam("authorId") Long authorId,
            @RequestBody PublicationUpdateRequestDto request) {
        return ResponseEntity.ok(publicationService.updatePublication(publicationId, authorId, request));
    }

    @PostMapping("/{publicationId}/adoption-requests")
    public ResponseEntity<AdoptionRequest> createAdoptionRequest(
            @PathVariable Long publicationId,
            @RequestParam("applicantId") Long applicantId,
            @RequestBody AdoptionRequestCreateDto request) {
        return ResponseEntity.ok(publicationService.createAdoptionRequest(publicationId, applicantId, request.getMessage()));
    }

    @GetMapping("/{publicationId}/adoption-requests")
    public ResponseEntity<List<AdoptionRequest>> getAdoptionRequests(
            @PathVariable Long publicationId,
            @RequestParam("authorId") Long authorId) {
        return ResponseEntity.ok(publicationService.getPublicationRequests(publicationId, authorId));
    }

    @PutMapping("/{publicationId}/adoption-requests/{requestId}")
    public ResponseEntity<AdoptionRequest> updateAdoptionRequestStatus(
            @PathVariable Long publicationId,
            @PathVariable Long requestId,
            @RequestParam("authorId") Long authorId,
            @RequestParam("status") RequestStatus status) {
        return ResponseEntity.ok(publicationService.changeAdoptionRequestStatus(publicationId, requestId, authorId, status));
    }

    @DeleteMapping("/{publicationId}/adoption-requests/{requestId}")
    public ResponseEntity<Void> deleteAdoptionRequest(
            @PathVariable Long publicationId,
            @PathVariable Long requestId,
            @RequestParam("authorId") Long authorId) {
        publicationService.deleteAdoptionRequest(publicationId, requestId, authorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{publicationId}")
    public ResponseEntity<Void> deletePublication(
            @PathVariable Long publicationId,
            @RequestParam("authorId") Long authorId) {
        publicationService.deletePublication(publicationId, authorId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgument() {
        return ResponseEntity.badRequest().build();
    }
}
