package com.iax.animalme.service;

import com.iax.animalme.enums.PetStatus;
import com.iax.animalme.model.Client;
import com.iax.animalme.model.Pet;
import com.iax.animalme.repository.PetRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PetService {
    private PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    public Pet createPet(Pet pet) {
        pet.setStatus(PetStatus.AVAILABLE);
        return petRepository.save(pet);
    }

    public Optional<Pet> getPetById(@PathVariable("id") Long id) {
        return petRepository.findById(id);
    }

    public List<Pet> getAllPets() {
        return petRepository.findAll();
    }

    public List<Pet> getAviablePets() {
        return petRepository.findByStatus(PetStatus.AVAILABLE);
    }

    public Pet updatePet(Long id,Pet data) {
        // Check pet exists
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));

        pet.setName(data.getName());
        pet.setDescription(data.getDescription());
        pet.setSpecies(data.getSpecies());
        pet.setBreed(data.getBreed());
        pet.setBirthDate(data.getBirthDate());
        pet.setVaccinations(data.getVaccinations());
        pet.setStatus(data.getStatus());
        pet.setOwner(data.getOwner());
        pet.setAdoptionPost(data.getAdoptionPost());

        return petRepository.save(pet);
    }

    public void deletePet(@PathVariable("id") Long id) {
        petRepository.deleteById(id);
    }
}
