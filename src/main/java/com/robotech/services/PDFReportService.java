package com.robotech.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.robotech.models.Club;
import com.robotech.models.Participante;
import com.robotech.models.Torneo;
import com.robotech.models.User;
import com.robotech.models.views.*;
import com.robotech.repositories.*;
import com.robotech.repositories.views.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PDFReportService {

    private final VistaTorneoCompletaRepository torneoVistaRepo;
    private final VistaParticipanteDetalleRepository participanteVistaRepo;
    private final VistaEnfrentamientoResultadoRepository enfrentamientoVistaRepo;
    private final VistaRankingGeneralRepository rankingVistaRepo;
    private final VistaEstadisticasClubRepository estadisticasClubRepo;
    private final TorneoRepository torneoRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ParticipanteRepository participanteRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DeviceRgb COLOR_PRIMARIO = new DeviceRgb(102, 126, 234);
    private static final DeviceRgb COLOR_SECUNDARIO = new DeviceRgb(118, 75, 162);

    // ==================== 1. REPORTE TORNEO COMPLETO ====================

    public byte[] generarReporteTorneoCompleto(Long torneoId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            VistaTorneoCompleta torneo = torneoVistaRepo.findById(torneoId)
                    .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

            // Encabezado
            agregarEncabezado(document, "REPORTE DE TORNEO");

            // Info del torneo
            Table infoTable = new Table(2);
            infoTable.setWidth(UnitValue.createPercentValue(100));

            agregarFilaInfo(infoTable, "Torneo:", torneo.getTorneoNombre());
            agregarFilaInfo(infoTable, "Estado:", torneo.getTorneoEstado());
            agregarFilaInfo(infoTable, "Categoría:", torneo.getCategoriaNombre());
            agregarFilaInfo(infoTable, "Modalidad:",
                    torneo.getModalidad() != null ? torneo.getModalidad() : "No asignada");
            agregarFilaInfo(infoTable, "Fase Actual:",
                    torneo.getFaseActual() != null ? torneo.getFaseActual() : "No iniciado");
            
            // ✅ NUEVO: Fechas de inicio y fin
            String fechaInicio = torneo.getFechaInicio() != null 
                ? torneo.getFechaInicio().format(FORMATTER) 
                : "No iniciado";
            agregarFilaInfo(infoTable, "Fecha Inicio:", fechaInicio);
            
            String fechaFin = torneo.getFechaFin() != null 
                ? torneo.getFechaFin().format(FORMATTER) 
                : "No finalizado";
            agregarFilaInfo(infoTable, "Fecha Fin:", fechaFin);
            
            agregarFilaInfo(infoTable, "Juez:",
                    torneo.getJuezNombre() != null ? torneo.getJuezNombre() : "No asignado");
            agregarFilaInfo(infoTable, "Participantes:", String.valueOf(torneo.getTotalParticipantes()));
            agregarFilaInfo(infoTable, "Enfrentamientos:", String.valueOf(torneo.getTotalEnfrentamientos()));

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Participantes
            List<VistaParticipanteDetalle> participantes = participanteVistaRepo
                    .findByTorneoIdOrderByPuntuacionDesc(torneoId);

            if (!participantes.isEmpty()) {
                document.add(new Paragraph("RANKING DE PARTICIPANTES")
                        .setFontSize(14)
                        .setBold()
                        .setFontColor(COLOR_PRIMARIO));

                Table participantesTable = new Table(new float[] { 1, 3, 3, 2, 2, 2, 2 });
                participantesTable.setWidth(UnitValue.createPercentValue(100));

                agregarEncabezadoTabla(participantesTable,
                        "#", "Robot", "Competidor", "Club", "Puntos", "V-D-E", "Efect.");

                int posicion = 1;
                for (VistaParticipanteDetalle p : participantes) {
                    participantesTable.addCell(crearCelda(String.valueOf(posicion++)));
                    participantesTable.addCell(crearCelda(p.getNombreRobot()));
                    participantesTable.addCell(crearCelda(p.getUsuarioNombre()));
                    participantesTable.addCell(crearCelda(p.getClubNombre() != null ? p.getClubNombre() : "Sin club"));
                    participantesTable.addCell(crearCelda(String.valueOf(p.getPuntuacionTotal())));
                    participantesTable.addCell(crearCelda(
                            p.getPartidosGanados() + "-" +
                                    p.getPartidosPerdidos() + "-" +
                                    p.getPartidosEmpatados()));
                    participantesTable.addCell(crearCelda(
                            String.format("%.1f%%", p.getEfectividad() != null ? p.getEfectividad() : 0.0)));
                }

                document.add(participantesTable);
            }

            // Enfrentamientos
            List<VistaEnfrentamientoResultado> enfrentamientos = enfrentamientoVistaRepo.findByTorneoId(torneoId);

            if (!enfrentamientos.isEmpty()) {
                document.add(new Paragraph("\n\nENFRENTAMIENTOS")
                        .setFontSize(14)
                        .setBold()
                        .setFontColor(COLOR_PRIMARIO));

                Table enfTable = new Table(new float[] { 2, 3, 3, 2, 2 });
                enfTable.setWidth(UnitValue.createPercentValue(100));

                agregarEncabezadoTabla(enfTable, "Fecha", "Participante 1", "Participante 2", "Resultado", "Ronda");

                for (VistaEnfrentamientoResultado e : enfrentamientos) {
                    enfTable.addCell(crearCelda(
                            e.getFechaEnfrentamiento() != null ? e.getFechaEnfrentamiento().format(FORMATTER)
                                    : "Pendiente"));
                    enfTable.addCell(crearCelda(
                            e.getParticipante1Robot() + "\n(" + e.getParticipante1Usuario() + ")"));
                    enfTable.addCell(crearCelda(
                            e.getParticipante2Robot() + "\n(" + e.getParticipante2Usuario() + ")"));
                    enfTable.addCell(crearCelda(formatearResultado(e)));
                    enfTable.addCell(crearCelda(e.getRonda() != null ? e.getRonda() : "N/A"));
                }

                document.add(enfTable);
            }

            agregarPiePagina(document);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ==================== 2. REPORTE RANKING POR TORNEO ====================

    public byte[] generarReporteRanking(Long torneoId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            List<VistaRankingGeneral> ranking = rankingVistaRepo.findByTorneoIdOrderByPosicion(torneoId);

            if (ranking.isEmpty()) {
                throw new RuntimeException("No hay datos de ranking para este torneo");
            }

            // ✅ NUEVO: Obtener información del torneo
            Torneo torneo = torneoRepository.findById(torneoId)
                    .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

            // ✅ NUEVO: Encabezado mejorado
            agregarEncabezado(document, "RANKING FINAL - " + ranking.get(0).getTorneoNombre());

            // ✅ NUEVO: Agregar información del estado del torneo
            Paragraph info = new Paragraph();
            info.add("Estado: " + torneo.getEstado() + "\n");

            if (torneo.getFechaFin() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                info.add("Fecha de finalización: " + torneo.getFechaFin().format(formatter) + "\n");
            }

            if (torneo.getFechaInicio() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                info.add("Fecha de inicio: " + torneo.getFechaInicio().format(formatter) + "\n");
            }

            info.add("Total de participantes: " + ranking.size());
            info.setFontSize(10);
            info.setMarginBottom(15);
            document.add(info);

            Table table = new Table(new float[] { 1, 3, 3, 2, 2, 2, 2, 2 });
            table.setWidth(UnitValue.createPercentValue(100));

            agregarEncabezadoTabla(table,
                    "Pos", "Robot", "Competidor", "Club", "Puntos", "Victorias", "Derrotas", "Efect.");

            // ✅ NUEVO: Agregar medallas visuales para TOP 3
            for (VistaRankingGeneral r : ranking) {
                String posicion = String.valueOf(r.getPosicion());

                // Agregar medallas para los primeros 3 lugares
                if (r.getPosicion() == 1) {
                    posicion = "🥇 " + posicion;
                } else if (r.getPosicion() == 2) {
                    posicion = "🥈 " + posicion;
                } else if (r.getPosicion() == 3) {
                    posicion = "🥉 " + posicion;
                }

                table.addCell(crearCelda(posicion));
                table.addCell(crearCelda(r.getNombreRobot()));
                table.addCell(crearCelda(r.getCompetidor()));
                table.addCell(crearCelda(r.getClubNombre() != null ? r.getClubNombre() : "Sin club"));
                table.addCell(crearCelda(String.valueOf(r.getPuntuacionTotal())));
                table.addCell(crearCelda(String.valueOf(r.getPartidosGanados())));
                table.addCell(crearCelda(String.valueOf(r.getPartidosPerdidos())));
                table.addCell(crearCelda(
                        String.format("%.1f%%", r.getEfectividad() != null ? r.getEfectividad() : 0.0)));
            }

            document.add(table);
            agregarPiePagina(document);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ==================== 3. REPORTE ESTADISTICAS CLUB ====================

    public byte[] generarReporteEstadisticasClub(Long clubId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            VistaEstadisticasClub stats = estadisticasClubRepo.findById(clubId)
                    .orElseThrow(() -> new RuntimeException("Club no encontrado"));

            agregarEncabezado(document, "ESTADÍSTICAS DEL CLUB");

            Table infoTable = new Table(2);
            infoTable.setWidth(UnitValue.createPercentValue(100));

            // ✅ Calcular tasa de efectividad
            double tasaEfectividad = calcularTasaEfectividad(
                stats.getTotalVictorias(), 
                stats.getTotalDerrotas(), 
                stats.getTotalEmpates()
            );

            agregarFilaInfo(infoTable, "Club:", stats.getClubNombre());
            agregarFilaInfo(infoTable, "Ciudad:", stats.getCiudad());
            agregarFilaInfo(infoTable, "País:", stats.getPais());
            agregarFilaInfo(infoTable, "Responsable:", stats.getClubOwner());
            agregarFilaInfo(infoTable, "Total de Miembros:", String.valueOf(stats.getTotalMiembros()));
            agregarFilaInfo(infoTable, "Total de Robots:", String.valueOf(stats.getTotalRobots()));
            agregarFilaInfo(infoTable, "Participaciones:", String.valueOf(stats.getTotalParticipaciones()));
            agregarFilaInfo(infoTable, "Victorias:", String.valueOf(stats.getTotalVictorias()));
            agregarFilaInfo(infoTable, "Derrotas:", String.valueOf(stats.getTotalDerrotas()));
            agregarFilaInfo(infoTable, "Empates:", String.valueOf(stats.getTotalEmpates()));
            agregarFilaInfo(infoTable, "Tasa de Efectividad:", String.format("%.2f%%", tasaEfectividad));
            agregarFilaInfo(infoTable, "Puntuación Total:", String.valueOf(stats.getPuntuacionAcumulada()));
            agregarFilaInfo(infoTable, "Torneos Ganados:", String.valueOf(stats.getTorneosGanados()));

            document.add(infoTable);
            agregarPiePagina(document);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ==================== 4. REPORTE TODOS LOS TORNEOS (MEJORADO) ====================

    public byte[] generarReporteTodosLosTorneos() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            List<VistaTorneoCompleta> torneos = torneoVistaRepo.findAll();

            agregarEncabezado(document, "REPORTE GENERAL DE TORNEOS");

            // ✅ MEJORA: 9 columnas con fecha inicio, fecha fin y ganador
            Table table = new Table(new float[] { 3, 1.8f, 1.8f, 1.8f, 1.2f, 1.2f, 1.8f, 1.8f, 2.5f });
            table.setWidth(UnitValue.createPercentValue(100));
            table.setTextAlignment(TextAlignment.CENTER);

            agregarEncabezadoTabla(table,
                    "Torneo", "Estado", "Categoría", "Modalidad", 
                    "Part.", "Enf.", "F. Inicio", "F. Fin", "Ganador");

            for (VistaTorneoCompleta t : torneos) {
                table.addCell(crearCeldaCentrada(t.getTorneoNombre()));
                table.addCell(crearCeldaCentrada(t.getTorneoEstado()));
                table.addCell(crearCeldaCentrada(t.getCategoriaNombre()));
                table.addCell(crearCeldaCentrada(t.getModalidad() != null ? t.getModalidad() : "N/A"));
                table.addCell(crearCeldaCentrada(String.valueOf(t.getTotalParticipantes())));
                table.addCell(crearCeldaCentrada(String.valueOf(t.getTotalEnfrentamientos())));
                
                // Mostrar fecha de inicio
                String fechaInicio = t.getFechaInicio() != null 
                    ? t.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                    : "Pendiente";
                table.addCell(crearCeldaCentrada(fechaInicio));
                
                // ✅ NUEVO: Mostrar fecha de fin
                String fechaFin = t.getFechaFin() != null 
                    ? t.getFechaFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                    : "En curso";
                table.addCell(crearCeldaCentrada(fechaFin));
                
                // Obtener ganador del torneo
                String ganador = obtenerGanadorTorneo(t.getTorneoId(), t.getTorneoEstado());
                table.addCell(crearCeldaCentrada(ganador));
            }

            document.add(table);
            agregarPiePagina(document);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ==================== 5. REPORTE TODOS LOS CLUBS (MEJORADO) ====================

    public byte[] generarReporteTodosLosClubs() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            List<VistaEstadisticasClub> clubs = estadisticasClubRepo.findAllOrderByPuntuacionDesc();

            agregarEncabezado(document, "REPORTE GENERAL DE CLUBS");

            // ✅ MEJORA: Incluye Victorias, Derrotas y Tasa de Efectividad
            Table table = new Table(new float[] { 3, 2, 1.5f, 1.5f, 2, 2, 2, 2 });
            table.setWidth(UnitValue.createPercentValue(100));
            table.setTextAlignment(TextAlignment.CENTER);

            agregarEncabezadoTabla(table,
                    "Club", "Ciudad", "Miembros", "Robots", "Victorias", "Derrotas", "Puntos", "Efectividad");

            for (VistaEstadisticasClub c : clubs) {
                // Calcular tasa de efectividad
                double tasaEfectividad = calcularTasaEfectividad(
                    c.getTotalVictorias(), 
                    c.getTotalDerrotas(), 
                    c.getTotalEmpates()
                );

                table.addCell(crearCeldaCentrada(c.getClubNombre()));
                table.addCell(crearCeldaCentrada(c.getCiudad()));
                table.addCell(crearCeldaCentrada(String.valueOf(c.getTotalMiembros())));
                table.addCell(crearCeldaCentrada(String.valueOf(c.getTotalRobots())));
                table.addCell(crearCeldaCentrada(String.valueOf(c.getTotalVictorias())));
                table.addCell(crearCeldaCentrada(String.valueOf(c.getTotalDerrotas())));
                table.addCell(crearCeldaCentrada(String.valueOf(c.getPuntuacionAcumulada())));
                table.addCell(crearCeldaCentrada(String.format("%.2f%%", tasaEfectividad)));
            }

            document.add(table);
            agregarPiePagina(document);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * ✅ NUEVO: Método para obtener el ganador de un torneo
     */
    private String obtenerGanadorTorneo(Long torneoId, String estado) {
        // Si el torneo no está finalizado, no hay ganador
        if (!"FINALIZADO".equals(estado)) {
            return "En curso";
        }

        try {
            // Obtener el ranking del torneo ordenado por posición
            List<VistaRankingGeneral> ranking = rankingVistaRepo.findByTorneoIdOrderByPosicion(torneoId);
            
            if (ranking.isEmpty()) {
                return "Sin datos";
            }

            // El primero en el ranking es el ganador
            VistaRankingGeneral ganador = ranking.get(0);
            return ganador.getNombreRobot() + " (" + ganador.getCompetidor() + ")";
            
        } catch (Exception e) {
            return "Error al obtener";
        }
    }

    private void agregarEncabezado(Document document, String titulo) {
        Paragraph header = new Paragraph("🤖 ROBOTECH")
                .setFontSize(20)
                .setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph subheader = new Paragraph(titulo)
                .setFontSize(16)
                .setFontColor(COLOR_SECUNDARIO)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(header);
        document.add(subheader);
        document.add(new Paragraph("\n"));
    }

    private void agregarPiePagina(Document document) {
        Paragraph footer = new Paragraph(
                "Generado el: " + LocalDateTime.now().format(FORMATTER))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(new Paragraph("\n"));
        document.add(footer);
    }

    private void agregarFilaInfo(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A")).setBorder(Border.NO_BORDER));
    }

    private void agregarEncabezadoTabla(Table table, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(COLOR_PRIMARIO)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private Cell crearCelda(String contenido) {
        return new Cell()
                .add(new Paragraph(contenido != null ? contenido : "N/A"))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell crearCeldaCentrada(String contenido) {
        return new Cell()
                .add(new Paragraph(contenido != null ? contenido : "N/A"))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    }

    /**
     * ✅ Calcular tasa de efectividad del club
     * Fórmula: (Victorias * 3 + Empates) / (Total de partidos * 3) * 100
     */
    private double calcularTasaEfectividad(Long victorias, Long derrotas, Long empates) {
        if (victorias == null) victorias = 0L;
        if (derrotas == null) derrotas = 0L;
        if (empates == null) empates = 0L;

        long totalPartidos = victorias + derrotas + empates;
        
        if (totalPartidos == 0) {
            return 0.0;
        }

        // (Victorias * 3 + Empates) / (Total * 3) * 100
        double puntosObtenidos = (victorias * 3.0) + empates;
        double puntosMaximos = totalPartidos * 3.0;
        
        return (puntosObtenidos / puntosMaximos) * 100.0;
    }

    private String formatearResultado(VistaEnfrentamientoResultado e) {
        if ("PENDIENTE".equals(e.getResultado())) {
            return "PENDIENTE";
        }
        return e.getPuntosParticipante1() + " - " + e.getPuntosParticipante2();
    }

    // ==================== VALIDACIONES DE SEGURIDAD ====================

    /**
     * ✅ Validar que club owner solo vea SU club
     */
    public byte[] generarReporteClubValidado(Long clubId, Long userId) {
        // 1. Obtener el usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Verificar que tiene rol CLUB_OWNER
        boolean esClubOwner = user.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));

        if (!esClubOwner) {
            throw new SecurityException("No tienes permisos de club owner");
        }

        // 3. Obtener el club del owner
        Club clubDelOwner = clubRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));

        // 4. Verificar que el clubId solicitado es SU club
        if (!clubDelOwner.getId().equals(clubId)) {
            throw new SecurityException(
                    "No tienes permiso para ver este club. " +
                            "Solo puedes ver tu propio club (ID: " + clubDelOwner.getId() + ")");
        }

        // 5. Si todo OK, generar el reporte
        return generarReporteEstadisticasClub(clubId);
    }

    /**
     * ✅ Validar que club owner solo vea torneos donde SU club participa
     */
    public byte[] generarReporteTorneoValidado(Long torneoId, Long userId) {
        // 1. Obtener el usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Verificar que tiene rol CLUB_OWNER
        boolean esClubOwner = user.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));

        if (!esClubOwner) {
            throw new SecurityException("No tienes permisos de club owner");
        }

        // 3. Obtener el club del owner
        Club clubDelOwner = clubRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));

        // 4. Verificar que el club tiene participantes en este torneo
        List<Participante> participantes = participanteRepository
                .findByTorneoIdAndUsuario_ClubId(torneoId, clubDelOwner.getId());

        if (participantes.isEmpty()) {
            throw new SecurityException(
                    "Tu club no participa en este torneo. " +
                            "No puedes generar este reporte.");
        }

        // 5. Si todo OK, generar el reporte
        return generarReporteTorneoCompleto(torneoId);
    }
}