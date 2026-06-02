package com.iax.animalme.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.iax.animalme.application.service.NotificationApplicationService;
import com.iax.animalme.domain.model.Notification;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationApplicationService notificationApplicationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void endpointsReturnExpectedResponses() {
        Notification notification = new Notification();
        when(notificationApplicationService.listForUser(1L)).thenReturn(List.of(notification));
        when(notificationApplicationService.markAsRead(2L, 1L)).thenReturn(notification);

        assertEquals(1, controller.list(1L).getBody().size());
        assertEquals(HttpStatus.OK, controller.markAsRead(2L, 1L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.markAllAsRead(1L).getStatusCode());

        verify(notificationApplicationService).markAllAsRead(1L);
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        ResponseEntity<Map<String, String>> response = controller
                .handleIllegalArgument(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad", response.getBody().get("message"));
    }
}
