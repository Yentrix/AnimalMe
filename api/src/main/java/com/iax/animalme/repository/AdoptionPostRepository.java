package com.iax.animalme.repository;

import com.iax.animalme.model.AdoptionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdoptionPostRepository  extends JpaRepository<AdoptionPost, Long> {
}
