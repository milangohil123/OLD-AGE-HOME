package com.oldagehome.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.*;

@SpringBootApplication
public class PortalApplication {
    public static void main(String[] args) {
        copyImagesOnStartup();
        SpringApplication.run(PortalApplication.class, args);
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
