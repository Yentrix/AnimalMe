package com.iax.animalme.infrastructure.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

class ErrorConstantsTest {

    @Test
    void constantsHaveExpectedValuesAndClassCanBeInstantiatedReflectively() throws Exception {
        assertEquals("El email ya está registrado", ErrorConstants.USER_EMAIL_ALREADY_EXISTS);
        assertEquals("Email o contraseña incorrectos", ErrorConstants.USER_LOGIN_INVALID);
        assertEquals("La contraseña no puede estar vacía", ErrorConstants.USER_PASSWORD_EMPTY);
        assertEquals("La contraseña debe tener al menos 8 caracteres", ErrorConstants.USER_PASSWORD_TOO_SHORT);
        assertEquals("La nueva contraseña debe ser diferente a la actual", ErrorConstants.USER_PASSWORD_SAME_AS_OLD);
        assertEquals("Usuario no encontrado", ErrorConstants.USER_NOT_FOUND);

        Constructor<ErrorConstants> constructor = ErrorConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
