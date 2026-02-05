package com.robotech.services;

import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.RoleRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User asignarRolesUsuario(Long userId, List<String> nombresRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Set<Role> nuevosRoles = new HashSet<>();
        
        for (String nombreRol : nombresRoles) {
            Role role = roleRepository.findByNombre(nombreRol)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));
            nuevosRoles.add(role);
        }

        user.setRoles(nuevosRoles);
        return userRepository.save(user);
    }

    @Transactional
    public User agregarRolUsuario(Long userId, String nombreRol) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Role role = roleRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    @Transactional
    public User removerRolUsuario(Long userId, String nombreRol) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Role role = roleRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        user.getRoles().remove(role);
        return userRepository.save(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<User> getUsuariosPorRol(String nombreRol) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getNombre().equals(nombreRol)))
                .toList();
    }
}