package com.robotech.services;

import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.RoleRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User updateUserRoles(Long userId, Set<Role> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.getRoles().clear();
        
        for (Role role : newRoles) {
            Role existingRole = roleRepository.findByNombre(role.getNombre())
                    .orElseThrow(() -> new RuntimeException("Rol no válido: " + role.getNombre()));
            user.getRoles().add(existingRole);
        }

        return userRepository.save(user);
    }

    public User updateUserEstado(Long userId, String estado) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setEstado(estado);
        return userRepository.save(user);
    }
}