package com.robotech.services;

import com.robotech.dto.SolicitudTransferenciaDTO;
import com.robotech.models.*;
import com.robotech.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private final SolicitudTransferenciaRepository solicitudTransferenciaRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final RobotRepository robotRepository;
    private final RoleRepository roleRepository;

    /**
     * ✅ NUEVO: Usuario sin club (rol USER) solicita unirse a un club
     */
    @Transactional
    public SolicitudTransferenciaDTO solicitarIngresoNuevo(Long usuarioId, Long clubDestinoId, String mensaje) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que NO tiene club
        if (usuario.getClub() != null) {
            throw new RuntimeException("Ya perteneces a un club. Usa la transferencia normal");
        }

        // Validar que tiene rol USER pero NO COMPETITOR
        boolean esUser = usuario.getRoles().stream()
                .anyMatch(role -> "ROLE_USER".equals(role.getNombre()));
        
        boolean esCompetidor = usuario.getRoles().stream()
                .anyMatch(role -> "ROLE_COMPETITOR".equals(role.getNombre()));

        if (!esUser || esCompetidor) {
            throw new RuntimeException("Esta funcionalidad es solo para usuarios sin club");
        }

        Club clubDestino = clubRepository.findById(clubDestinoId)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));

        // Validar que no tenga solicitud pendiente
        var solicitudExistente = solicitudTransferenciaRepository.findSolicitudPendienteByUsuario(usuarioId);
        if (solicitudExistente.isPresent()) {
            throw new RuntimeException("Ya tienes una solicitud pendiente");
        }

        // Validar cupos del club destino
        if (clubDestino.isFull()) {
            throw new RuntimeException("El club no tiene cupos disponibles (" + 
                clubDestino.getMaxParticipantes() + " máximo)");
        }

        // Crear solicitud directa de ingreso (sin club origen)
        SolicitudTransferencia solicitud = new SolicitudTransferencia();
        solicitud.setUsuario(usuario);
        solicitud.setClubOrigen(null); // ✅ SIN CLUB ORIGEN
        solicitud.setClubDestino(clubDestino);
        solicitud.setEstado("PENDIENTE_INGRESO"); // ✅ Directamente a PENDIENTE_INGRESO
        solicitud.setMensajeUsuario(mensaje);
        solicitud.setTipoSolicitud("INGRESO_NUEVO"); // ✅ Nuevo tipo

        SolicitudTransferencia saved = solicitudTransferenciaRepository.save(solicitud);
        
        System.out.println("✅ Solicitud de ingreso nuevo creada:");
        System.out.println("   Usuario: " + usuario.getEmail());
        System.out.println("   Club destino: " + clubDestino.getNombre());
        
        return convertToDTO(saved);
    }

    /**
     * ✅ PASO 1: Competidor solicita transferencia (ORIGINAL - sin cambios)
     */
    @Transactional
    public SolicitudTransferenciaDTO solicitarTransferencia(Long usuarioId, Long clubDestinoId, String mensaje) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que es competidor
        boolean esCompetidor = usuario.getRoles().stream()
                .anyMatch(role -> "ROLE_COMPETITOR".equals(role.getNombre()));
        
        if (!esCompetidor) {
            throw new RuntimeException("Solo los competidores pueden solicitar transferencias");
        }

        // Validar que tiene club actual
        if (usuario.getClub() == null) {
            throw new RuntimeException("No perteneces a ningún club actualmente");
        }

        Club clubOrigen = usuario.getClub();
        Club clubDestino = clubRepository.findById(clubDestinoId)
                .orElseThrow(() -> new RuntimeException("Club destino no encontrado"));

        // Validar que no sea el mismo club
        if (clubOrigen.getId().equals(clubDestino.getId())) {
            throw new RuntimeException("No puedes transferirte al mismo club");
        }

        // Validar que no tenga solicitud pendiente
        var solicitudExistente = solicitudTransferenciaRepository.findSolicitudPendienteByUsuario(usuarioId);
        if (solicitudExistente.isPresent()) {
            throw new RuntimeException("Ya tienes una solicitud de transferencia pendiente");
        }

        // Validar cupos del club destino
        if (clubDestino.isFull()) {
            throw new RuntimeException("El club destino no tiene cupos disponibles (" + 
                clubDestino.getMaxParticipantes() + " máximo)");
        }

        // Crear solicitud
        SolicitudTransferencia solicitud = new SolicitudTransferencia();
        solicitud.setUsuario(usuario);
        solicitud.setClubOrigen(clubOrigen);
        solicitud.setClubDestino(clubDestino);
        solicitud.setEstado("PENDIENTE_SALIDA");
        solicitud.setMensajeUsuario(mensaje);
        solicitud.setTipoSolicitud("TRANSFERENCIA"); // ✅ Tipo TRANSFERENCIA

        SolicitudTransferencia saved = solicitudTransferenciaRepository.save(solicitud);
        
        return convertToDTO(saved);
    }

    /**
     * ✅ PASO 2: Club Owner ORIGEN aprueba/rechaza salida (ORIGINAL - sin cambios)
     */
    @Transactional
    public SolicitudTransferenciaDTO procesarSalida(Long solicitudId, Long clubOwnerId, boolean aprobar, String motivo) {
        SolicitudTransferencia solicitud = solicitudTransferenciaRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Validar estado
        if (!solicitud.isPendienteSalida()) {
            throw new RuntimeException("Esta solicitud no está pendiente de salida");
        }

        // Validar que es el owner del club origen
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!solicitud.getClubOrigen().getOwner().getId().equals(clubOwnerId)) {
            throw new RuntimeException("No eres el dueño del club de origen");
        }

        if (aprobar) {
            solicitud.setEstado("PENDIENTE_INGRESO");
            solicitud.setFechaAprobacionSalida(LocalDateTime.now());
            solicitud.setAprobadoSalidaPor(clubOwner);
            
        } else {
            solicitud.setEstado("RECHAZADA_SALIDA");
            solicitud.setMotivoRechazo(motivo != null ? motivo : "El club actual rechazó la transferencia");
            solicitud.setFechaRechazo(LocalDateTime.now());
            solicitud.setRechazadoPor(clubOwner);
        }

        SolicitudTransferencia saved = solicitudTransferenciaRepository.save(solicitud);
        return convertToDTO(saved);
    }

    /**
     * ✅ MODIFICADO: Ahora maneja INGRESO_NUEVO y TRANSFERENCIA
     */
    @Transactional
    public SolicitudTransferenciaDTO procesarIngreso(Long solicitudId, Long clubOwnerId, boolean aprobar, String motivo) {
        SolicitudTransferencia solicitud = solicitudTransferenciaRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Validar estado
        if (!solicitud.isPendienteIngreso()) {
            throw new RuntimeException("Esta solicitud no está pendiente de ingreso");
        }

        // Validar que es el owner del club destino
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!solicitud.getClubDestino().getOwner().getId().equals(clubOwnerId)) {
            throw new RuntimeException("No eres el dueño del club de destino");
        }

        if (aprobar) {
            // ✅ Ejecutar ingreso (funciona para INGRESO_NUEVO y TRANSFERENCIA)
            ejecutarIngreso(solicitud, clubOwner);
            
        } else {
            solicitud.setEstado("RECHAZADA_INGRESO");
            solicitud.setMotivoRechazo(motivo != null ? motivo : "El club destino rechazó la solicitud");
            solicitud.setFechaRechazo(LocalDateTime.now());
            solicitud.setRechazadoPor(clubOwner);
        }

        SolicitudTransferencia saved = solicitudTransferenciaRepository.save(solicitud);
        return convertToDTO(saved);
    }

    /**
     * ✅ MODIFICADO: Ahora maneja usuarios sin club (INGRESO_NUEVO)
     */
    private void ejecutarIngreso(SolicitudTransferencia solicitud, User clubOwner) {
        User usuario = solicitud.getUsuario();
        Club clubDestino = solicitud.getClubDestino();

        // Validar cupos nuevamente
        if (clubDestino.isFull()) {
            throw new RuntimeException("El club destino ya no tiene cupos disponibles");
        }

        // ✅ Asignar club al usuario
        usuario.setClub(clubDestino);

        // ✅ NUEVO: Agregar rol COMPETITOR si no lo tiene
        boolean tieneCompetitorRole = usuario.getRoles().stream()
                .anyMatch(role -> "ROLE_COMPETITOR".equals(role.getNombre()));

        if (!tieneCompetitorRole) {
            Role competitorRole = roleRepository.findByNombre("ROLE_COMPETITOR")
                    .orElseThrow(() -> new RuntimeException("Rol COMPETITOR no encontrado"));
            usuario.getRoles().add(competitorRole);
            
            System.out.println("✅ Rol COMPETITOR restaurado para: " + usuario.getEmail());
        }

        usuario.setEstado("APROBADO");
        userRepository.save(usuario);

        // ✅ Marcar solicitud como APROBADA
        solicitud.setEstado("APROBADA");
        solicitud.setFechaAprobacionIngreso(LocalDateTime.now());
        solicitud.setAprobadoIngresoPor(clubOwner);

        String tipoAccion = "INGRESO_NUEVO".equals(solicitud.getTipoSolicitud()) 
                ? "INGRESO NUEVO" 
                : "TRANSFERENCIA";

        System.out.println("✅ " + tipoAccion + " COMPLETADA:");
        System.out.println("   Usuario: " + usuario.getEmail());
        if (solicitud.getClubOrigen() != null) {
            System.out.println("   De: " + solicitud.getClubOrigen().getNombre());
        }
        System.out.println("   A: " + clubDestino.getNombre());
    }

    /**
     * 📋 LISTAR SOLICITUDES PENDIENTES DE SALIDA (sin cambios)
     */
    public List<SolicitudTransferenciaDTO> getSolicitudesPendientesSalida(Long clubOwnerId) {
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Club club = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));

        List<SolicitudTransferencia> solicitudes = solicitudTransferenciaRepository
                .findPendientesSalidaByClubOrigen(club.getId());

        return solicitudes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ MODIFICADO: Ahora incluye INGRESO_NUEVO y TRANSFERENCIA
     */
    public List<SolicitudTransferenciaDTO> getSolicitudesPendientesIngreso(Long clubOwnerId) {
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Club club = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));

        List<SolicitudTransferencia> solicitudes = solicitudTransferenciaRepository
                .findPendientesIngresoByClubDestino(club.getId());

        return solicitudes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 📋 LISTAR MIS SOLICITUDES (sin cambios)
     */
    public List<SolicitudTransferenciaDTO> getMisSolicitudes(Long usuarioId) {
        List<SolicitudTransferencia> solicitudes = solicitudTransferenciaRepository
                .findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId);

        return solicitudes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ❌ CANCELAR SOLICITUD (sin cambios)
     */
    @Transactional
    public void cancelarSolicitud(Long solicitudId, Long usuarioId) {
        SolicitudTransferencia solicitud = solicitudTransferenciaRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!solicitud.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("No puedes cancelar esta solicitud");
        }

        if (solicitud.isAprobada() || solicitud.isRechazada()) {
            throw new RuntimeException("No puedes cancelar una solicitud ya procesada");
        }

        solicitud.setEstado("CANCELADA");
        solicitud.setFechaRechazo(LocalDateTime.now());
        solicitudTransferenciaRepository.save(solicitud);
    }

    /**
     * ✅ MODIFICADO: Incluye tipo de solicitud en DTO
     */
    private SolicitudTransferenciaDTO convertToDTO(SolicitudTransferencia s) {
        SolicitudTransferenciaDTO dto = new SolicitudTransferenciaDTO();
        
        dto.setId(s.getId());
        dto.setEstado(s.getEstado());
        dto.setMensajeUsuario(s.getMensajeUsuario());
        dto.setMotivoRechazo(s.getMotivoRechazo());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaAprobacionSalida(s.getFechaAprobacionSalida());
        dto.setFechaAprobacionIngreso(s.getFechaAprobacionIngreso());
        dto.setFechaRechazo(s.getFechaRechazo());

        // Usuario
        if (s.getUsuario() != null) {
            dto.setUsuarioId(s.getUsuario().getId());
            dto.setUsuarioNombre(s.getUsuario().getNombre() + " " + s.getUsuario().getApellido());
            dto.setUsuarioEmail(s.getUsuario().getEmail());
            dto.setUsuarioDni(s.getUsuario().getDni());
            dto.setUsuarioFoto(s.getUsuario().getFotoPerfil());
            
            List<Robot> robots = robotRepository.findByUsuarioId(s.getUsuario().getId());
            dto.setRobotsDelUsuario(robots.size());
        }

        // Club Origen (puede ser null para INGRESO_NUEVO)
        if (s.getClubOrigen() != null) {
            dto.setClubOrigenId(s.getClubOrigen().getId());
            dto.setClubOrigenNombre(s.getClubOrigen().getNombre());
            dto.setClubOrigenCiudad(s.getClubOrigen().getCiudad());
        }

        // Club Destino
        if (s.getClubDestino() != null) {
            dto.setClubDestinoId(s.getClubDestino().getId());
            dto.setClubDestinoNombre(s.getClubDestino().getNombre());
            dto.setClubDestinoCiudad(s.getClubDestino().getCiudad());
            dto.setCuposDisponiblesDestino(s.getClubDestino().getCuposDisponibles());
        }

        // Aprobadores
        if (s.getAprobadoSalidaPor() != null) {
            dto.setAprobadoSalidaPorNombre(
                s.getAprobadoSalidaPor().getNombre() + " " + s.getAprobadoSalidaPor().getApellido());
        }

        if (s.getAprobadoIngresoPor() != null) {
            dto.setAprobadoIngresoPorNombre(
                s.getAprobadoIngresoPor().getNombre() + " " + s.getAprobadoIngresoPor().getApellido());
        }

        if (s.getRechazadoPor() != null) {
            dto.setRechazadoPorNombre(
                s.getRechazadoPor().getNombre() + " " + s.getRechazadoPor().getApellido());
        }

        return dto;
    }
}