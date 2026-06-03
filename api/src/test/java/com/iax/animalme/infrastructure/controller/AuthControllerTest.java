package com.iax.animalme.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.iax.animalme.application.dto.LoginRequestDto;
import com.iax.animalme.application.service.AuthApplicationService;
import com.iax.animalme.domain.model.User;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthApplicationService authApplicationService;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginAndRegisterReturnOk() {
        LoginRequestDto loginRequest = new LoginRequestDto("mail@test.com", "pwd");
        User user = User.builder().id(1L).email("mail@test.com").build();

        when(authApplicationService.login(loginRequest)).thenReturn(user);
        when(authApplicationService.registerClient(user)).thenReturn(user);

        ResponseEntity<?> loginResponse = controller.login(loginRequest);
        ResponseEntity<?> registerResponse = controller.register(user);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertEquals(user, loginResponse.getBody());
        assertEquals(user, registerResponse.getBody());
    }

    @Test
    void handlersReturnBadRequestWithMessage() {
        ResponseEntity<Map<String, String>> response1 = controller
                .handleIllegalArgument(new IllegalArgumentException("bad"));
        ResponseEntity<Map<String, String>> response2 = controller
                .handleRuntime(new RuntimeException("runtime"));

        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals("bad", response1.getBody().get("message"));
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertEquals("runtime", response2.getBody().get("message"));
        assertNotNull(response2.getBody());
    }
}
