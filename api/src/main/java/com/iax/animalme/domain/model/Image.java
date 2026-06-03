package com.iax.animalme.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    @JsonIgnore // La imagen no necesita devolver el objeto Pet completo [cite: 17]
    private Pet pet;

    @ManyToOne
    @JoinColumn(name = "publication_id")
    @JsonIgnore // La imagen no necesita devolver la Publicación completa [cite: 18]
    private Publication publication;
}
