package com.iax.animalme.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iax.animalme.application.dto.UserPasswordUpdateDto;
import com.iax.animalme.application.dto.UserProfileUpdateDto;
import com.iax.animalme.domain.enums.UserRole;
import com.iax.animalme.domain.model.User;
import com.iax.animalme.domain.repository.UserRepository;
import com.iax.animalme.infrastructure.utilities.ErrorConstants;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserApplicationService service;

    @Test
    void listAndSearchDelegatesToRepository() {
        when(userRepository.findAll()).thenReturn(List.of(new User()));
        when(userRepository.findByFirstNameContainingIgnoreCase("ana")).thenReturn(List.of(new User()));
        when(userRepository.findByEmailContainingIgnoreCase("mail")).thenReturn(List.of(new User()));
        when(userRepository.findByRole(UserRole.USER)).thenReturn(List.of(new User()));

        assertEquals(1, service.listUsers().size());
        assertEquals(1, service.listByFirstName("ana").size());
        assertEquals(1, service.listByEmail("mail").size());
        assertEquals(1, service.getUsersByRole(UserRole.USER).size());
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.findById(1L));
        assertEquals(ErrorConstants.USER_NOT_FOUND, ex.getMessage());
    }

    @Test
    void updateProfileRejectsBlankEmail() {
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setEmail("   ");

        User user = User.builder().id(9L).email("old@test.com").build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateProfile(9L, dto));
        assertEquals("El email no puede estar vacio", ex.getMessage());
    }

    @Test
    void updateProfileRejectsEmailAlreadyUsedByOtherUser() {
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setEmail("dup@test.com");

        User target = User.builder().id(10L).email("old@test.com").build();
        User existing = User.builder().id(11L).email("dup@test.com").build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateProfile(10L, dto));
        assertEquals(ErrorConstants.USER_EMAIL_ALREADY_EXISTS, ex.getMessage());
    }

    @Test
    void updateProfileAllowsSameEmailAndTrimsFields() {
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setFirstName("  Ana ");
        dto.setEmail("  me@test.com ");
        dto.setContactEmail("  c@test.com ");
        dto.setContactPhone(" 123 ");

        User target = User.builder().id(10L).email("me@test.com").build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.updateProfile(10L, dto);

        assertEquals("Ana", saved.getFirstName());
        assertEquals("me@test.com", saved.getEmail());
        assertEquals("c@test.com", saved.getContactEmail());
        assertEquals("123", saved.getContactPhone());
    }

    @Test
    void updatePasswordRejectsWhenCurrentPasswordMissing() {
        User user = User.builder().id(1L).password("enc").build();
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto();
        dto.setCurrentPassword("  ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, dto));
        assertEquals("Debes escribir tu contraseña actual", ex.getMessage());
    }

    @Test
    void updatePasswordRejectsWhenCurrentPasswordIsWrong() {
        User user = User.builder().id(1L).password("enc").build();
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto();
        dto.setCurrentPassword("wrong");
        dto.setNewPassword("newpassword");
        dto.setConfirmNewPassword("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "enc")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, dto));
        assertEquals("La contraseña actual no es correcta", ex.getMessage());
    }

    @Test
    void updatePasswordRejectsWeakInvalidOrRepeatedPassword() {
        User user = User.builder().id(1L).password("enc").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-ok", "enc")).thenReturn(true);

        UserPasswordUpdateDto emptyDto = new UserPasswordUpdateDto();
        emptyDto.setCurrentPassword("current-ok");
        emptyDto.setNewPassword(" ");
        emptyDto.setConfirmNewPassword(" ");
        assertEquals(ErrorConstants.USER_PASSWORD_EMPTY,
                assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, emptyDto)).getMessage());

        UserPasswordUpdateDto shortDto = new UserPasswordUpdateDto();
        shortDto.setCurrentPassword("current-ok");
        shortDto.setNewPassword("1234567");
        shortDto.setConfirmNewPassword("1234567");
        assertEquals(ErrorConstants.USER_PASSWORD_TOO_SHORT,
                assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, shortDto)).getMessage());

        UserPasswordUpdateDto mismatch = new UserPasswordUpdateDto();
        mismatch.setCurrentPassword("current-ok");
        mismatch.setNewPassword("12345678");
        mismatch.setConfirmNewPassword("different");
        assertEquals("La confirmacion de la contraseña no coincide",
                assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, mismatch)).getMessage());

        UserPasswordUpdateDto repeated = new UserPasswordUpdateDto();
        repeated.setCurrentPassword("current-ok");
        repeated.setNewPassword("samePassword");
        repeated.setConfirmNewPassword("samePassword");
        when(passwordEncoder.matches("samePassword", "enc")).thenReturn(true);
        assertEquals(ErrorConstants.USER_PASSWORD_SAME_AS_OLD,
                assertThrows(IllegalArgumentException.class, () -> service.updatePassword(1L, repeated)).getMessage());
    }

    @Test
    void updatePasswordEncodesAndSavesWhenDataIsValid() {
        User user = User.builder().id(1L).password("enc").build();
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto();
        dto.setCurrentPassword("current-ok");
        dto.setNewPassword("newPassword8");
        dto.setConfirmNewPassword("newPassword8");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-ok", "enc")).thenReturn(true);
        when(passwordEncoder.matches("newPassword8", "enc")).thenReturn(false);
        when(passwordEncoder.encode("newPassword8")).thenReturn("encoded");

        assertDoesNotThrow(() -> service.updatePassword(1L, dto));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void deleteUserDelegates() {
        service.deleteUser(22L);
        verify(userRepository).deleteById(22L);
        verify(userRepository, never()).save(any());
    }
}
