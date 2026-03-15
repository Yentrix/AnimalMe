package com.iax.animalme.repository;

import com.iax.animalme.model.AdoptionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdoptionRequestRepository  extends JpaRepository<AdoptionRequest, Long> {
    List<AdoptionRequest> findByPetId(Long pet);
}
