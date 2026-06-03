package com.iax.animalme.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iax.animalme.domain.model.Breed;
import com.iax.animalme.domain.model.Species;
import com.iax.animalme.domain.repository.BreedRepository;
import com.iax.animalme.domain.repository.SpeciesRepository;

@ExtendWith(MockitoExtension.class)
class SpeciesDomainServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private BreedRepository breedRepository;

    @InjectMocks
    private SpeciesDomainService service;

    @Test
    void normalizeTextRemovesAccentsAndSymbols() {
        assertEquals("arbol perro", service.normalizeText(" Árbol, perro! "));
        assertEquals("", service.normalizeText(null));
    }

    @Test
    void getOrCreateSpeciesReturnsExistingOrCreatesNew() {
        Species existing = new Species();
        existing.setId(1L);
        existing.setName("Dog");

        when(speciesRepository.findByNameIgnoreCase("Dog")).thenReturn(Optional.of(existing));
        assertSame(existing, service.getOrCreateSpecies("Dog"));

        Species created = new Species();
        created.setId(2L);
        created.setName("Cat");

        when(speciesRepository.findByNameIgnoreCase("Cat")).thenReturn(Optional.empty());
        when(speciesRepository.save(any(Species.class))).thenReturn(created);

        Species result = service.getOrCreateSpecies("Cat");
        assertEquals(2L, result.getId());
        assertEquals("Cat", result.getName());
    }

    @Test
    void getOrCreateBreedReturnsExistingOrCreatesNew() {
        Species species = new Species();
        species.setId(9L);

        Breed existing = new Breed();
        existing.setId(1L);
        existing.setName("Beagle");

        when(breedRepository.findByNameAndSpeciesId("Beagle", 9L)).thenReturn(Optional.of(existing));
        assertSame(existing, service.getOrCreateBreed("Beagle", species));

        Breed created = new Breed();
        created.setId(2L);
        created.setName("Mixed");
        when(breedRepository.findByNameAndSpeciesId("Mixed", 9L)).thenReturn(Optional.empty());
        when(breedRepository.save(any(Breed.class))).thenReturn(created);

        Breed result = service.getOrCreateBreed("Mixed", species);
        assertEquals("Mixed", result.getName());
        verify(breedRepository).save(any(Breed.class));
    }
}
