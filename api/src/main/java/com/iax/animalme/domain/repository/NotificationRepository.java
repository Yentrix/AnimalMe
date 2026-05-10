package com.iax.animalme.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iax.animalme.domain.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByIsReadAscCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
}
