package com.iax.animalme.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.model.Species;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Long> {
}
