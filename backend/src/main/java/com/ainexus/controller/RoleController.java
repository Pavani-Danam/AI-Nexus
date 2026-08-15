package com.ainexus.controller;

import com.ainexus.entity.RoleEntity;
import com.ainexus.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleEntity> createRole(@RequestParam String name) {
        RoleEntity role = roleService.createRole(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    @GetMapping
    public ResponseEntity<List<RoleEntity>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }
}
