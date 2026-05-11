package com.iax.animalme.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.base-path:uploads}")
    private String fileBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedBasePath = fileBasePath.endsWith("/") ? fileBasePath : fileBasePath + "/";

        // Permite servir archivos subidos desde la ruta configurada
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizedBasePath)
                .setCachePeriod(0);
    }
}
