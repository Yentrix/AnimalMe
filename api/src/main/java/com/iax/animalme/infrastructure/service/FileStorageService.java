package com.iax.animalme.infrastructure.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.base-path:uploads}")
    private String basePath;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(basePath, "users"));
        Files.createDirectories(Paths.get(basePath, "pets"));
        Files.createDirectories(Paths.get(basePath, "publications"));
    }

    /**
     * @param folder Subcarpeta (users, pets, publications)
     * @param prefix Prefijo para el archivo (ej: user_id)
     */
    public String storeFile(MultipartFile file, String folder, String prefix) throws IOException {
        // Generamos un nombre único para evitar colisiones
        String fileName = prefix + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetLocation = Paths.get(basePath, folder).toAbsolutePath().resolve(fileName);
        
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/") // Endpoint que servirá las imágenes
                .path(folder + "/")
                .path(fileName)
                .toUriString();
    }
}