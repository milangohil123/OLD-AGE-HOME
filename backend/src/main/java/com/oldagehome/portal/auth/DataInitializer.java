package com.oldagehome.portal.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .email("admin@oldagehome.com")
                    .role("ADMIN")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            System.out.println("=================================================");
            System.out.println("Default Admin seeded successfully!");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("=================================================");
        }
    }
}
