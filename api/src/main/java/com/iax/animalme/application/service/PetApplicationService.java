package com.iax.animalme.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.iax.animalme.domain.enums.AdoptionStatus;
import com.iax.animalme.domain.model.Image;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.ImageRepository;
import com.iax.animalme.domain.repository.PetRepository;
import com.iax.animalme.infrastructure.service.FileStorageService;

@Service
public class PetApplicationService {

    private final PetRepository petRepository;
    private final ImageRepository imageRepository;
    private final UserApplicationService userService;
    private final FileStorageService fileStorageService;

    public PetApplicationService(PetRepository petRepository,
            UserApplicationService userService,
            FileStorageService fileStorageService,
            ImageRepository imageRepository) {
        this.petRepository = petRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.imageRepository = imageRepository;
    }

    public Pet createPet(Pet pet, Long ownerId, MultipartFile imageFile) throws Exception {
        User owner = userService.findById(ownerId);
        pet.setOwner(owner);
        if (pet.getAdoptionStatus() == null) {
            pet.setAdoptionStatus(AdoptionStatus.AVAILABLE);
        }

        Pet savedPet = petRepository.save(pet);

        if (imageFile != null && !imageFile.isEmpty()) {
            // Guardamos físicamente el archivo
            String fileName = fileStorageService.storeFile(imageFile, "pets", savedPet.getId().toString());

            // Creamos la entidad Image y la vinculamos
            Image petImage = new Image();
            petImage.setUrl(fileName); // El path o nombre devuelto por tu FileStorage
            petImage.setPet(savedPet);
            imageRepository.save(petImage);
        }

        return savedPet;
    }

    public List<Pet> getPetsByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }

    public Pet updatePet(Long id, Pet petDetails, MultipartFile image) throws Exception {
        Pet oldPet = petRepository.findById(id)
                .orElseThrow(() -> new Exception("Mascota no encontrada"));

        // Usamos toBuilder para mantener el ID y el Owner originales
        Pet petToSave = oldPet.toBuilder()
                .name(petDetails.getName())
            .age(petDetails.getAge())
                .sex(petDetails.getSex())
                .sizeCm(petDetails.getSizeCm())
                .description(petDetails.getDescription())
                .adoptionStatus(petDetails.getAdoptionStatus() != null ? petDetails.getAdoptionStatus() : oldPet.getAdoptionStatus())
                .species(petDetails.getSpecies())
                .breed(petDetails.getBreed())
                .build();

        // IMPORTANTE: Guardar en la base de datos
        Pet savedPet = petRepository.save(petToSave);

        if (image != null && !image.isEmpty()) {
            String fileName = fileStorageService.storeFile(image, "pets", savedPet.getId().toString());
            Image petImage = new Image();
            petImage.setUrl(fileName);
            petImage.setPet(savedPet);
            imageRepository.save(petImage);
        }

        return savedPet;
    }
}