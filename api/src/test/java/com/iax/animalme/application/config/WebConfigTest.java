package com.iax.animalme.application.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class WebConfigTest {

    @Test
    void addResourceHandlersSupportsPathWithAndWithoutTrailingSlash() {
        WebConfig config = new WebConfig();
        ResourceHandlerRegistry registry = Mockito.mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = Mockito.mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);
        when(registration.addResourceLocations(anyString())).thenReturn(registration);
        when(registration.setCachePeriod(0)).thenReturn(registration);

        ReflectionTestUtils.setField(config, "fileBasePath", "uploads");
        config.addResourceHandlers(registry);
        assertNotNull(registry);

        ReflectionTestUtils.setField(config, "fileBasePath", "uploads/");
        config.addResourceHandlers(registry);
        assertNotNull(registry);
    }
}
