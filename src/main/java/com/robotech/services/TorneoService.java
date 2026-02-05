package com.robotech.services;

import com.robotech.dto.*;
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
public class TorneoService {

    private final TorneoRepository torneoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ParticipanteRepository participanteRepository;
    private final UserRepository userRepository;
    private final RobotRepository robotRepository;
    private final EnfrentamientoRepository enfrentamientoRepository;
    private final HistorialTorneoRepository historialTorneoRepository;
    private final SimilarityService similarityService;
    private final SedeRepository sedeRepository;
    private final RankingService rankingService; // ✅ NUEVO: Para limpieza de caché

    // ==================== CRUD BÁSICO ====================

    public List<Torneo> getAllTorneos() {
        return torneoRepository.findAll();
    }

    public List<Torneo> getTorneosActivos() {
        return torneoRepository.findByEstado("ACTIVO");
    }

    public List<Torneo> getTorneosPendientes() {
        return torneoRepository.findByEstado("PENDIENTE");
    }

    public List<Torneo> getTorneosFinalizados() {
        return torneoRepository.findByEstado("FINALIZADO");
    }

    public Optional<Torneo> getTorneoById(Long id) {
        return torneoRepository.findById(id);
    }

    /**
     * ✅ NUEVO MÉTODO: Obtener torneo por ID
     * Necesario para validar el estado en el endpoint de ranking
     */
    public Torneo obtenerTorneoPorId(Long torneoId) {
        return torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado con ID: " + torneoId));
    }

    @Transactional
    public Torneo createTorneo(CreateTorneoRequest request) {
        // Validar categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        if (!categoria.getActiva()) {
            throw new RuntimeException("La categoría no está activa");
        }

        // Validar sede
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        if (!sede.getActiva()) {
            throw new RuntimeException("La sede no está activa");
        }

        // ✅ NUEVO: Validar juez responsable
        User juez = userRepository.findById(request.getJuezResponsableId())
                .orElseThrow(() -> new RuntimeException("Juez no encontrado"));

        boolean esJuez = juez.getRoles().stream()
                .anyMatch(role -> "ROLE_JUDGE".equals(role.getNombre()));

        if (!esJuez) {
            throw new RuntimeException("El usuario seleccionado no tiene el rol de JUEZ");
        }

        // Validar similitud de nombres
        String nombreNuevo = request.getNombre();
        if (nombreNuevo == null || nombreNuevo.isBlank()) {
            throw new RuntimeException("El nombre del torneo no puede estar vacío");
        }

        List<String> nombresExistentes = torneoRepository.findAll().stream()
                .map(Torneo::getNombre)
                .collect(Collectors.toList());

        if (similarityService.existeTorneoSimilar(nombreNuevo, nombresExistentes)) {
            String similar = similarityService.encontrarTorneoSimilar(nombreNuevo, nombresExistentes);
            throw new RuntimeException("Ya existe un torneo con nombre similar: '" + similar + "'");
        }

        // Validar fecha de activación si es automática
        if (Boolean.TRUE.equals(request.getActivacionAutomatica())) {
            if (request.getFechaActivacionProgramada() == null) {
                throw new RuntimeException("Debes especificar la fecha de activación automática");
            }

            if (request.getFechaActivacionProgramada().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("La fecha de activación debe ser futura");
            }
        }

        Torneo torneo = new Torneo();
        torneo.setNombre(request.getNombre());
        torneo.setDescripcion(request.getDescripcion());
        torneo.setSede(sede);
        torneo.setCategoria(categoria);
        torneo.setEstado("PENDIENTE");
        torneo.setModalidad(null);

        // ✅ NUEVO: Asignar juez desde la creación
        torneo.setJuezResponsable(juez);
        torneo.setFaseActual(null);

        // Configurar programación automática
        torneo.setFechaActivacionProgramada(request.getFechaActivacionProgramada());
        torneo.setActivacionAutomatica(
                Boolean.TRUE.equals(request.getActivacionAutomatica()) &&
                        request.getFechaActivacionProgramada() != null);

        Torneo saved = torneoRepository.save(torneo);

        System.out.println("✅ Torneo creado con juez asignado:");
        System.out.println("   - Torneo: " + saved.getNombre());
        System.out.println("   - Juez: " + juez.getNombre() + " " + juez.getApellido());

        return saved;
    }

    @Transactional
    public Torneo updateTorneo(Long id, CreateTorneoRequest request) {
        Torneo existing = torneoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (request.getCategoriaId() != null &&
                !request.getCategoriaId().equals(existing.getCategoria().getId())) {

            Long participantes = participanteRepository.countByTorneoId(id);
            if (participantes > 0) {
                throw new RuntimeException("No se puede cambiar la categoría de un torneo con participantes");
            }

            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            if (!categoria.getActiva()) {
                throw new RuntimeException("La categoría no está activa");
            }

            existing.setCategoria(categoria);
        }

        // Actualizar sede
        if (request.getSedeId() != null) {
            Sede sede = sedeRepository.findById(request.getSedeId())
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

            if (!sede.getActiva()) {
                throw new RuntimeException("La sede no está activa");
            }

            existing.setSede(sede);
        }

        // ✅ NUEVO: Actualizar juez responsable
        if (request.getJuezResponsableId() != null) {
            User juez = userRepository.findById(request.getJuezResponsableId())
                    .orElseThrow(() -> new RuntimeException("Juez no encontrado"));

            boolean esJuez = juez.getRoles().stream()
                    .anyMatch(role -> "ROLE_JUDGE".equals(role.getNombre()));

            if (!esJuez) {
                throw new RuntimeException("El usuario seleccionado no tiene el rol de JUEZ");
            }

            // ⚠️ Solo permitir cambio si el torneo está PENDIENTE
            if (!"PENDIENTE".equals(existing.getEstado())) {
                throw new RuntimeException("No se puede cambiar el juez de un torneo ya iniciado");
            }

            existing.setJuezResponsable(juez);

            System.out.println("🔄 Juez actualizado:");
            System.out.println("   - Torneo: " + existing.getNombre());
            System.out.println("   - Nuevo juez: " + juez.getNombre() + " " + juez.getApellido());
        }

        // Validar similitud si se cambia el nombre
        if (request.getNombre() != null && !request.getNombre().isBlank() &&
                !request.getNombre().equals(existing.getNombre())) {

            List<String> nombresExistentes = torneoRepository.findAll().stream()
                    .filter(t -> !t.getId().equals(id))
                    .map(Torneo::getNombre)
                    .collect(Collectors.toList());

            if (similarityService.existeTorneoSimilar(request.getNombre(), nombresExistentes)) {
                String similar = similarityService.encontrarTorneoSimilar(request.getNombre(), nombresExistentes);
                throw new RuntimeException("Ya existe un torneo con nombre similar: '" + similar + "'");
            }

            existing.setNombre(request.getNombre());
        }

        if (request.getDescripcion() != null)
            existing.setDescripcion(request.getDescripcion());

        // Actualizar programación automática
        if (request.getFechaActivacionProgramada() != null) {
            if (request.getFechaActivacionProgramada().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("La fecha de activación debe ser futura");
            }
            existing.setFechaActivacionProgramada(request.getFechaActivacionProgramada());
        }

        if (request.getActivacionAutomatica() != null) {
            existing.setActivacionAutomatica(
                    Boolean.TRUE.equals(request.getActivacionAutomatica()) &&
                            existing.getFechaActivacionProgramada() != null);
        }

        return torneoRepository.save(existing);
    }

    /**
     * ✅ MÉTODO MODIFICADO: Cambiar estado del torneo
     * Ahora limpia el caché del ranking cuando el torneo finaliza
     */
    @Transactional
    public Torneo cambiarEstadoTorneo(Long id, String nuevoEstado) {
        Torneo torneo = torneoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (!List.of("PENDIENTE", "ACTIVO", "FINALIZADO").contains(nuevoEstado)) {
            throw new RuntimeException("Estado inválido");
        }

        String estadoAnterior = torneo.getEstado();
        torneo.setEstado(nuevoEstado);

        // Si cambia a ACTIVO, establecer fecha de inicio
        if ("ACTIVO".equals(nuevoEstado) && torneo.getFechaInicio() == null) {
            torneo.setFechaInicio(LocalDateTime.now());
        }

        // ✅ NUEVO: Si cambia a FINALIZADO, establecer fecha fin Y limpiar caché
        if ("FINALIZADO".equals(nuevoEstado)) {
            torneo.setFechaFin(LocalDateTime.now());

            // Limpiar caché del ranking para evitar mostrar datos en tiempo real
            // de un torneo que ya finalizó
            rankingService.limpiarCacheRanking(id);

            System.out.println("🗑️ Caché de ranking limpiado para torneo finalizado ID: " + id);
        }

        Torneo actualizado = torneoRepository.save(torneo);

        System.out.println("🔄 Estado de torneo actualizado:");
        System.out.println("   - Torneo: " + actualizado.getNombre());
        System.out.println("   - Estado anterior: " + estadoAnterior);
        System.out.println("   - Estado nuevo: " + nuevoEstado);

        return actualizado;
    }

    @Transactional
    public void deleteTorneo(Long id) {
        Torneo torneo = torneoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (!"PENDIENTE".equals(torneo.getEstado())) {
            throw new RuntimeException("Solo se pueden eliminar torneos PENDIENTES");
        }

        Long participantes = participanteRepository.countByTorneoId(id);
        if (participantes > 0) {
            throw new RuntimeException("No se puede eliminar un torneo con participantes");
        }

        torneoRepository.delete(torneo);
    }

    public List<Torneo> getTorneosByCategoria(Long categoriaId) {
        return torneoRepository.findByCategoriaId(categoriaId);
    }

    // ============ INSCRIPCIÓN ====================

    @Transactional
    public Participante unirseATorneo(Long torneoId, Long usuarioId, Long robotId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        // ✅ CORRECCIÓN: Permitir inscripción en PENDIENTE y ACTIVO
        if ("FINALIZADO".equals(torneo.getEstado())) {
            throw new RuntimeException("No puedes unirte a un torneo finalizado");
        }

        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Robot no encontrado"));

        // Verificar que el robot pertenece al usuario
        if (!robot.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("El robot no te pertenece");
        }

        // Verificar que el robot está aprobado
        if (!"APROBADO".equals(robot.getEstado())) {
            throw new RuntimeException("El robot debe estar aprobado para participar");
        }

        // Verificar categoría del robot vs torneo
        if (!robot.getCategoria().getId().equals(torneo.getCategoria().getId())) {
            throw new RuntimeException("El robot no pertenece a la categoría del torneo");
        }

        // Verificar si ya está inscrito
        if (participanteRepository.findByUsuarioIdAndTorneoId(usuarioId, torneoId).isPresent()) {
            throw new RuntimeException("Ya estás inscrito en este torneo");
        }

        // ✅ VALIDACIÓN ADICIONAL: Si el torneo ya inició, validar si aún se permite
        // inscripción
        if ("ACTIVO".equals(torneo.getEstado()) && torneo.getFechaInicio() != null) {
            // Opcional: Puedes agregar lógica para limitar inscripciones después de X
            // tiempo
            // Por ahora, permitimos inscripción mientras esté ACTIVO
            System.out.println("⚠️ Inscripción en torneo ACTIVO - Usuario: " + usuario.getEmail());
        }

        // Crear participante
        Participante participante = new Participante();
        participante.setUsuario(usuario);
        participante.setTorneo(torneo);
        participante.setRobot(robot);
        participante.setNombreRobot(robot.getNombre());
        participante.setDescripcionRobot(robot.getDescripcion());
        participante.setFotoRobot(robot.getFotoRobot());
        participante.setPuntuacionTotal(0);
        participante.setPartidosGanados(0);
        participante.setPartidosPerdidos(0);
        participante.setPartidosEmpatados(0);

        Participante savedParticipante = participanteRepository.save(participante);

        System.out.println("✅ Participante inscrito exitosamente:");
        System.out.println("   - Torneo: " + torneo.getNombre());
        System.out.println("   - Usuario: " + usuario.getEmail());
        System.out.println("   - Robot: " + robot.getNombre());

        return savedParticipante;
    }

    public List<Participante> getParticipantesByTorneo(Long torneoId) {
        torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        return participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(torneoId);
    }

    public Long countParticipantes(Long torneoId) {
        return participanteRepository.countByTorneoId(torneoId);
    }

    public boolean isUsuarioInscrito(Long torneoId, Long usuarioId) {
        return participanteRepository.findByUsuarioIdAndTorneoId(usuarioId, torneoId).isPresent();
    }

    // ==================== MODALIDADES Y JUEZ ====================

    /**
     * ✅ MODIFICADO: Ya no asigna juez (se asignó al crear)
     * Solo cambia modalidad si el juez correcto lo solicita
     */
    @Transactional
    public Torneo asignarModalidad(Long torneoId, String modalidad, Long juezId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (!"ACTIVO".equals(torneo.getEstado())) {
            throw new RuntimeException("El torneo debe estar ACTIVO (iniciado por el admin) para asignar modalidad");
        }

        if ("FINALIZADO".equals(torneo.getEstado())) {
            throw new RuntimeException("No se puede cambiar la modalidad de un torneo finalizado");
        }

        if (!List.of("ELIMINATORIA", "TODOS_CONTRA_TODOS").contains(modalidad)) {
            throw new RuntimeException("Modalidad inválida. Usa: ELIMINATORIA o TODOS_CONTRA_TODOS");
        }

        // ✅ NUEVA VALIDACIÓN: Verificar que el juez solicitante es el asignado
        if (torneo.getJuezResponsable() == null) {
            throw new RuntimeException("Este torneo no tiene juez asignado. Contacta al administrador");
        }

        if (!torneo.getJuezResponsable().getId().equals(juezId)) {
            throw new RuntimeException(
                    "No eres el juez asignado a este torneo. " +
                            "Juez responsable: " + torneo.getJuezResponsable().getNombre() +
                            " " + torneo.getJuezResponsable().getApellido());
        }

        torneo.setModalidad(modalidad);

        System.out.println("✅ Modalidad asignada por juez autorizado:");
        System.out.println("   - Torneo: " + torneo.getNombre());
        System.out.println("   - Modalidad: " + modalidad);
        System.out.println("   - Juez: " + torneo.getJuezResponsable().getEmail());

        return torneoRepository.save(torneo);
    }

    /**
     * ✅ NUEVO: Solo ADMIN inicia el torneo (lo pone en ACTIVO)
     */
    @Transactional
    public Map<String, Object> iniciarTorneo(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if ("FINALIZADO".equals(torneo.getEstado())) {
            throw new RuntimeException("El torneo ya finalizó");
        }

        if ("ACTIVO".equals(torneo.getEstado())) {
            throw new RuntimeException("El torneo ya está activo");
        }

        List<Participante> participantes = participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(torneoId);

        if (participantes.size() < 2) {
            throw new RuntimeException("Se necesitan al menos 2 participantes para iniciar el torneo");
        }

        // ✅ Cambiar estado a ACTIVO
        torneo.setEstado("ACTIVO");
        torneo.setFechaInicio(LocalDateTime.now());
        torneoRepository.save(torneo);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Torneo iniciado exitosamente. Esperando que el juez asigne modalidad.");
        resultado.put("estado", "ACTIVO");
        resultado.put("participantes", participantes.size());
        resultado.put("advertencia", "El juez debe asignar la modalidad y generar enfrentamientos");

        return resultado;
    }

    // ✅ MÉTODO CORREGIDO: Generar enfrentamientos con validaciones mejoradas
    @Transactional
    public Map<String, Object> generarEnfrentamientos(Long torneoId, Long juezId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (!"ACTIVO".equals(torneo.getEstado())) {
            throw new RuntimeException("El torneo debe estar ACTIVO");
        }

        if (torneo.getModalidad() == null) {
            throw new RuntimeException("Debes asignar una modalidad antes de generar enfrentamientos");
        }

        // ✅ VALIDACIÓN REFORZADA: Solo el juez asignado puede generar enfrentamientos
        if (torneo.getJuezResponsable() == null) {
            throw new RuntimeException("Este torneo no tiene juez asignado. Contacta al administrador");
        }

        if (!torneo.getJuezResponsable().getId().equals(juezId)) {
            throw new RuntimeException(
                    "No eres el juez asignado a este torneo. " +
                            "Juez responsable: " + torneo.getJuezResponsable().getNombre() +
                            " " + torneo.getJuezResponsable().getApellido());
        }

        List<Participante> participantes = participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(torneoId);

        // ✅ VALIDACIÓN 1: Mínimo 2 participantes
        if (participantes.size() < 2) {
            throw new RuntimeException("Se necesitan al menos 2 participantes para iniciar el torneo");
        }

        // ✅ VALIDACIÓN 2: Solo para ELIMINATORIA - número PAR y máximo 16
        if ("ELIMINATORIA".equals(torneo.getModalidad())) {
            int cantidad = participantes.size();

            // Validar que sea número PAR
            if (cantidad % 2 != 0) {
                throw new RuntimeException(
                        "Para modalidad ELIMINATORIA se requiere un número PAR de participantes. " +
                                "Actualmente hay " + cantidad + " participantes. " +
                                "Por favor inscribe o elimina 1 participante para continuar.");
            }

            // Validar que sea potencia de 2 (2, 4, 8, 16)
            if (!esPotenciaDeDos(cantidad)) {
                throw new RuntimeException(
                        "Para modalidad ELIMINATORIA el número de participantes debe ser 2, 4, 8 o 16. " +
                                "Actualmente hay " + cantidad + " participantes.");
            }

            // Validar límite máximo de 16
            if (cantidad > 16) {
                throw new RuntimeException(
                        "El límite máximo para torneos ELIMINATORIA es 16 participantes. " +
                                "Actualmente hay " + cantidad + " participantes.");
            }
        }

        // Limpiar enfrentamientos anteriores si existen
        List<Enfrentamiento> enfrentamientosAnteriores = enfrentamientoRepository.findByTorneoId(torneoId);
        if (!enfrentamientosAnteriores.isEmpty()) {
            enfrentamientoRepository.deleteAll(enfrentamientosAnteriores);
        }

        // Generar enfrentamientos según modalidad
        if ("ELIMINATORIA".equals(torneo.getModalidad())) {
            generarEnfrentamientosEliminatoria(torneo, participantes);
        } else if ("TODOS_CONTRA_TODOS".equals(torneo.getModalidad())) {
            generarEnfrentamientosTodosContraTodos(torneo, participantes);
        }

        torneoRepository.save(torneo);

        System.out.println("✅ Enfrentamientos generados por juez autorizado:");
        System.out.println("   - Torneo: " + torneo.getNombre());
        System.out.println("   - Juez: " + torneo.getJuezResponsable().getEmail());
        System.out.println("   - Enfrentamientos: " + enfrentamientoRepository.findByTorneoId(torneoId).size());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Enfrentamientos generados exitosamente");
        resultado.put("modalidad", torneo.getModalidad());
        resultado.put("participantes", participantes.size());
        resultado.put("enfrentamientos", enfrentamientoRepository.findByTorneoId(torneoId).size());
        resultado.put("faseActual", torneo.getFaseActual());
        resultado.put("juezResponsable", torneo.getJuezResponsable().getNombre() + " " +
                torneo.getJuezResponsable().getApellido());

        return resultado;
    }

    // ✅ MÉTODO AUXILIAR: Verificar si un número es potencia de 2
    private boolean esPotenciaDeDos(int numero) {
        return numero > 0 && (numero & (numero - 1)) == 0;
    }

    // ✅ MÉTODO CORREGIDO: Generar enfrentamientos eliminatoria
    private void generarEnfrentamientosEliminatoria(Torneo torneo, List<Participante> participantes) {
        // NO mezclar aleatoriamente - mantener orden de ranking/inscripción
        // Collections.shuffle(participantes); // ❌ ELIMINADO

        int cantidadParticipantes = participantes.size();

        // Determinar fase inicial según cantidad exacta
        String faseInicial = determinarFaseInicial(cantidadParticipantes);
        torneo.setFaseActual(faseInicial);

        System.out.println("🏆 Generando bracket ELIMINATORIA");
        System.out.println("   - Participantes: " + cantidadParticipantes);
        System.out.println("   - Fase inicial: " + faseInicial);
        System.out.println("   - Enfrentamientos: " + (cantidadParticipantes / 2));

        // Generar enfrentamientos de la primera fase
        for (int i = 0; i < cantidadParticipantes; i += 2) {
            Enfrentamiento enfrentamiento = new Enfrentamiento();
            enfrentamiento.setParticipante1(participantes.get(i));
            enfrentamiento.setParticipante2(participantes.get(i + 1));
            enfrentamiento.setTorneo(torneo);
            enfrentamiento.setRonda(faseInicial);
            enfrentamiento.setResultado("PENDIENTE");
            enfrentamiento.setPuntosParticipante1(0);
            enfrentamiento.setPuntosParticipante2(0);
            enfrentamiento.setFechaEnfrentamiento(LocalDateTime.now());

            enfrentamientoRepository.save(enfrentamiento);

            System.out.println("   ✅ " + participantes.get(i).getNombreRobot() +
                    " vs " + participantes.get(i + 1).getNombreRobot());
        }
    }

    // ✅ MÉTODO CORREGIDO: Determinar fase inicial con números exactos
    private String determinarFaseInicial(int cantidadParticipantes) {
        switch (cantidadParticipantes) {
            case 16:
                return "OCTAVOS";
            case 8:
                return "CUARTOS";
            case 4:
                return "SEMIFINAL";
            case 2:
                return "FINAL";
            default:
                throw new RuntimeException("Número de participantes inválido: " + cantidadParticipantes);
        }
    }

    // Método sin cambios - Todos contra todos
    private void generarEnfrentamientosTodosContraTodos(Torneo torneo, List<Participante> participantes) {
        torneo.setFaseActual("LIGA");

        for (int i = 0; i < participantes.size(); i++) {
            for (int j = i + 1; j < participantes.size(); j++) {
                Enfrentamiento enfrentamiento = new Enfrentamiento();
                enfrentamiento.setParticipante1(participantes.get(i));
                enfrentamiento.setParticipante2(participantes.get(j));
                enfrentamiento.setTorneo(torneo);
                enfrentamiento.setRonda("LIGA");
                enfrentamiento.setResultado("PENDIENTE");
                enfrentamiento.setFechaEnfrentamiento(LocalDateTime.now().plusDays(1));

                enfrentamientoRepository.save(enfrentamiento);
            }
        }
    }

    @Transactional
    public Map<String, Object> obtenerEstadoBracket(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        List<Enfrentamiento> enfrentamientos = enfrentamientoRepository.findByTorneoId(torneoId);

        // Agrupar por ronda
        Map<String, List<EnfrentamientoDTO>> bracket = enfrentamientos.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getRonda(),
                        Collectors.mapping(this::convertToEnfrentamientoDTO, Collectors.toList())));

        // Ordenar las rondas
        List<String> ordenRondas = Arrays.asList("OCTAVOS", "CUARTOS", "SEMIFINAL", "FINAL", "LIGA");
        Map<String, List<EnfrentamientoDTO>> bracketOrdenado = new LinkedHashMap<>();
        for (String ronda : ordenRondas) {
            if (bracket.containsKey(ronda)) {
                bracketOrdenado.put(ronda, bracket.get(ronda));
            }
        }

        return Map.of(
                "torneo", convertToDTO(torneo),
                "bracket", bracketOrdenado,
                "totalEnfrentamientos", enfrentamientos.size(),
                "completados", enfrentamientos.stream().filter(e -> !"PENDIENTE".equals(e.getResultado())).count());
    }

    // ✅ MÉTODO CORREGIDO: Registrar resultado - rechazar empates en eliminatorias
    @Transactional
    public EnfrentamientoDTO registrarResultado(Long torneoId, Long enfrentamientoId,
            Integer puntos1, Integer puntos2, Long juezId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        if (torneo.getJuezResponsable() == null || !torneo.getJuezResponsable().getId().equals(juezId)) {
            throw new RuntimeException("No eres el juez responsable de este torneo");
        }

        Enfrentamiento enfrentamiento = enfrentamientoRepository.findById(enfrentamientoId)
                .orElseThrow(() -> new RuntimeException("Enfrentamiento no encontrado"));

        if (!"PENDIENTE".equals(enfrentamiento.getResultado())) {
            throw new RuntimeException("Este enfrentamiento ya tiene un resultado registrado");
        }

        // ✅ VALIDACIÓN: No permitir empates en eliminatorias
        if ("ELIMINATORIA".equals(torneo.getModalidad()) && puntos1.equals(puntos2)) {
            throw new RuntimeException(
                    "No se permiten empates en modalidad ELIMINATORIA. " +
                            "Debe haber un ganador definido. " +
                            "Por favor realiza un desempate.");
        }

        enfrentamiento.setPuntosParticipante1(puntos1);
        enfrentamiento.setPuntosParticipante2(puntos2);

        if (puntos1 > puntos2) {
            enfrentamiento.setResultado("GANA_1");
            actualizarPuntuacion(enfrentamiento.getParticipante1(), true);
            actualizarPuntuacion(enfrentamiento.getParticipante2(), false);
        } else if (puntos2 > puntos1) {
            enfrentamiento.setResultado("GANA_2");
            actualizarPuntuacion(enfrentamiento.getParticipante2(), true);
            actualizarPuntuacion(enfrentamiento.getParticipante1(), false);
        } else {
            // Solo se permite en TODOS_CONTRA_TODOS
            enfrentamiento.setResultado("EMPATE");
            actualizarPuntuacionEmpate(enfrentamiento.getParticipante1());
            actualizarPuntuacionEmpate(enfrentamiento.getParticipante2());
        }

        enfrentamiento.setJuez(torneo.getJuezResponsable());
        enfrentamientoRepository.save(enfrentamiento);

        // ✅ AGREGAR ESTAS LÍNEAS:
        // Invalidar el caché del ranking para que se recalcule con los nuevos datos
        rankingService.limpiarCacheRanking(torneoId);
        System.out.println("🗑️ Caché de ranking invalidado para torneo ID: " + torneoId);

        return convertToEnfrentamientoDTO(enfrentamiento);

    }

    private void actualizarPuntuacion(Participante participante, boolean gano) {
        if (gano) {
            participante.setPuntuacionTotal(participante.getPuntuacionTotal() + 3);
            participante.setPartidosGanados(participante.getPartidosGanados() + 1);
        } else {
            participante.setPartidosPerdidos(participante.getPartidosPerdidos() + 1);
        }
        participanteRepository.save(participante);
    }

    private void actualizarPuntuacionEmpate(Participante participante) {
        participante.setPuntuacionTotal(participante.getPuntuacionTotal() + 1);
        participante.setPartidosEmpatados(participante.getPartidosEmpatados() + 1);
        participanteRepository.save(participante);
    }

    // ==================== CONVERSIÓN DTO ====================

    public TorneoDTO convertToDTO(Torneo torneo) {
        TorneoDTO dto = new TorneoDTO();
        dto.setId(torneo.getId());
        dto.setNombre(torneo.getNombre());
        dto.setDescripcion(torneo.getDescripcion());

        if (torneo.getSede() != null) {
            dto.setSedeId(torneo.getSede().getId());
            dto.setSedeNombre(torneo.getSede().getNombre());
            dto.setSedeDireccion(torneo.getSede().getDireccion());
            dto.setSedeDistrito(torneo.getSede().getDistrito());
        }

        if (torneo.getCategoria() != null) {
            dto.setCategoriaId(torneo.getCategoria().getId());
            dto.setCategoriaNombre(torneo.getCategoria().getNombre());
            dto.setCategoriaDescripcion(torneo.getCategoria().getDescripcion());
            dto.setEdadMinima(torneo.getCategoria().getEdadMinima());
            dto.setEdadMaxima(torneo.getCategoria().getEdadMaxima());
            dto.setPesoMaximo(torneo.getCategoria().getPesoMaximo());
        }

        dto.setEstado(torneo.getEstado());
        dto.setModalidad(torneo.getModalidad());
        dto.setFaseActual(torneo.getFaseActual());

        if (torneo.getJuezResponsable() != null) {
            dto.setJuezResponsableId(torneo.getJuezResponsable().getId());
            dto.setJuezResponsableNombre(
                    torneo.getJuezResponsable().getNombre() + " " +
                            torneo.getJuezResponsable().getApellido());
        }

        dto.setFechaCreacion(torneo.getFechaCreacion());
        dto.setFechaInicio(torneo.getFechaInicio());
        dto.setFechaFin(torneo.getFechaFin());
        dto.setFechaActivacionProgramada(torneo.getFechaActivacionProgramada());
        dto.setActivacionAutomatica(torneo.getActivacionAutomatica());

        if (torneo.getFechaActivacionProgramada() != null && "PENDIENTE".equals(torneo.getEstado())) {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime fechaActivacion = torneo.getFechaActivacionProgramada();

            if (fechaActivacion.isAfter(ahora)) {
                Duration duracion = Duration.between(ahora, fechaActivacion);
                dto.setSegundosRestantes(duracion.getSeconds());
            } else {
                dto.setSegundosRestantes(0L);
            }
        }

        // ✅ AGREGAR CONTEO DE PARTICIPANTES Y ENFRENTAMIENTOS
        try {
            // Contar participantes del torneo
            int cantidadParticipantes = Math.toIntExact(participanteRepository.countByTorneoId(torneo.getId()));
            dto.setCantidadParticipantes(cantidadParticipantes);

            // Contar enfrentamientos (competencias) del torneo
            int cantidadEnfrentamientos = enfrentamientoRepository.countByTorneoId(torneo.getId());
            dto.setCantidadCompetencias(cantidadEnfrentamientos);

            System.out.println("✅ Torneo " + torneo.getId() + ": " +
                    cantidadParticipantes + " participantes, " +
                    cantidadEnfrentamientos + " enfrentamientos");
        } catch (Exception e) {
            System.err.println("⚠️ Error contando participantes/enfrentamientos: " + e.getMessage());
            dto.setCantidadParticipantes(0);
            dto.setCantidadCompetencias(0);
        }

        return dto;
    }

    private EnfrentamientoDTO convertToEnfrentamientoDTO(Enfrentamiento e) {
        EnfrentamientoDTO dto = new EnfrentamientoDTO();
        dto.setId(e.getId());
        dto.setParticipante1Nombre(
                e.getParticipante1().getUsuario().getNombre() + " " + e.getParticipante1().getUsuario().getApellido());
        dto.setParticipante2Nombre(
                e.getParticipante2().getUsuario().getNombre() + " " + e.getParticipante2().getUsuario().getApellido());
        dto.setParticipante1Robot(e.getParticipante1().getNombreRobot());
        dto.setParticipante2Robot(e.getParticipante2().getNombreRobot());
        dto.setPuntosParticipante1(e.getPuntosParticipante1());
        dto.setPuntosParticipante2(e.getPuntosParticipante2());
        dto.setResultado(e.getResultado());
        dto.setRonda(e.getRonda());
        dto.setFechaEnfrentamiento(e.getFechaEnfrentamiento());

        if (e.getJuez() != null) {
            dto.setJuezNombre(e.getJuez().getNombre() + " " + e.getJuez().getApellido());
        }

        if (e.getTorneo() != null) {
            dto.setTorneoId(e.getTorneo().getId());
            dto.setTorneoNombre(e.getTorneo().getNombre());
        }

        return dto;
    }

    public List<EnfrentamientoDTO> getEnfrentamientosTorneo(Long torneoId) {
        List<Enfrentamiento> enfrentamientos = enfrentamientoRepository.findByTorneoId(torneoId);
        return enfrentamientos.stream().map(this::convertToEnfrentamientoDTO).collect(Collectors.toList());
    }

    // ✅ AGREGAR ESTE MÉTODO EN TorneoService.java

    /**
     * Obtiene el ranking completo de un torneo con toda la información necesaria
     * ✅ IMPORTANTE: Este método tiene @Transactional para evitar
     * LazyInitializationException
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerRankingCompleto(Long torneoId) {
        // Obtener el torneo para verificar su estado
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        System.out.println(
                "📊 Obteniendo ranking para torneo: " + torneo.getNombre() + " (Estado: " + torneo.getEstado() + ")");

        // Si el torneo está finalizado, redirigir al PDF
        if ("FINALIZADO".equals(torneo.getEstado())) {
            return Map.of(
                    "mensaje", "El torneo ha finalizado. Descarga el ranking en PDF",
                    "estadoTorneo", "FINALIZADO",
                    "urlPDF", "/api/reportes/torneos/" + torneoId + "/ranking/pdf",
                    "ranking", List.of());
        }

        // Si el torneo está pendiente, no mostrar ranking
        if ("PENDIENTE".equals(torneo.getEstado())) {
            return Map.of(
                    "mensaje", "El torneo aún no ha iniciado",
                    "estadoTorneo", "PENDIENTE",
                    "ranking", List.of());
        }

        // Si el torneo está ACTIVO, calcular y devolver ranking
        try {
            List<Participante> participantes = rankingService.calcularRankingPorTorneo(torneoId);
            System.out.println("👥 Participantes encontrados: " + participantes.size());

            // ✅ Convertir a DTO DENTRO de la transacción para evitar lazy loading errors
            List<ParticipanteDTO> ranking = participantes.stream()
                    .map(p -> {
                        try {
                            ParticipanteDTO dto = new ParticipanteDTO();
                            dto.setId(p.getId());
                            dto.setNombreRobot(p.getNombreRobot());
                            dto.setDescripcionRobot(p.getDescripcionRobot());
                            dto.setPuntuacionTotal(p.getPuntuacionTotal() != null ? p.getPuntuacionTotal() : 0);
                            dto.setPartidosGanados(p.getPartidosGanados() != null ? p.getPartidosGanados() : 0);
                            dto.setPartidosPerdidos(p.getPartidosPerdidos() != null ? p.getPartidosPerdidos() : 0);
                            dto.setPartidosEmpatados(p.getPartidosEmpatados() != null ? p.getPartidosEmpatados() : 0);
                            dto.setEfectividad(rankingService.calcularEfectividad(p));

                            // ✅ Acceder a las relaciones dentro de la transacción
                            User usuario = p.getUsuario();
                            if (usuario != null) {
                                dto.setUsuarioId(usuario.getId());
                                dto.setUsuarioNombre(usuario.getNombre() + " " + usuario.getApellido());
                                dto.setUsuarioEmail(usuario.getEmail());
                                dto.setUsuarioDni(usuario.getDni());

                                if (usuario.getClub() != null) {
                                    dto.setClubId(usuario.getClub().getId());
                                    dto.setClubNombre(usuario.getClub().getNombre());
                                }
                            }

                            Robot robot = p.getRobot();
                            if (robot != null && robot.getCategoria() != null) {
                                dto.setCategoriaId(robot.getCategoria().getId());
                                dto.setCategoriaNombre(robot.getCategoria().getNombre());
                            }

                            return dto;
                        } catch (Exception e) {
                            System.err
                                    .println("❌ Error convirtiendo participante " + p.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                            throw new RuntimeException("Error procesando participante: " + e.getMessage(), e);
                        }
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Ranking procesado exitosamente con " + ranking.size() + " participantes");

            return Map.of(
                    "mensaje", "Ranking en tiempo real",
                    "estadoTorneo", "ACTIVO",
                    "ranking", ranking);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo ranking completo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al obtener el ranking: " + e.getMessage(), e);
        }
    }

    // ✅ MÉTODO CORREGIDO: Avanzar ganadores - validar que no haya empates
    @Transactional
    public Map<String, Object> avanzarGanadores(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

        String faseActual = torneo.getFaseActual();

        if ("FINAL".equals(faseActual)) {
            // Si es la final, finalizar el torneo
            torneo.setEstado("FINALIZADO");
            torneo.setFechaFin(LocalDateTime.now());

            // ✅ NUEVO: Limpiar caché del ranking al finalizar
            rankingService.limpiarCacheRanking(torneoId);

            torneoRepository.save(torneo);

            return Map.of(
                    "mensaje", "¡Torneo finalizado!",
                    "fase", "FINAL",
                    "finalizado", true);
        }

        // Obtener enfrentamientos de la fase actual
        List<Enfrentamiento> enfrentamientosFaseActual = enfrentamientoRepository
                .findByTorneoIdAndRonda(torneoId, faseActual);

        // Verificar que todos los enfrentamientos estén completos
        boolean todosCompletos = enfrentamientosFaseActual.stream()
                .allMatch(e -> !"PENDIENTE".equals(e.getResultado()));

        if (!todosCompletos) {
            throw new RuntimeException("Aún hay enfrentamientos pendientes en " + faseActual);
        }

        // ✅ VALIDACIÓN: Verificar que no haya empates en eliminatorias
        if ("ELIMINATORIA".equals(torneo.getModalidad())) {
            boolean hayEmpates = enfrentamientosFaseActual.stream()
                    .anyMatch(e -> "EMPATE".equals(e.getResultado()));

            if (hayEmpates) {
                throw new RuntimeException(
                        "Hay enfrentamientos empatados en " + faseActual + ". " +
                                "Todos los enfrentamientos deben tener un ganador definido antes de avanzar.");
            }
        }

        // Obtener ganadores (solo participantes con victoria definida)
        List<Participante> ganadores = enfrentamientosFaseActual.stream()
                .map(e -> {
                    if ("GANA_1".equals(e.getResultado()))
                        return e.getParticipante1();
                    if ("GANA_2".equals(e.getResultado()))
                        return e.getParticipante2();
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (ganadores.isEmpty()) {
            throw new RuntimeException("No hay ganadores para avanzar");
        }

        // ✅ VALIDACIÓN: Verificar número par de ganadores
        if (ganadores.size() % 2 != 0) {
            throw new RuntimeException(
                    "Error en el bracket: se detectó un número impar de ganadores (" + ganadores.size() + "). " +
                            "Contacta al administrador del sistema.");
        }

        // Determinar siguiente fase
        String siguienteFase = obtenerSiguienteFase(faseActual);
        torneo.setFaseActual(siguienteFase);
        torneoRepository.save(torneo);

        // Generar enfrentamientos de la siguiente fase (mantener orden, no mezclar)
        for (int i = 0; i < ganadores.size(); i += 2) {
            Enfrentamiento nuevoEnfrentamiento = new Enfrentamiento();
            nuevoEnfrentamiento.setParticipante1(ganadores.get(i));
            nuevoEnfrentamiento.setParticipante2(ganadores.get(i + 1));
            nuevoEnfrentamiento.setTorneo(torneo);
            nuevoEnfrentamiento.setRonda(siguienteFase);
            nuevoEnfrentamiento.setResultado("PENDIENTE");
            nuevoEnfrentamiento.setPuntosParticipante1(0);
            nuevoEnfrentamiento.setPuntosParticipante2(0);
            nuevoEnfrentamiento.setFechaEnfrentamiento(LocalDateTime.now());

            enfrentamientoRepository.save(nuevoEnfrentamiento);
        }

        return Map.of(
                "mensaje", "Ganadores avanzaron exitosamente",
                "faseAnterior", faseActual,
                "nuevaFase", siguienteFase,
                "ganadoresAvanzados", ganadores.size());
    }

    private String obtenerSiguienteFase(String faseActual) {
        switch (faseActual) {
            case "OCTAVOS":
                return "CUARTOS";
            case "CUARTOS":
                return "SEMIFINAL";
            case "SEMIFINAL":
                return "FINAL";
            default:
                throw new RuntimeException("Fase inválida: " + faseActual);
        }
    }
}