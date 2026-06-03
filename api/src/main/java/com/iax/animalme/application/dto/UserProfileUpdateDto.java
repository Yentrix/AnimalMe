package com.iax.animalme.application.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String firstName;
    private String email;
    private String contactEmail;
    private String contactPhone;
}
