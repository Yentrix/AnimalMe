package com.iax.animalme.controller;

import com.iax.animalme.model.AdoptionPost;
import com.iax.animalme.model.AdoptionRequest;
import com.iax.animalme.model.Pet;
import com.iax.animalme.service.AdoptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adoptions")
public class AdoptionController {

    @Autowired
    private AdoptionService adoptionService;

    // GET /api/adoptions/posts
    @GetMapping("/posts")
    public List<AdoptionPost> listAllAdoptions() {
        return adoptionService.getAllAdoptionPosts();
    }

    // GET /api/adoptions/request/pet/{id}
    @GetMapping("request/pet/{id}")
    public List<AdoptionRequest> listAllRequestFormPetId(@PathVariable Long id) {
        return adoptionService.getAllRequestFromPetId(id);
    }

    // POST /api/adoptions/request
    @PostMapping("/requests")
    public ResponseEntity<AdoptionRequest> submitRequest(@RequestBody AdoptionRequest request) {
        return ResponseEntity.ok(adoptionService.applyForAdoption(request));
    }

    // PUT /api/adoptions/request/{id}/approve
    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        adoptionService.approveAdoption(id);
        return ResponseEntity.ok("Adoption approved successfully!");
    }

}
