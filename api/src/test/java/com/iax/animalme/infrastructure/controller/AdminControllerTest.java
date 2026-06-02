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

import com.iax.animalme.application.dto.AdminBanUserRequestDto;
import com.iax.animalme.application.dto.AdminNotificationRequestDto;
import com.iax.animalme.application.service.AdminApplicationService;
import com.iax.animalme.domain.model.Pet;
import com.iax.animalme.domain.model.Publication;
import com.iax.animalme.domain.model.User;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminApplicationService adminApplicationService;

    @InjectMocks
    private AdminController controller;

    @Test
    void adminEndpointsReturnExpectedResponses() {
        User user = User.builder().id(2L).build();
        Publication publication = new Publication();
        Pet pet = Pet.builder().id(3L).build();
        AdminBanUserRequestDto banRequest = new AdminBanUserRequestDto();
        AdminNotificationRequestDto notificationRequest = new AdminNotificationRequestDto();

        when(adminApplicationService.listUsers(1L, "q")).thenReturn(List.of(user));
        when(adminApplicationService.banUser(1L, 2L, banRequest)).thenReturn(user);
        when(adminApplicationService.unbanUser(1L, 2L)).thenReturn(user);
        when(adminApplicationService.listPublications(1L, "q")).thenReturn(List.of(publication));
        when(adminApplicationService.listPets(1L, "q")).thenReturn(List.of(pet));
        when(adminApplicationService.sendNotification(1L, notificationRequest)).thenReturn(7);

        assertEquals(HttpStatus.OK, controller.listUsers(1L, "q").getStatusCode());
        assertEquals(HttpStatus.OK, controller.banUser(1L, 2L, banRequest).getStatusCode());
        assertEquals(HttpStatus.OK, controller.unbanUser(1L, 2L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.listPublications(1L, "q").getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deletePublication(1L, 10L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.listPets(1L, "q").getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deletePet(1L, 5L).getStatusCode());

        ResponseEntity<Map<String, Integer>> notificationResponse = controller.sendNotification(1L, notificationRequest);
        assertEquals(7, notificationResponse.getBody().get("recipients"));

        verify(adminApplicationService).deletePublication(1L, 10L);
        verify(adminApplicationService).deletePet(1L, 5L);
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        ResponseEntity<Map<String, String>> response = controller
                .handleIllegalArgument(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad", response.getBody().get("message"));
    }
}
