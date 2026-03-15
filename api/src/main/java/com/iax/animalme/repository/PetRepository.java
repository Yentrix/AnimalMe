package com.iax.animalme.repository;

import com.iax.animalme.enums.PetStatus;
import com.iax.animalme.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByStatus(PetStatus status);
    List<Pet> findBySpeciesName(String speciesName);
}
