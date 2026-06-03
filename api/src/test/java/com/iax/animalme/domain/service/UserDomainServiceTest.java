package com.iax.animalme.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.service.FileStorageService;
import com.iax.animalme.infrastructure.utilities.ErrorConstants;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserDomainService service;

    @Test
    void validateUniqueEmailAndLoginValidation() {
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(User.builder().id(1L).build()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateUniqueEmail("a@test.com"));
        assertEquals(ErrorConstants.USER_EMAIL_ALREADY_EXISTS, ex.getMessage());

        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> service.validateLogin(Optional.empty()));
        assertEquals(ErrorConstants.USER_LOGIN_INVALID, ex2.getMessage());
    }

    @Test
    void validatePasswordRules() {
        assertEquals(ErrorConstants.USER_PASSWORD_EMPTY,
                assertThrows(IllegalArgumentException.class, () -> service.validatePassword(" ")).getMessage());
        assertEquals(ErrorConstants.USER_PASSWORD_TOO_SHORT,
                assertThrows(IllegalArgumentException.class, () -> service.validatePassword("1234567")).getMessage());
    }

    @Test
    void passwordHelpersUseEncoder() {
        User user = User.builder().password("enc").build();
        when(passwordEncoder.matches("raw", "enc")).thenReturn(true);

        assertTrue(service.validatePasswordUser(user, "raw"));
        assertTrue(service.verifyEmptyPassword("  "));
        assertFalse(service.verifyPasswordDifference("raw", "enc"));
        assertTrue(service.verifyPasswordSize("12345678"));
    }

    @Test
    void saveImageReturnsNullOrStoredUrl() throws Exception {
        assertEquals(null, service.saveImage(null, 1L));

        MockMultipartFile file = new MockMultipartFile("image", "pic.png", "image/png", "x".getBytes());
        when(fileStorageService.storeFile(file, "users", "1")).thenReturn("http://img");

        String url = service.saveImage(file, 1L);
        assertEquals("http://img", url);
    }

    @Test
    void updateDataUserAndUpdateUserKeepOldImageWhenNoNewFile() throws Exception {
        User original = User.builder()
                .id(1L)
                .profilePictureUrl("old-url")
                .firstName("Old")
                .lastName("Last")
                .email("old@test.com")
                .build();

        User newData = User.builder()
                .firstName("New")
                .lastName("User")
                .email("new@test.com")
                .contactPhone("111")
                .contactEmail("new-contact@test.com")
                .country("ES")
                .city("Valencia")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(original));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = service.updateUser(1L, newData, null);

        assertEquals("New", updated.getFirstName());
        assertEquals("old-url", updated.getProfilePictureUrl());
        assertEquals("Valencia", updated.getCity());
        assertNotNull(updated);
    }

    @Test
    void updatePasswordRejectsInvalidAndSavesEncodedWhenValid() {
        User user = User.builder().id(1L).password("enc").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("enc", "enc")).thenReturn(true);

        assertEquals(ErrorConstants.USER_PASSWORD_SAME_AS_OLD,
                assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, "enc")).getMessage());

        when(passwordEncoder.matches("new-password", "enc")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");

        service.updatePassword(1L, "new-password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
    }
}
