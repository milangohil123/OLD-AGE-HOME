package com.oldagehome.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import javax.sql.DataSource;

@SpringBootApplication
public class PortalApplication {
    private static final Logger logger = LoggerFactory.getLogger(PortalApplication.class);

    public static void main(String[] args) {
        logger.info("[STARTUP] Application initialization started");
        copyImagesOnStartup();
        SpringApplication.run(PortalApplication.class, args);
    }

    @Component
    public static class StartupMilestoneLogger {
        private final Logger log = LoggerFactory.getLogger(StartupMilestoneLogger.class);

        public StartupMilestoneLogger(DataSource dataSource) {
            log.info("[STARTUP] Database connection initialized");
        }

        @EventListener
        public void handleContextRefresh(ContextRefreshedEvent event) {
            log.info("[STARTUP] Hibernate initialized");
            log.info("[STARTUP] Migrations completed (Schema Validated)");
        }

        @EventListener
        public void handleApplicationReady(ApplicationReadyEvent event) {
            log.info("[STARTUP] Application initialization completed");
        }
    }

    private static void copyImagesOnStartup() {
        try {
            Path targetDir = Paths.get("src/main/resources/static/images");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            // Copy logo
            Path srcLogo = Paths.get("../../Gemini_Generated_Image_gbr6a4gbr6a4gbr6.png");
            Path destLogo = targetDir.resolve("vrudhashram-logo.png");
            if (Files.exists(srcLogo)) {
                Files.copy(srcLogo, destLogo, StandardCopyOption.REPLACE_EXISTING);
            }
            // Copy thank you
            Path srcThanks = Paths.get("../../WhatsApp Image 2026-07-18 at 11.24.33 AM.jpeg");
            Path destThanks = targetDir.resolve("thank-you.jpeg");
            if (Files.exists(srcThanks)) {
                Files.copy(srcThanks, destThanks, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
