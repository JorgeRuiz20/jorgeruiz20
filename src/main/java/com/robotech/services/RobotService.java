package com.robotech.services;

import com.robotech.controllers.RobotController;
import com.robotech.models.*;
import com.robotech.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RobotService {
    private final RobotRepository robotRepository;
    private final UserRepository userRepository;
    private final CategoriaRepository categoriaRepository;
    private final ClubRepository clubRepository;
    private final SimilarityService similarityService;

    @Transactional
    public Robot registrarRobot(Robot robot, Long usuarioId, Long categoriaId) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        if (!"APROBADO".equals(usuario.getEstado())) {
            throw new RuntimeException("Tu cuenta debe estar aprobada para registrar robots");
        }
        
        if (usuario.getClub() == null) {
            throw new RuntimeException("Debes pertenecer a un club para registrar robots");
        }
        
        // ✅ Máximo 5 robots por usuario
        long cantidadRobots = robotRepository.findByUsuarioId(usuarioId).size();
        if (cantidadRobots >= 5) {
            throw new RuntimeException("Has alcanzado el límite máximo de 5 robots por competidor");
        }
        
        // Verificar peso
        if (robot.getPeso() > categoria.getPesoMaximo()) {
            throw new RuntimeException("El robot excede el peso máximo permitido para esta categoría: " + 
                categoria.getPesoMaximo() + "g");
        }
        
        // ✅ Validación de similitud de nombres de robot (umbral 80% - moderado)
        String nombreNuevo = robot.getNombre();
        List<String> nombresExistentes = robotRepository.findByUsuarioId(usuarioId).stream()
                .map(Robot::getNombre)
                .collect(Collectors.toList());
        
        if (similarityService.existeRobotSimilar(nombreNuevo, nombresExistentes)) {
            String similar = similarityService.encontrarRobotSimilar(nombreNuevo, nombresExistentes);
            throw new RuntimeException("Ya tienes un robot con nombre similar: '" + similar + "'");
        }
        
        robot.setUsuario(usuario);
        robot.setCategoria(categoria);
        robot.setCodigoIdentificacion(generarCodigoRobot());
        robot.setEstado("PENDIENTE");
        
        return robotRepository.save(robot);
    }

    @Transactional
    public Robot actualizarRobot(Long robotId, Long usuarioId, RobotController.ActualizarRobotRequest request) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));
        
        if (!robot.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("Solo puedes actualizar tus propios robots");
        }
        
        // Actualizar nombre con validación
        if (request.getNombreRobot() != null && !request.getNombreRobot().isEmpty()) {
            List<String> nombresExistentes = robotRepository.findByUsuarioId(usuarioId).stream()
                    .filter(r -> !r.getId().equals(robotId))
                    .map(Robot::getNombre)
                    .collect(Collectors.toList());
            
            if (similarityService.existeRobotSimilar(request.getNombreRobot(), nombresExistentes)) {
                String similar = similarityService.encontrarRobotSimilar(request.getNombreRobot(), nombresExistentes);
                throw new RuntimeException("Ya tienes otro robot con nombre similar: '" + similar + "'");
            }
            robot.setNombre(request.getNombreRobot());
        }
        
        if (request.getDescripcionRobot() != null) {
            robot.setDescripcion(request.getDescripcionRobot());
        }
        
        if (request.getFotoRobot() != null) {
            robot.setFotoRobot(request.getFotoRobot());
        }
        
        if (request.getEspecificacionesTecnicas() != null) {
            robot.setEspecificacionesTecnicas(request.getEspecificacionesTecnicas());
        }
        
        if (request.getPeso() != null) {
            if (request.getPeso() > robot.getCategoria().getPesoMaximo()) {
                throw new RuntimeException("El peso excede el máximo permitido para la categoría: " + 
                    robot.getCategoria().getPesoMaximo() + "g");
            }
            robot.setPeso(request.getPeso());
        }
        
        if (request.getCategoriaId() != null && !request.getCategoriaId().equals(robot.getCategoria().getId())) {
            Categoria nuevaCategoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            if (robot.getPeso() > nuevaCategoria.getPesoMaximo()) {
                throw new RuntimeException("El peso del robot excede el máximo de la nueva categoría: " + 
                    nuevaCategoria.getPesoMaximo() + "g");
            }
            
            robot.setCategoria(nuevaCategoria);
            robot.setEstado("PENDIENTE");
        }
        
        return robotRepository.save(robot);
    }

    public List<Robot> getRobotsByUsuario(Long usuarioId) {
        return robotRepository.findByUsuarioId(usuarioId);
    }

    public List<Robot> getRobotsAprobadosByCategoria(Long categoriaId) {
        return robotRepository.findByCategoriaIdAndEstado(categoriaId, "APROBADO");
    }

    public List<Robot> getRobotsByClubOwner(Long clubOwnerId) {
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esClubOwner = clubOwner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!esClubOwner) {
            throw new RuntimeException("El usuario no tiene permisos de club owner");
        }
        
        Club club = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No se encontró club para este owner"));
        
        List<User> miembrosClub = userRepository.findByClubIdAndEstado(club.getId(), "APROBADO");
        
        return miembrosClub.stream()
                .flatMap(miembro -> robotRepository.findByUsuarioId(miembro.getId()).stream())
                .filter(robot -> "APROBADO".equals(robot.getEstado()))
                .collect(Collectors.toList());
    }

    public List<Robot> getRobotsPendientesByClubOwner(Long clubOwnerId) {
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esClubOwner = clubOwner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!esClubOwner) {
            throw new RuntimeException("El usuario no tiene permisos de club owner");
        }
        
        Club club = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No se encontró club para este owner"));
        
        return robotRepository.findRobotsPendientesByClub(club.getId());
    }

    @Transactional
    public Robot aprobarRobot(Long robotId, Long clubOwnerId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));
        
        if (!"PENDIENTE".equals(robot.getEstado())) {
            throw new RuntimeException("El robot no está en estado pendiente");
        }
        
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esClubOwner = clubOwner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!esClubOwner) {
            throw new RuntimeException("No tienes permisos de club owner");
        }
        
        Club clubDelOwner = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No se encontró club para este owner"));
        
        Club clubDelUsuario = robot.getUsuario().getClub();
        
        if (clubDelUsuario == null) {
            throw new RuntimeException("El usuario del robot no pertenece a ningún club");
        }
        
        if (!clubDelUsuario.getId().equals(clubDelOwner.getId())) {
            throw new RuntimeException("No tienes permisos para aprobar robots de este club");
        }
        
        robot.setEstado("APROBADO");
        return robotRepository.save(robot);
    }

 // SOLO LA FUNCIÓN rechazarRobot actualizada - reemplazar en tu RobotService.java

    @Transactional
    public Robot rechazarRobot(Long robotId, Long clubOwnerId, String motivo) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));
        
        if (!"PENDIENTE".equals(robot.getEstado())) {
            throw new RuntimeException("El robot no está en estado pendiente");
        }
        
        User clubOwner = userRepository.findById(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esClubOwner = clubOwner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!esClubOwner) {
            throw new RuntimeException("No tienes permisos de club owner");
        }
        
        Club clubDelOwner = clubRepository.findByOwnerId(clubOwnerId)
                .orElseThrow(() -> new RuntimeException("No se encontró club para este owner"));
        
        Club clubDelUsuario = robot.getUsuario().getClub();
        
        if (clubDelUsuario == null) {
            throw new RuntimeException("El usuario del robot no pertenece a ningún club");
        }
        
        if (!clubDelUsuario.getId().equals(clubDelOwner.getId())) {
            throw new RuntimeException("No tienes permisos para rechazar robots de este club");
        }
        
        // ✅ GUARDAR MOTIVO DE RECHAZO
        robot.setEstado("RECHAZADO");
        robot.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo : "Sin motivo especificado");
        
        return robotRepository.save(robot);
    }

    @Transactional
    public void eliminarRobot(Long robotId, Long usuarioId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));
        
        if (!robot.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("Solo puedes eliminar tus propios robots");
        }
        
        robotRepository.delete(robot);
    }

    public Robot getRobotById(Long robotId) {
        return robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));
    }

    private String generarCodigoRobot() {
        String codigo;
        do {
            codigo = "ROB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (robotRepository.findByCodigoIdentificacion(codigo).isPresent());
        return codigo;
    }
}