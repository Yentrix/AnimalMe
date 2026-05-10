package com.iax.animalme.application.dto;

import lombok.Data;

@Data
public class AdminBanUserRequestDto {
    private String mode;
    private Integer days;
    private Integer hours;
    private Integer minutes;
}
