package com.berkan.receiptscanner.config;

import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import com.berkan.receiptscanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername(adminUsername).isPresent()) {
                logger.info("Admin user already exists: {}", adminUsername);
                return;
            }

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);

            logger.info("Admin user created successfully. Username: {}", adminUsername);
            logger.warn("Remember to change the admin password after first login!");
        };
    }
}
