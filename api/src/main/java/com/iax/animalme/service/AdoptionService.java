package com.iax.animalme.service;

import com.iax.animalme.enums.PetStatus;
import com.iax.animalme.enums.RequestStatus;
import com.iax.animalme.model.AdoptionPost;
import com.iax.animalme.model.AdoptionRequest;
import com.iax.animalme.model.Pet;
import com.iax.animalme.repository.AdoptionPostRepository;
import com.iax.animalme.repository.AdoptionRequestRepository;
import com.iax.animalme.repository.PetRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
@Transactional
public class AdoptionService {
    private AdoptionRequestRepository requestRepository;
    private AdoptionPostRepository postRepository;
    private PetRepository petRepository;

    public AdoptionService(AdoptionRequestRepository requestRepository,
                           AdoptionPostRepository postRepository,
                           PetRepository petRepository) {
        this.requestRepository = requestRepository;
        this.postRepository = postRepository;
        this.petRepository = petRepository;
    }

    public AdoptionRequest applyForAdoption(AdoptionRequest request) {
        return requestRepository.save(request);
    }

    public void approveAdoption(Long requestId) {
        AdoptionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Mark request as approved
        request.setStatus(RequestStatus.APPROVED);

        // Update pet status to adopted
        Pet pet = request.getPet();
        pet.setStatus(PetStatus.ADOPTED);

        // save changes
        petRepository.save(pet);
        requestRepository.save(request);

        // TODO: Add a reject all other pending requests
    }

    public List<AdoptionPost> getAllAdoptionPosts() {
        return postRepository.findAll();
    }

    public List<AdoptionRequest> getAllRequestFromPetId(@PathVariable("id") Long id) {
        return requestRepository.findByPetId(id);
    }
}
