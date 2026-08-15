package com.ainexus.service;

import com.ainexus.entity.RoleEntity;
import com.ainexus.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public RoleEntity createRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(name).build()));
    }

    @Transactional(readOnly = true)
    public Optional<RoleEntity> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<RoleEntity> getAllRoles() {
        return roleRepository.findAll();
    }
}
