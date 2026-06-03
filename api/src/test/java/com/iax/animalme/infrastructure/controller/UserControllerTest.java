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

import com.iax.animalme.application.dto.UserPasswordUpdateDto;
import com.iax.animalme.application.dto.UserProfileUpdateDto;
import com.iax.animalme.application.service.UserApplicationService;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserApplicationService userApplicationService;

    @InjectMocks
    private UserController controller;

    @Test
    void listByRoleUsesRoleFilterOrFindAll() {
        when(userApplicationService.getUsersByRole(UserRole.USER)).thenReturn(List.of(new User()));
        when(userRepository.findAll()).thenReturn(List.of(new User(), new User()));

        assertEquals(1, controller.listByRole(UserRole.USER).getBody().size());
        assertEquals(2, controller.listByRole(null).getBody().size());
    }

    @Test
    void getUserByIdReturnsNotFoundWhenServiceReturnsNull() {
        when(userApplicationService.findById(1L)).thenReturn(null);
        ResponseEntity<User> response = controller.getUserById(1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void searchAndUpdateEndpointsDelegateAndReturnExpectedStatus() {
        User user = User.builder().id(1L).build();
        UserProfileUpdateDto profile = new UserProfileUpdateDto();
        UserPasswordUpdateDto password = new UserPasswordUpdateDto();

        when(userApplicationService.listByFirstName("ana")).thenReturn(List.of(user));
        when(userApplicationService.listByEmail("mail")).thenReturn(List.of(user));
        when(userApplicationService.updateProfile(1L, profile)).thenReturn(user);

        assertEquals(1, controller.searchByName("ana").size());
        assertEquals(1, controller.searchByEmail("mail").size());
        assertEquals(HttpStatus.OK, controller.updateProfile(1L, profile).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.updatePassword(1L, password).getStatusCode());

        verify(userApplicationService).updatePassword(1L, password);
    }

    @Test
    void handleExceptionReturnsBadRequestWithMessage() {
        ResponseEntity<Map<String, String>> response = controller.handleException(new RuntimeException("boom"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("boom", response.getBody().get("message"));
    }
}
