package com.iax.animalme.domain.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iax.animalme.domain.enums.NotificationType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @JsonProperty("isRead")
    private Boolean isRead = false;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    private Long relatedPublicationId;
    private Long relatedRequestId;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    @JsonIgnoreProperties({"password", "pets", "favoritePublications"})
    private User recipient;
}
