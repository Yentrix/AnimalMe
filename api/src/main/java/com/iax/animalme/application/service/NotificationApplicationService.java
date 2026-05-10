package com.iax.animalme.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.iax.animalme.domain.enums.NotificationType;
import com.iax.animalme.domain.model.Notification;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.NotificationRepository;
import com.iax.animalme.domain.repository.UserRepository;

@Service
public class NotificationApplicationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationApplicationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            Long publicationId,
            Long requestId) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario destinatario no existe"));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedPublicationId(publicationId);
        notification.setRelatedRequestId(requestId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    public List<Notification> listForUser(Long userId) {
        ensureUserExists(userId);
        return notificationRepository.findByRecipientIdOrderByIsReadAscCreatedAtDesc(userId);
    }

    public Notification markAsRead(Long notificationId, Long userId) {
        ensureUserExists(userId);

        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("La notificacion no existe para este usuario"));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return notification;
    }

    public void markAllAsRead(Long userId) {
        ensureUserExists(userId);

        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();

        unread.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });

        notificationRepository.saveAll(unread);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("El usuario no existe");
        }
    }
}
