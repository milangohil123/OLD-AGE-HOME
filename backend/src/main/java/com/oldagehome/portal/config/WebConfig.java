package com.oldagehome.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the local upload directory via the URL path /uploads/**
        Path uploadPath = Paths.get(uploadDir);
        String resourceLocation = uploadPath.toAbsolutePath().toUri().toString();
        
        // Ensure trailing slash is appended for proper resource mapping resolution
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
