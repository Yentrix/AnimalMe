package com.iax.animalme.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Esto permite que al acceder a http://localhost:8080/uploads/nombre.png funcione
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/") // Asegúrate de que la ruta coincida con donde guardas los archivos
                .setCachePeriod(0);
    }
}
