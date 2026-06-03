package com.iax.animalme.application.dto;

import lombok.Data;

@Data
public class UserPasswordUpdateDto {
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}
