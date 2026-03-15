package com.iax.animalme.model;

import jakarta.persistence.*;

@Entity
public class Breed {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "species_id")
    private Species species;
}
