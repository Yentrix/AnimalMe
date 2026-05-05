package com.iax.animalme.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.model.Publication;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {
    List<Publication> findByStatusOrderByCreatedAtDesc(PublicationStatus status);
    
    List<Publication> findByAuthorId(Long authorId);
}