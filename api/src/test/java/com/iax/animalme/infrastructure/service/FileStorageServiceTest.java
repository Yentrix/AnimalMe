package com.iax.animalme.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class FileStorageServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void initAndStoreFileCreateDirectoriesAndWriteFile() throws Exception {
        Path tempDir = Files.createTempDirectory("animalme-files");

        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "basePath", tempDir.toString());
        service.init();

        assertTrue(Files.isDirectory(tempDir.resolve("users")));
        assertTrue(Files.isDirectory(tempDir.resolve("pets")));
        assertTrue(Files.isDirectory(tempDir.resolve("publications")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MockMultipartFile file = new MockMultipartFile("f", "photo.png", "image/png", "img-content".getBytes());
        String url = service.storeFile(file, "pets", "99");

        assertTrue(url.contains("/uploads/pets/99_"));
        assertTrue(Files.list(tempDir.resolve("pets")).findAny().isPresent());
    }
}
