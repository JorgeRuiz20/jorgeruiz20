package com.robotech.services;

import com.robotech.dto.CreateUserRequest;
import com.robotech.models.Club;
import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.ClubRepository;
import com.robotech.repositories.RoleRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClubRepository clubRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SimilarityService similarityService;

    @Transactional
    public Map<String, Object> createUserAndSendCredentials(CreateUserRequest request) {
        try {
            // ✅ VALIDAR DNI
            if (!similarityService.validarDNI(request.getDni())) {
                throw new RuntimeException("DNI inválido. Debe ser exactamente 8 dígitos numéricos");
            }
            
            // ✅ VALIDAR NOMBRE Y APELLIDO
            if (!similarityService.validarNombre(request.getNombre())) {
                throw new RuntimeException("Nombre inválido. Solo letras y espacios (mín 2, máx 50 caracteres)");
            }
            
            if (!similarityService.validarNombre(request.getApellido())) {
                throw new RuntimeException("Apellido inválido. Solo letras y espacios (mín 2, máx 50 caracteres)");
            }
            
            // ✅ VALIDAR EMAIL
            if (!similarityService.validarEmail(request.getEmail())) {
                throw new RuntimeException("El formato del email no es válido");
            }
            
            // Validar que el email no esté registrado
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("El email ya está registrado");
            }

            if (userRepository.existsByDni(request.getDni())) {
                throw new RuntimeException("El DNI ya está registrado");
            }

            // ✅ VALIDACIÓN NOMBRE COMPLETO (70% - permisivo)
            String nombreCompletoNuevo = request.getNombre() + " " + request.getApellido();
            List<String> nombresCompletosExistentes = userRepository.findAll().stream()
                    .map(u -> u.getNombre() + " " + u.getApellido())
                    .collect(Collectors.toList());
            
            if (similarityService.existeNombreCompletoSimilar(nombreCompletoNuevo, nombresCompletosExistentes)) {
                String nombreSimilar = similarityService.encontrarNombreCompletoSimilar(nombreCompletoNuevo, nombresCompletosExistentes);
                throw new RuntimeException("Ya existe un usuario con nombre similar: '" + nombreSimilar + "'. Verifica antes de crear.");
            }

            // Validar que los roles existan
            Set<Role> roles = new HashSet<>();
            boolean esClubOwner = false;
            
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByNombre(roleName)
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleName));
                roles.add(role);
                
                if ("ROLE_CLUB_OWNER".equals(roleName)) {
                    esClubOwner = true;
                }
            }

            // Generar contraseña temporal aleatoria
            String temporalPassword = generateTemporalPassword();

            // Crear usuario
            User user = new User();
            user.setNombre(request.getNombre());
            user.setApellido(request.getApellido());
            user.setDni(request.getDni());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(temporalPassword));
            user.setTelefono(request.getTelefono());
            user.setFechaNacimiento(request.getFechaNacimiento());
            user.setFotoPerfil(request.getFotoPerfil());
            user.setEstado("APROBADO");
            user.setRoles(roles);
            user.setClub(null);

            User savedUser = userRepository.save(user);

            // ✅ SI ES CLUB OWNER, CREAR CLUB AUTOMÁTICAMENTE
            Club clubCreado = null;
            if (esClubOwner) {
                clubCreado = crearClubParaOwner(savedUser);
            }

            // Preparar datos para email
            List<String> rolesNombres = roles.stream()
                    .map(Role::getNombre)
                    .toList();

            // Intentar enviar email con credenciales
            boolean emailEnviado = false;
            String emailError = null;
            
            try {
                emailService.sendCredentialsEmail(
                    savedUser.getEmail(),
                    savedUser.getNombre() + " " + savedUser.getApellido(),
                    temporalPassword,
                    rolesNombres
                );
                emailEnviado = true;
            } catch (Exception emailEx) {
                emailError = emailEx.getMessage();
                System.err.println("❌ Error enviando email: " + emailEx.getMessage());
            }

            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", savedUser.getId());
            response.put("email", savedUser.getEmail());
            response.put("temporalPassword", temporalPassword);
            response.put("roles", rolesNombres);
            response.put("emailEnviado", emailEnviado);
            
            if (emailEnviado) {
                response.put("message", "Usuario creado exitosamente. Credenciales enviadas a: " + savedUser.getEmail());
            } else {
                response.put("message", "Usuario creado pero el email no pudo ser enviado. Contraseña temporal: " + temporalPassword);
                response.put("emailError", emailError != null ? emailError : "Error desconocido al enviar email");
            }
            
            // Agregar info del club si se creó
            if (clubCreado != null) {
                response.put("clubId", clubCreado.getId());
                response.put("clubNombre", clubCreado.getNombre());
                response.put("message", "Usuario y Club creados exitosamente. " + 
                    (emailEnviado ? "Credenciales enviadas a: " + savedUser.getEmail() 
                                  : "Contraseña temporal: " + temporalPassword));
            }
            
            return response;
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al crear usuario: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ CREAR CLUB AUTOMÁTICAMENTE PARA CLUB OWNER (con validación mejorada)
     */
    private Club crearClubParaOwner(User owner) {
        String baseNombre = "Club de " + owner.getNombre() + " " + owner.getApellido();
        String nombreFinal = baseNombre;
        int contador = 1;
        
        // Verificar existencia usando Jaro-Winkler (umbral 83% para clubs)
        List<String> nombresExistentes = clubRepository.findAll().stream()
                .map(Club::getNombre)
                .toList();
        
        while (similarityService.existeClubSimilar(nombreFinal, nombresExistentes)) {
            nombreFinal = baseNombre + " (" + contador + ")";
            contador++;
        }
        
        Club club = new Club();
        club.setNombre(nombreFinal);
        club.setDescripcion("Club creado automáticamente. Actualiza esta descripción.");
        club.setCiudad("Por definir");
        club.setPais("Perú");
        club.setOwner(owner);
        club.setMaxParticipantes(16);
        
        Club clubGuardado = clubRepository.save(club);
        
        // Asignar el club al usuario
        owner.setClub(clubGuardado);
        userRepository.save(owner);
        
        return clubGuardado;
    }

    private String generateTemporalPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@!*?";

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 2; i++) password.append(upper.charAt((int) (Math.random() * upper.length())));
        for (int i = 0; i < 2; i++) password.append(lower.charAt((int) (Math.random() * lower.length())));
        for (int i = 0; i < 2; i++) password.append(digits.charAt((int) (Math.random() * digits.length())));
        for (int i = 0; i < 2; i++) password.append(special.charAt((int) (Math.random() * special.length())));

        char[] arr = password.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }

        return new String(arr);
    }
}