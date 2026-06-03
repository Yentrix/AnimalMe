package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.iax.animalme.domain.enums.AdoptionStatus;
import com.iax.animalme.domain.model.Image;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.ImageRepository;
import com.iax.animalme.domain.repository.PetRepository;
import com.iax.animalme.infrastructure.service.FileStorageService;

@ExtendWith(MockitoExtension.class)
class PetApplicationServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserApplicationService userApplicationService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PetApplicationService service;

    @Test
    void createPetSetsOwnerAndDefaultStatusAndStoresImage() throws Exception {
        User owner = User.builder().id(9L).build();
        Pet pet = Pet.builder().name("Luna").build();
        Pet savedPet = Pet.builder().id(10L).name("Luna").build();
        MockMultipartFile image = new MockMultipartFile("image", "pet.jpg", "image/jpeg", "img".getBytes());

        when(userApplicationService.findById(9L)).thenReturn(owner);
        when(petRepository.save(pet)).thenReturn(savedPet);
        when(fileStorageService.storeFile(image, "pets", "10")).thenReturn("http://u");

        Pet result = service.createPet(pet, 9L, image);

        assertEquals(AdoptionStatus.AVAILABLE, pet.getAdoptionStatus());
        assertEquals(owner, pet.getOwner());
        assertEquals(10L, result.getId());
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void updatePetThrowsWhenPetDoesNotExist() {
        when(petRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> service.updatePet(1L, Pet.builder().name("X").build(), null));
        assertEquals("Mascota no encontrada", ex.getMessage());
    }

    @Test
    void updatePetKeepsOldStatusWhenMissingAndAddsImageWhenProvided() throws Exception {
        Pet oldPet = Pet.builder()
                .id(1L)
                .name("Old")
                .adoptionStatus(AdoptionStatus.AVAILABLE)
                .owner(User.builder().id(99L).build())
                .build();

        Pet details = Pet.builder().name("NewName").adoptionStatus(null).build();
        MockMultipartFile image = new MockMultipartFile("image", "pet.jpg", "image/jpeg", "img".getBytes());

        when(petRepository.findById(1L)).thenReturn(Optional.of(oldPet));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStorageService.storeFile(image, "pets", "1")).thenReturn("http://new-image");

        Pet saved = service.updatePet(1L, details, image);

        assertEquals("NewName", saved.getName());
        assertEquals(AdoptionStatus.AVAILABLE, saved.getAdoptionStatus());
        assertEquals(99L, saved.getOwner().getId());
        verify(imageRepository).save(any(Image.class));
    }
}
