package com.ainexus.service;

import com.ainexus.dto.AuthResponse;
import com.ainexus.dto.RegisterRequest;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.DuplicateResourceException;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        String emailPrefix = request.getEmail().split("@")[0];
        String generatedUsername = emailPrefix + "_" + (System.currentTimeMillis() % 10000);
        if (userRepository.existsByUsername(generatedUsername)) {
            generatedUsername = "user_" + System.currentTimeMillis();
        }

        User user = User.builder()
                .name(request.getName())
                .username(generatedUsername)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-provision personal workspace
        Workspace personalWorkspace = Workspace.builder()
                .name(request.getName().trim() + "'s Workspace")
                .description("Default personal workspace for " + request.getName())
                .owner(savedUser)
                .build();
        Workspace savedWorkspace = workspaceRepository.save(personalWorkspace);

        String token = (jwtTokenProvider != null) ? jwtTokenProvider.generateToken(savedUser.getUsername()) : "dev-token";

        return AuthResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .token(token)
                .accessToken(token)
                .defaultWorkspaceId(savedWorkspace.getId())
                .build();
    }

    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("Username already in use: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(updatedUser.getName());
                    existingUser.setUsername(updatedUser.getUsername());
                    existingUser.setEmail(updatedUser.getEmail());
                    existingUser.setEnabled(updatedUser.isEnabled());
                    if (updatedUser.getRole() != null) {
                        existingUser.setRole(updatedUser.getRole());
                    }
                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
