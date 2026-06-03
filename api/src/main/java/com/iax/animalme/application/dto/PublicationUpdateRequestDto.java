package com.iax.animalme.application.dto;

import java.util.List;

import com.iax.animalme.domain.enums.AdoptionStatus;

import lombok.Data;

@Data
public class PublicationUpdateRequestDto {
    private String title;
    private String description;
    private AdoptionStatus adoptionStatus;
    private List<Long> petIds;
    private List<Long> removedImageIds;
}
