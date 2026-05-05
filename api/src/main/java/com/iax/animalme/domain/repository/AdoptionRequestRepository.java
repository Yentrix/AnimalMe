package com.iax.animalme.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.model.AdoptionRequest;

@Repository
public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, Long> {
    List<AdoptionRequest> findByPublicationId(Long publicationId);
    List<AdoptionRequest> findByApplicantId(Long applicantId);
}
