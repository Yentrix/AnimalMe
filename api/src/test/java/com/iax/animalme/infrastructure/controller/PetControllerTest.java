package com.iax.animalme.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.iax.animalme.application.service.PetApplicationService;
import com.iax.animalme.domain.model.Breed;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Species;
import com.iax.animalme.domain.service.SpeciesDomainService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {

    @Mock
    private PetApplicationService petApplicationService;

    @Mock
    private SpeciesDomainService speciesDomainService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PetController controller;

    @Test
    void createAndUpdatePetReturnOkWhenServicesWork() throws Exception {
        String json = "{\"name\":\"Luna\"}";
        MockMultipartFile image = new MockMultipartFile("image", "pet.jpg", "image/jpeg", "img".getBytes());

        Species species = new Species();
        species.setId(1L);
        Breed breed = new Breed();
        breed.setId(2L);

        Pet pet = Pet.builder().id(9L).name("Luna").build();

        when(speciesDomainService.getOrCreateSpecies("Perro")).thenReturn(species);
        when(speciesDomainService.getOrCreateBreed("Mestizo", species)).thenReturn(breed);
        when(objectMapper.readValue(json, Pet.class)).thenReturn(pet);
        when(petApplicationService.createPet(pet, 1L, image)).thenReturn(pet);
        when(petApplicationService.updatePet(9L, pet, image)).thenReturn(pet);

        ResponseEntity<Pet> create = controller.createPet(json, image, "Perro", "Mestizo", 1L);
        ResponseEntity<Pet> update = controller.updatePet(9L, json, image, "Perro", "Mestizo");

        assertEquals(HttpStatus.OK, create.getStatusCode());
        assertEquals(HttpStatus.OK, update.getStatusCode());
    }

    @Test
    void createAndUpdatePetReturnInternalServerErrorOnException() throws Exception {
        String json = "{}";

        when(speciesDomainService.getOrCreateSpecies("Perro")).thenThrow(new RuntimeException("boom"));
        ResponseEntity<Pet> create = controller.createPet(json, null, "Perro", "Mestizo", 1L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, create.getStatusCode());

        when(speciesDomainService.getOrCreateSpecies("Gato")).thenThrow(new RuntimeException("boom"));
        ResponseEntity<Pet> update = controller.updatePet(1L, json, null, "Gato", "Comuna");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, update.getStatusCode());
    }

    @Test
    void getPetsByOwnerReturnsOkOrInternalServerError() {
        when(petApplicationService.getPetsByOwner(1L)).thenReturn(List.of(Pet.builder().id(1L).build()));
        assertEquals(HttpStatus.OK, controller.getPetsByOwner(1L).getStatusCode());

        when(petApplicationService.getPetsByOwner(2L)).thenThrow(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.getPetsByOwner(2L).getStatusCode());
    }
}
