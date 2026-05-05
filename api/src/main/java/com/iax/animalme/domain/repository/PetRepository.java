package com.iax.animalme.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.enums.AdoptionStatus;
import com.iax.animalme.domain.model.Pet;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findBySpeciesId(Long speciesId);
    List<Pet> findByBreedId(Long breedId);
    List<Pet> findByAdoptionStatus(AdoptionStatus status);
    
    List<Pet> findByOwnerId(Long ownerId);
}
