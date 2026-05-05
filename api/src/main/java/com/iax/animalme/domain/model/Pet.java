package com.iax.animalme.domain.model;

import java.util.List;

import com.iax.animalme.domain.enums.AdoptionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "pets")
@Data
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String sex;
    private Integer sizeCm; // Nullable
    private String description;

    @Enumerated(EnumType.STRING)
    private AdoptionStatus adoptionStatus;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "species_id")
    private Species species;

    @ManyToOne
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @OneToMany(mappedBy = "pet")
    private List<Image> images;
}
