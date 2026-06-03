package com.iax.animalme.domain.service;

import java.text.Normalizer;

import org.springframework.stereotype.Service;

import com.iax.animalme.domain.model.Breed;
import com.iax.animalme.domain.model.Species;
import com.iax.animalme.domain.repository.BreedRepository;
import com.iax.animalme.domain.repository.SpeciesRepository;

@Service
public class SpeciesDomainService {
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;

    public SpeciesDomainService(SpeciesRepository speciesRepository, BreedRepository breedRepository) {
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
    }

    public String normalizeText(String text) {
        if (text == null)
            return "";
        // Quitar acentos usando Normalizer
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        // Quitar puntos, comas y símbolos, pasar a minúsculas
        return normalized.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().trim();
    }

    public Species getOrCreateSpecies(String name) {
        return speciesRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Species s = new Species();
                    s.setName(name);
                    return speciesRepository.save(s);
                });
    }

    public Breed getOrCreateBreed(String breedName, Species species) {
        return breedRepository.findByNameAndSpeciesId(breedName, species.getId())
                .orElseGet(() -> {
                    Breed b = new Breed();
                    b.setName(breedName);
                    b.setSpecies(species);
                    return breedRepository.save(b);
                });
    }
}
