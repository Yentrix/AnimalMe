package com.iax.animalme.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.model.Breed;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findBySpeciesId(Long speciesId);
}
