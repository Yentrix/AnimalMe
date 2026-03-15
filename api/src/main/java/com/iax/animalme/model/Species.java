package com.iax.animalme.model;

import jakarta.persistence.*;

@Entity
public class Species {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
}
