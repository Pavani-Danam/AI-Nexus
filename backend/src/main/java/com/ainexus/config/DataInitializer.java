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
        if (userRepository.findByEmail("admin@ainexus.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Administrator")
                    .username("admin")
                    .email("admin@ainexus.com")
                    .password(passwordEncoder.encode("AdminPassword123!"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }
    }
}
