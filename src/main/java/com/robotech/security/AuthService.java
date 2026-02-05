package com.robotech.security;

import com.robotech.dto.AuthResponse;
import com.robotech.dto.LoginRequest;
import com.robotech.dto.RegisterRequest;
import com.robotech.models.*;
import com.robotech.repositories.*;
import com.robotech.services.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClubRepository clubRepository;
    private final CodigoRegistroRepository codigoRegistroRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SimilarityService similarityService;

    @Transactional
    public void register(RegisterRequest request) {
        // ✅ VALIDAR DNI: Exactamente 8 dígitos numéricos
        if (!similarityService.validarDNI(request.getDni())) {
            throw new RuntimeException("DNI inválido. Debe ser exactamente 8 dígitos numéricos (sin espacios ni caracteres especiales)");
        }
        
        // ✅ VALIDAR NOMBRE Y APELLIDO
        if (!similarityService.validarNombre(request.getNombre())) {
            throw new RuntimeException("Nombre inválido. Solo se permiten letras y espacios (mínimo 2, máximo 50 caracteres)");
        }
        
        if (!similarityService.validarNombre(request.getApellido())) {
            throw new RuntimeException("Apellido inválido. Solo se permiten letras y espacios (mínimo 2, máximo 50 caracteres)");
        }
        
        // ✅ VALIDAR EMAIL: Formato correcto
        if (!similarityService.validarEmail(request.getEmail())) {
            throw new RuntimeException("El formato del email no es válido");
        }
        
        // ✅ VALIDACIÓN EMAIL: Similitud con emails existentes (umbral 75% - permisivo)
        String emailNuevo = request.getEmail();
        List<String> emailsExistentes = userRepository.findAll().stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
        
        if (similarityService.existeEmailSimilar(emailNuevo, emailsExistentes)) {
            String emailSimilar = similarityService.encontrarEmailSimilar(emailNuevo, emailsExistentes);
            throw new RuntimeException("Ya existe un email similar registrado: '" + emailSimilar + "'. Por favor usa un email diferente.");
        }
        
        // ✅ VALIDACIÓN NOMBRE COMPLETO: Similitud (umbral 70% - muy permisivo)
        String nombreCompletoNuevo = request.getNombre() + " " + request.getApellido();
        List<String> nombresCompletosExistentes = userRepository.findAll().stream()
                .map(u -> u.getNombre() + " " + u.getApellido())
                .collect(Collectors.toList());
        
        if (similarityService.existeNombreCompletoSimilar(nombreCompletoNuevo, nombresCompletosExistentes)) {
            String nombreSimilar = similarityService.encontrarNombreCompletoSimilar(nombreCompletoNuevo, nombresCompletosExistentes);
            throw new RuntimeException("Ya existe un usuario con nombre similar: '" + nombreSimilar + "'. Si eres tú, contacta al administrador.");
        }
        
        // Validar contraseña
        if (!isValidPassword(request.getPassword())) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial");
        }

        // Validar código de registro
        CodigoRegistro codigoRegistro = codigoRegistroRepository.findByCodigo(request.getCodigoRegistro())
                .orElseThrow(() -> new RuntimeException("Código de registro inválido"));

        if (codigoRegistro.getUsado()) {
            throw new RuntimeException("Este código de registro ya ha sido utilizado");
        }

        // Validar DNI único
        if (userRepository.existsByDni(request.getDni())) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        // Obtener club del código
        Club club = codigoRegistro.getClub();
        if (club == null) {
            throw new RuntimeException("El código de registro no tiene un club asociado");
        }

        // Validar que el club no esté lleno
        if (club.isFull()) {
            throw new RuntimeException("El club ha alcanzado el máximo de " + club.getMaxParticipantes() + " participantes");
        }

        // Crear rol de usuario y competidor
        Role roleUser = roleRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));
        
        Role roleCompetitor = roleRepository.findByNombre("ROLE_COMPETITOR")
                .orElseThrow(() -> new RuntimeException("Rol COMPETITOR no encontrado"));

        Set<Role> rolesToAssign = new HashSet<>();
        rolesToAssign.add(roleUser);
        rolesToAssign.add(roleCompetitor);

        // Crear usuario
        User user = new User();
        user.setDni(request.getDni());
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFotoPerfil(request.getFotoPerfil());
        user.setEstado("APROBADO");
        user.setTelefono(request.getTelefono());
        user.setFechaNacimiento(request.getFechaNacimiento());
        user.setRoles(rolesToAssign);
        user.setClub(club);

        User savedUser = userRepository.save(user);

        // Marcar código como usado
        codigoRegistro.setUsado(true);
        codigoRegistro.setUsadoPor(savedUser);
        codigoRegistro.setFechaUso(LocalDateTime.now());
        codigoRegistroRepository.save(codigoRegistro);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            if ("RECHAZADO".equals(user.getEstado())) {
                throw new RuntimeException("Tu cuenta ha sido desactivada. Contacta al administrador");
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String token = jwtService.generateToken(user);
            
            return AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .nombre(user.getNombre() + " " + user.getApellido())
                    .estado(user.getEstado())
                    .clubId(user.getClub() != null ? user.getClub().getId() : null)
                    .dni(user.getDni())
                    .telefono(user.getTelefono())
                    .roles(user.getRoles().stream()
                            .map(Role::getNombre)
                            .collect(Collectors.toSet()))
                    .build();

        } catch (BadCredentialsException ex) {
            throw new RuntimeException("Credenciales inválidas: email o contraseña incorrectos");
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=\\-{}|:;\"'<>,.?/]).{8,}$";
        return password.matches(pattern);
    }
}