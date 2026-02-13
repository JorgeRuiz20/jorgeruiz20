package com.robotech.config;

import com.robotech.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5501",
                "https://robotech-frontend2.onrender.com",
                "https://prueba1-vsoz--3000--1db57326.local-credentialless.webcontainer.io"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Health check público
                        .requestMatchers("/api/health/**").permitAll()

                        // Swagger público
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()

                        // Endpoints de autenticación públicos
                        .requestMatchers("/api/auth/**").permitAll()

                        // Recuperación de contraseña (PÚBLICO)
                        .requestMatchers("/api/auth/password/**").permitAll()

                        // Endpoints de upload públicos
                        .requestMatchers("/api/upload/**").permitAll()

                        // Verificación de código de registro (público)
                        .requestMatchers("/api/admin/codigos-registro/verificar/**").permitAll()
                        .requestMatchers("/api/codigos-registro/verificar/**").permitAll()

                        // Rutas públicas
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/categorias/**").permitAll()
                        .requestMatchers("/api/historial/**").permitAll()

                        // ✅ Restablecimiento de email (solo ADMIN)
                        .requestMatchers("/api/admin/email-reset/**").hasRole("ADMIN")

                        //edpoint eliminar club

                        .requestMatchers("/api/admin/club-deshabilitacion/**").hasRole("ADMIN")

                        // ✅ CLUBS: Solo ADMIN crea, CLUB_OWNER edita
                        .requestMatchers("/api/clubs").permitAll() // GET público
                        .requestMatchers("/api/clubs/my-club/**").hasRole("CLUB_OWNER") // CLUB_OWNER edita
                        .requestMatchers("/api/clubs/**").hasRole("ADMIN") // POST solo ADMIN

                        // Endpoints para admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        // ✅ SEDES: Solo ADMIN gestiona
                        .requestMatchers("/api/sedes").permitAll() // GET público
                        .requestMatchers("/api/sedes/**").hasRole("ADMIN") // POST/PUT/DELETE solo ADMIN

                        // Endpoints para club owner
                        
                        .requestMatchers("/api/robots/{id}/aprobar").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/robots/mi-club").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/robots/mi-club/pendientes").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/robots/{id}/rechazar").hasRole("CLUB_OWNER")

                        // ==================== TRANSFERENCIAS ====================
                        // ✅ NUEVO: Permitir a USER solicitar ingreso a clubs
                        .requestMatchers("/api/transferencias/solicitar-ingreso").hasRole("USER")
                        .requestMatchers("/api/transferencias/mis-solicitudes").hasAnyRole("USER", "COMPETITOR")
                        .requestMatchers("/api/transferencias/*/cancelar").hasAnyRole("USER", "COMPETITOR")

                        // ✅ Endpoints para COMPETITOR (transferencias normales)
                        .requestMatchers("/api/transferencias/solicitar").hasRole("COMPETITOR")

                        // ✅ Endpoints para CLUB_OWNER
                        .requestMatchers("/api/transferencias/pendientes-salida").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/transferencias/pendientes-ingreso").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/transferencias/*/procesar-salida").hasRole("CLUB_OWNER")
                        .requestMatchers("/api/transferencias/*/procesar-ingreso").hasRole("CLUB_OWNER")

                        // Endpoints para juez
                        // ✅ DESPUÉS (REEMPLAZAR CON ESTO):
                        // ✅ PÚBLICO: Cualquiera puede ver el bracket
                        .requestMatchers(HttpMethod.GET, "/api/torneos/*/bracket").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/torneos/*/enfrentamientos").hasRole("JUDGE")
                        .requestMatchers(HttpMethod.POST, "/api/torneos/*/avanzar-ganadores").hasRole("JUDGE")
                        .requestMatchers(HttpMethod.PUT, "/api/torneos/*/enfrentamientos/*/resultado").hasRole("JUDGE")
                        .requestMatchers(HttpMethod.POST, "/api/torneos/*/asignar-modalidad").hasRole("JUDGE")
                        .requestMatchers(HttpMethod.POST, "/api/torneos/*/generar-enfrentamientos").hasRole("JUDGE")
                        // Endpoints para competidor
                        .requestMatchers("/api/torneos/{id}/unirse").hasRole("COMPETITOR")
                        .requestMatchers("/api/robots/mis-robots").hasRole("COMPETITOR")
                        .requestMatchers("/api/robots").hasRole("COMPETITOR")

                        // ✅ TORNEOS: Solo ADMIN crea/edita
                        .requestMatchers("/api/torneos").permitAll() // GET público
                        .requestMatchers("/api/torneos/activos").permitAll()
                        .requestMatchers("/api/torneos/pendientes").permitAll()
                        .requestMatchers("/api/torneos/finalizados").permitAll()
                        .requestMatchers("/api/torneos/{id}").permitAll()
                        .requestMatchers("/api/torneos/{id}/participantes").permitAll()
                        .requestMatchers("/api/torneos/{id}/ranking").permitAll()
                        .requestMatchers("/api/torneos/categoria/**").permitAll()
                        .requestMatchers("/api/torneos/*/unirse").hasRole("COMPETITOR") // ✅ CORREGIDO
                        .requestMatchers("/api/torneos/**").hasRole("ADMIN") // POST/PUT/DELETE solo ADMIN

                        // Endpoints públicos de lectura
                        .requestMatchers("/api/competencias/torneo/**").permitAll()

                        // Endpoints de reportes
                        .requestMatchers("/api/reportes/**").hasAnyRole("ADMIN", "CLUB_OWNER", "JUDGE")

                        // Resto de endpoints requieren autenticación
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}