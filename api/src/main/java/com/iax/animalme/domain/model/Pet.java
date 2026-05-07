package com.iax.animalme.domain.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.iax.animalme.domain.enums.AdoptionStatus;

import jakarta.persistence.Column;
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "pets")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer age;
    private String sex;
    private Integer sizeCm;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdoptionStatus adoptionStatus;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"pets", "password", "publications", "favoritePublications"}) // Limpieza profunda del dueño [cite: 26]
    private User owner;

    @ManyToOne
    @JoinColumn(name = "species_id")
    private Species species;

    @ManyToOne
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @OneToMany(mappedBy = "pet")
    @JsonIgnoreProperties("pet") // Evita que la imagen intente cargar de nuevo a la mascota [cite: 29]
    private List<Image> images;
}