package com.iax.animalme.model;

import com.iax.animalme.enums.PetStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "pets")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne
    private Species species;

    @ManyToOne
    private Breed breed;

    private LocalDate birthDate;
    private String vaccinations; // Could also be a List<String> with @ElementCollection

    @Enumerated(EnumType.STRING)
    private PetStatus status;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client owner;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private AdoptionPost adoptionPost;
}
