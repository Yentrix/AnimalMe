package com.iax.animalme.application.dto;

import java.util.List;

import lombok.Data;

@Data
public class AdminNotificationRequestDto {
    private String title;
    private String message;
    private Boolean sendToAll;
    private List<Long> userIds;
}
