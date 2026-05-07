package com.iax.animalme.infrastructure.controller;

import com.iax.animalme.application.service.PetApplicationService;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.service.SpeciesDomainService;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetApplicationService petService;
    private final SpeciesDomainService speciesService;
    private final ObjectMapper objectMapper;

    public PetController(PetApplicationService petService,
            SpeciesDomainService speciesService,
            ObjectMapper objectMapper) {
        this.petService = petService;
        this.speciesService = speciesService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<Pet> createPet(
            @RequestPart("pet") String petJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam("speciesName") String speciesName,
            @RequestParam("breedName") String breedName,
            @RequestParam("ownerId") Long ownerId) {
        try {
            // 1. Procesar Especie y Raza
            var species = speciesService.getOrCreateSpecies(speciesName);
            var breed = speciesService.getOrCreateBreed(breedName, species);

            // 2. Convertir el JSON String a objeto Pet usando el objectMapper inyectado
            Pet pet = objectMapper.readValue(petJson, Pet.class);
            pet.setSpecies(species);
            pet.setBreed(breed);

            return ResponseEntity.ok(petService.createPet(pet, ownerId, image));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Pet>> getPetsByOwner(@PathVariable Long ownerId) {
        try {
            List<Pet> pets = petService.getPetsByOwner(ownerId);
            return ResponseEntity.ok(pets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pet> updatePet(
            @PathVariable Long id,
            @RequestPart("pet") String petJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam("speciesName") String speciesName,
            @RequestParam("breedName") String breedName) {
        try {
            var species = speciesService.getOrCreateSpecies(speciesName);
            var breed = speciesService.getOrCreateBreed(breedName, species);

            Pet petDetails = objectMapper.readValue(petJson, Pet.class);
            petDetails.setSpecies(species);
            petDetails.setBreed(breed);

            return ResponseEntity.ok(petService.updatePet(id, petDetails, image));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}