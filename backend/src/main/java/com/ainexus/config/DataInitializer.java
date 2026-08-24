package com.ainexus.config;

import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.repository.UserRepository;
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
    public void run(String... args) {
        userRepository.deleteAll();

        User admin = User.builder()
                .name("Admin User")
                .username("admin")
                .email("admin@ainexus.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        System.out.println("==================================================");
        System.out.println("[DATA INITIALIZER] SAVED ADMIN USER: admin@ainexus.com / Password123!");
        System.out.println("==================================================");
    }
}
