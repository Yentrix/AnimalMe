package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iax.animalme.domain.enums.NotificationType;
import com.iax.animalme.domain.model.Notification;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.NotificationRepository;
import com.iax.animalme.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationApplicationService service;

    @Test
    void createNotificationRejectsUnknownRecipient() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createNotification(1L, NotificationType.ADMIN_MESSAGE, "t", "m", null, null));
        assertEquals("El usuario destinatario no existe", ex.getMessage());
    }

    @Test
    void createNotificationBuildsNotificationAndSaves() {
        User recipient = User.builder().id(10L).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = service.createNotification(10L, NotificationType.ADMIN_MESSAGE, "Titulo", "Mensaje", 7L, 8L);

        assertEquals("Titulo", saved.getTitle());
        assertEquals("Mensaje", saved.getMessage());
        assertEquals(NotificationType.ADMIN_MESSAGE, saved.getType());
        assertEquals(7L, saved.getRelatedPublicationId());
        assertEquals(8L, saved.getRelatedRequestId());
        assertEquals(Boolean.FALSE, saved.getIsRead());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void listForUserRequiresExistingUser() {
        when(userRepository.existsById(5L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.listForUser(5L));
        assertEquals("El usuario no existe", ex.getMessage());
    }

    @Test
    void markAsReadUpdatesOnlyWhenUnread() {
        when(userRepository.existsById(5L)).thenReturn(true);

        Notification unread = new Notification();
        unread.setId(1L);
        unread.setIsRead(false);

        when(notificationRepository.findByIdAndRecipientId(1L, 5L)).thenReturn(Optional.of(unread));
        when(notificationRepository.save(unread)).thenReturn(unread);

        Notification result = service.markAsRead(1L, 5L);
        assertEquals(Boolean.TRUE, result.getIsRead());
        assertNotNull(result.getReadAt());

        Notification alreadyRead = new Notification();
        alreadyRead.setId(2L);
        alreadyRead.setIsRead(true);

        when(notificationRepository.findByIdAndRecipientId(2L, 5L)).thenReturn(Optional.of(alreadyRead));
        Notification untouched = service.markAsRead(2L, 5L);
        assertEquals(Boolean.TRUE, untouched.getIsRead());

        verify(notificationRepository).save(unread);
        verify(notificationRepository, never()).save(alreadyRead);
    }

    @Test
    void markAllAsReadMarksBatchUsingSingleTimestamp() {
        when(userRepository.existsById(9L)).thenReturn(true);

        Notification n1 = new Notification();
        n1.setIsRead(false);
        Notification n2 = new Notification();
        n2.setIsRead(false);

        when(notificationRepository.findByRecipientIdAndIsReadFalse(9L)).thenReturn(List.of(n1, n2));

        service.markAllAsRead(9L);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();

        assertEquals(2, saved.size());
        assertEquals(Boolean.TRUE, saved.get(0).getIsRead());
        assertEquals(Boolean.TRUE, saved.get(1).getIsRead());
        assertEquals(saved.get(0).getReadAt(), saved.get(1).getReadAt());
    }
}
