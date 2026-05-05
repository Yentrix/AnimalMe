package com.iax.animalme.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.model.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByPetId(Long petId);
    List<Image> findByPublicationId(Long publicationId);
}
