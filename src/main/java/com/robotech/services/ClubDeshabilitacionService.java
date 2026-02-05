package com.robotech.services;

import com.robotech.dto.ClubDeshabilitacionDTO;
import com.robotech.models.*;
import com.robotech.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubDeshabilitacionService {

    private final ClubDeshabilitacionRepository deshabilitacionRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final RobotRepository robotRepository;
    private final RoleRepository roleRepository;
    private final SolicitudTransferenciaRepository transferenciaRepository;
    private final EmailService emailService;

    /**
     * ✅ PASO 1: Admin deshabilita un club
     */
    @Transactional
    public ClubDeshabilitacionDTO deshabilitarClub(Long clubId, Long adminId, String motivo, Integer diasLimite) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        // Verificar que es admin
        boolean esAdmin = admin.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));

        if (!esAdmin) {
            throw new RuntimeException("Solo administradores pueden deshabilitar clubs");
        }

        // Verificar que no haya una deshabilitación activa
        if (deshabilitacionRepository.existsByClubIdAndEstadoIn(clubId,
                Arrays.asList("PENDIENTE", "PROCESANDO"))) {
            throw new RuntimeException("Este club ya tiene un proceso de deshabilitación activo");
        }

        // Obtener miembros del club
        List<User> miembros = userRepository.findByClubIdAndEstado(clubId, "APROBADO");

        if (miembros.isEmpty()) {
            // Si no hay miembros, deshabilitar directamente
            club.setActiva(false);
            clubRepository.save(club);
            throw new RuntimeException("Club deshabilitado directamente (sin miembros)");
        }

        // Crear registro de deshabilitación
        ClubDeshabilitacion deshabilitacion = new ClubDeshabilitacion();
        deshabilitacion.setClub(club);
        deshabilitacion.setAdmin(admin);
        deshabilitacion.setMotivo(motivo);
        deshabilitacion.setEstado("PENDIENTE");
        deshabilitacion.setTotalMiembros(miembros.size());
        deshabilitacion.setFechaDeshabilitacion(LocalDateTime.now());
        deshabilitacion.setFechaLimiteAccion(LocalDateTime.now().plusDays(diasLimite != null ? diasLimite : 7));

        ClubDeshabilitacion saved = deshabilitacionRepository.save(deshabilitacion);

        // Enviar notificaciones por email a todos los miembros
        enviarNotificacionesMiembros(club, miembros, saved);

        return convertToDTO(saved, miembros);
    }

    /**
     * ✅ Enviar emails de notificación a miembros
     */
    private void enviarNotificacionesMiembros(Club club, List<User> miembros, ClubDeshabilitacion deshabilitacion) {
        for (User miembro : miembros) {
            try {
                emailService.sendClubDeshabilitacionNotification(
                        miembro.getEmail(),
                        miembro.getNombre() + " " + miembro.getApellido(),
                        club.getNombre(),
                        deshabilitacion.getMotivo(),
                        deshabilitacion.getFechaLimiteAccion(),
                        deshabilitacion.getId());
            } catch (Exception e) {
                System.err.println("Error enviando email a: " + miembro.getEmail() + " - " + e.getMessage());
            }
        }

        deshabilitacion.setNotificacionesEnviadas(true);
        deshabilitacionRepository.save(deshabilitacion);
    }

    /**
     * ✅ PASO 2: Admin envía solicitudes masivas a un club específico
     */
    @Transactional
    public Map<String, Object> enviarSolicitudesMasivas(Long deshabilitacionId, Long clubDestinoId, Long adminId) {
        ClubDeshabilitacion deshabilitacion = deshabilitacionRepository.findById(deshabilitacionId)
                .orElseThrow(() -> new RuntimeException("Deshabilitación no encontrada"));

        if (!deshabilitacion.isPendiente() && !deshabilitacion.isProcesando()) {
            throw new RuntimeException("Esta deshabilitación no está en estado válido");
        }

        Club clubDestino = clubRepository.findById(clubDestinoId)
                .orElseThrow(() -> new RuntimeException("Club destino no encontrado"));

        // Obtener miembros que NO tienen transferencia pendiente
        List<User> miembrosSinTransferencia = obtenerMiembrosSinTransferencia(deshabilitacion.getClub().getId());

        if (miembrosSinTransferencia.isEmpty()) {
            throw new RuntimeException("No hay miembros disponibles para transferir");
        }

        // Verificar cupos del club destino
        int cuposDisponibles = clubDestino.getCuposDisponibles();
        if (cuposDisponibles < miembrosSinTransferencia.size()) {
            throw new RuntimeException("El club destino solo tiene " + cuposDisponibles +
                    " cupos disponibles y se necesitan " + miembrosSinTransferencia.size());
        }

        int solicitudesCreadas = 0;
        for (User miembro : miembrosSinTransferencia) {
            try {
                // Crear solicitud de transferencia automática
                SolicitudTransferencia solicitud = new SolicitudTransferencia();
                solicitud.setUsuario(miembro);
                solicitud.setClubOrigen(deshabilitacion.getClub());
                solicitud.setClubDestino(clubDestino);
                solicitud.setEstado("PENDIENTE_SALIDA");
                solicitud.setMensajeUsuario("Solicitud automática por deshabilitación de club: " +
                        deshabilitacion.getMotivo());

                transferenciaRepository.save(solicitud);
                solicitudesCreadas++;

            } catch (Exception e) {
                System.err.println("Error creando solicitud para: " + miembro.getEmail());
            }
        }

        deshabilitacion.setEstado("PROCESANDO");
        deshabilitacionRepository.save(deshabilitacion);

        return Map.of(
                "solicitudesCreadas", solicitudesCreadas,
                "clubDestino", clubDestino.getNombre(),
                "mensaje", solicitudesCreadas + " solicitudes enviadas al club " + clubDestino.getNombre());
    }

    /**
     * ✅ VERSIÓN FINAL CORREGIDA: Degradar miembros restantes de un club
     * deshabilitado
     * 
     * Maneja correctamente la adición de roles evitando duplicados en la base de
     * datos
     */
    @Transactional
    public Map<String, Object> degradarMiembrosRestantes(Long deshabilitacionId, Long adminId) {
        ClubDeshabilitacion deshabilitacion = deshabilitacionRepository.findById(deshabilitacionId)
                .orElseThrow(() -> new RuntimeException("Deshabilitación no encontrada"));

        List<User> miembrosSinReubicar = obtenerMiembrosSinTransferencia(deshabilitacion.getClub().getId());

        // ✅ Obtener todos los roles necesarios
        Role competitorRole = roleRepository.findByNombre("ROLE_COMPETITOR")
                .orElseThrow(() -> new RuntimeException("Rol COMPETITOR no encontrado"));

        Role clubOwnerRole = roleRepository.findByNombre("ROLE_CLUB_OWNER")
                .orElseThrow(() -> new RuntimeException("Rol CLUB_OWNER no encontrado"));

        Role userRole = roleRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado"));

        int degradados = 0;
        int clubOwnersAfectados = 0;

        for (User miembro : miembrosSinReubicar) {
            try {
                // Verificar si es club owner antes de degradar
                boolean esClubOwner = miembro.getRoles().stream()
                        .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));

                if (esClubOwner) {
                    clubOwnersAfectados++;
                }

                // ✅ Remover roles de club (COMPETITOR y CLUB_OWNER)
                miembro.getRoles().removeIf(role -> "ROLE_COMPETITOR".equals(role.getNombre()) ||
                        "ROLE_CLUB_OWNER".equals(role.getNombre()));

                // ✅ CRÍTICO: Asegurarse de que tenga ROLE_USER
                // Verificar si ya tiene el rol (por ID, no por equals)
                boolean tieneRoleUser = miembro.getRoles().stream()
                        .anyMatch(role -> role.getId().equals(userRole.getId()));

                if (!tieneRoleUser) {
                    miembro.getRoles().add(userRole);
                }

                // Quitar club
                miembro.setClub(null);

                // Rechazar robots pendientes
                List<Robot> robots = robotRepository.findByUsuarioId(miembro.getId());
                for (Robot robot : robots) {
                    if ("PENDIENTE".equals(robot.getEstado())) {
                        robot.setEstado("RECHAZADO");
                        robot.setMotivoRechazo("Club deshabilitado - usuario no reubicado");
                        robotRepository.save(robot);
                    }
                }

                // ✅ Guardar el usuario (JPA manejará la tabla intermedia correctamente)
                userRepository.save(miembro);
                degradados++;

                // Notificar por email
                String tipoUsuario = esClubOwner ? "dueño del club" : "competidor";
                emailService.sendDegradacionNotification(
                        miembro.getEmail(),
                        miembro.getNombre() + " " + miembro.getApellido(),
                        deshabilitacion.getClub().getNombre());

                System.out.println("✅ Usuario degradado: " + miembro.getEmail() +
                        " (Era " + (esClubOwner ? "CLUB_OWNER" : "COMPETITOR") +
                        ", ahora es USER)");

            } catch (Exception e) {
                System.err.println("❌ Error degradando a: " + miembro.getEmail());
                e.printStackTrace();
            }
        }

        deshabilitacion.setMiembrosDegradados(degradados);
        deshabilitacion.setEstado("COMPLETADA");
        deshabilitacion.setFechaCompletada(LocalDateTime.now());
        deshabilitacion.setObservaciones(
                String.format("Degradados: %d usuarios (incluyendo %d club owner(s))",
                        degradados, clubOwnersAfectados));
        deshabilitacionRepository.save(deshabilitacion);

        // Deshabilitar el club
        Club club = deshabilitacion.getClub();
        club.setActiva(false);
        clubRepository.save(club);

        return Map.of(
                "degradados", degradados,
                "clubOwnersAfectados", clubOwnersAfectados,
                "mensaje", String.format("%d usuarios degradados a ROLE_USER (incluyendo %d club owner(s))",
                        degradados, clubOwnersAfectados),
                "clubDeshabilitado", true,
                "detalles", "Todos los usuarios ahora tienen solo ROLE_USER y pueden solicitar ingreso a otros clubs");
    }

    /**
     * ✅ Obtener miembros sin transferencia pendiente
     */
    private List<User> obtenerMiembrosSinTransferencia(Long clubId) {
        List<User> miembros = userRepository.findByClubIdAndEstado(clubId, "APROBADO");

        return miembros.stream()
                .filter(m -> {
                    Optional<SolicitudTransferencia> solicitud = transferenciaRepository
                            .findSolicitudPendienteByUsuario(m.getId());
                    return solicitud.isEmpty();
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ Obtener estado de deshabilitación
     */
    public ClubDeshabilitacionDTO getEstadoDeshabilitacion(Long deshabilitacionId) {
        ClubDeshabilitacion deshabilitacion = deshabilitacionRepository.findById(deshabilitacionId)
                .orElseThrow(() -> new RuntimeException("Deshabilitación no encontrada"));

        List<User> miembros = userRepository.findByClubIdAndEstado(
                deshabilitacion.getClub().getId(), "APROBADO");

        return convertToDTO(deshabilitacion, miembros);
    }

    /**
     * ✅ Listar todas las deshabilitaciones
     */
    public List<ClubDeshabilitacionDTO> listarDeshabilitaciones() {
        return deshabilitacionRepository.findAll().stream()
                .map(d -> {
                    List<User> miembros = userRepository.findByClubIdAndEstado(
                            d.getClub().getId(), "APROBADO");
                    return convertToDTO(d, miembros);
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ Cancelar deshabilitación (si aún no está completada)
     */
    @Transactional
    public void cancelarDeshabilitacion(Long deshabilitacionId, Long adminId) {
        ClubDeshabilitacion deshabilitacion = deshabilitacionRepository.findById(deshabilitacionId)
                .orElseThrow(() -> new RuntimeException("Deshabilitación no encontrada"));

        if (deshabilitacion.isCompletada()) {
            throw new RuntimeException("No se puede cancelar una deshabilitación completada");
        }

        deshabilitacion.setEstado("CANCELADA");
        deshabilitacion.setFechaCompletada(LocalDateTime.now());
        deshabilitacionRepository.save(deshabilitacion);
    }

    /**
     * ✅ Convertir a DTO
     */
    private ClubDeshabilitacionDTO convertToDTO(ClubDeshabilitacion d, List<User> miembros) {
        ClubDeshabilitacionDTO dto = new ClubDeshabilitacionDTO();

        dto.setId(d.getId());
        dto.setClubId(d.getClub().getId());
        dto.setClubNombre(d.getClub().getNombre());
        dto.setAdminNombre(d.getAdmin().getNombre() + " " + d.getAdmin().getApellido());
        dto.setMotivo(d.getMotivo());
        dto.setEstado(d.getEstado());
        dto.setFechaDeshabilitacion(d.getFechaDeshabilitacion());
        dto.setFechaLimiteAccion(d.getFechaLimiteAccion());
        dto.setFechaCompletada(d.getFechaCompletada());
        dto.setTotalMiembros(d.getTotalMiembros());
        dto.setMiembrosReubicados(d.getMiembrosReubicados());
        dto.setMiembrosDegradados(d.getMiembrosDegradados());
        dto.setNotificacionesEnviadas(d.getNotificacionesEnviadas());
        dto.setObservaciones(d.getObservaciones());
        dto.setLimiteExpirado(d.limiteExpirado());

        // Calcular días restantes
        if (!d.isCompletada()) {
            Duration duracion = Duration.between(LocalDateTime.now(), d.getFechaLimiteAccion());
            dto.setDiasRestantes(duracion.toDays());
        }

        // Calcular miembros pendientes
        int miembrosSinTransferencia = obtenerMiembrosSinTransferencia(d.getClub().getId()).size();
        dto.setMiembrosPendientes(miembrosSinTransferencia);

        return dto;
    }
}