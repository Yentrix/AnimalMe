package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iax.animalme.application.dto.LoginRequestDto;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.enums.UserStatus;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.domain.service.UserDomainService;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthApplicationService service;

    @Test
    void loginThrowsWhenCredentialsAreInvalid() {
        LoginRequestDto request = new LoginRequestDto("mail@test.com", "bad");
        when(userRepository.findByEmail("mail@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.login(request));
        assertEquals("Incorrect email or password", ex.getMessage());
    }

    @Test
    void loginThrowsWhenUserIsPermanentlyBanned() {
        User user = User.builder().id(10L).status(UserStatus.BANNED_PERMANENT).password("enc").build();
        LoginRequestDto request = new LoginRequestDto("mail@test.com", "ok");

        when(userRepository.findByEmail("mail@test.com")).thenReturn(Optional.of(user));
        when(userDomainService.validatePasswordUser(user, "ok")).thenReturn(true);
        when(userDomainService.validateLogin(any())).thenReturn(user);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.login(request));
        assertEquals("Tu cuenta ha sido baneada permanentemente", ex.getMessage());
    }

    @Test
    void loginThrowsWhenUserIsTemporarilyBannedAndStillActive() {
        User user = User.builder()
                .id(10L)
                .status(UserStatus.BANNED_TEMPORARY)
                .bannedUntil(LocalDateTime.now().plusHours(2).plusMinutes(5))
                .password("enc")
                .build();
        LoginRequestDto request = new LoginRequestDto("mail@test.com", "ok");

        when(userRepository.findByEmail("mail@test.com")).thenReturn(Optional.of(user));
        when(userDomainService.validatePasswordUser(user, "ok")).thenReturn(true);
        when(userDomainService.validateLogin(any())).thenReturn(user);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.login(request));
        assertTrue(ex.getMessage().contains("Tiempo restante"));
    }

    @Test
    void loginUnbansTemporaryUserWhenBanHasExpired() {
        User user = User.builder()
                .id(10L)
                .status(UserStatus.BANNED_TEMPORARY)
                .bannedUntil(LocalDateTime.now().minusMinutes(1))
                .password("enc")
                .build();
        LoginRequestDto request = new LoginRequestDto("mail@test.com", "ok");

        when(userRepository.findByEmail("mail@test.com")).thenReturn(Optional.of(user));
        when(userDomainService.validatePasswordUser(user, "ok")).thenReturn(true);
        when(userDomainService.validateLogin(any())).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        User logged = service.login(request);

        assertNotNull(logged);
        assertEquals(UserStatus.ACTIVE, logged.getStatus());
        assertNull(logged.getBannedUntil());
        verify(userRepository).save(user);
    }

    @Test
    void registerClientSetsDefaultsEncodesPasswordAndSaves() {
        User input = User.builder()
                .email("new@test.com")
                .password("plainpass")
                .firstName("A")
                .build();

        when(passwordEncoder.encode("plainpass")).thenReturn("encoded");
        when(userRepository.save(input)).thenReturn(input);

        User saved = service.registerClient(input);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDomainService).validateUniqueEmail("new@test.com");
        verify(userDomainService).validatePassword("plainpass");
        verify(userRepository).save(captor.capture());

        User toSave = captor.getValue();
        assertEquals(UserRole.USER, toSave.getRole());
        assertEquals(UserStatus.ACTIVE, toSave.getStatus());
        assertEquals("new@test.com", toSave.getContactEmail());
        assertEquals("encoded", toSave.getPassword());
        assertNotNull(saved);
    }
}
