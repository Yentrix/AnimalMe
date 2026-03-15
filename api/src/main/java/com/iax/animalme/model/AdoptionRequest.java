package com.iax.animalme.model;

import com.iax.animalme.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "adoption_requests")
public class AdoptionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "adopter_id")
    private User adopter;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    private String message;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

     @CreationTimestamp
     private LocalDateTime requestDate;
}
