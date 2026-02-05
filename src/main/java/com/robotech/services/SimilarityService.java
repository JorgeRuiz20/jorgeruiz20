package com.robotech.services;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * ✅ Servicio mejorado con umbrales dinámicos por contexto
 * 
 * UMBRALES DE SIMILITUD:
 * - Emails: 0.75 (75%) - Muy permisivo
 * - Nombres/Apellidos: 0.70 (70%) - Permisivo
 * - Robots: 0.80 (80%) - Moderado
 * - Torneos: 0.82 (82%) - Moderado-Estricto
 * - Categorías: 0.85 (85%) - Estricto
 * - Sedes: 0.85 (85%) - Estricto
 * - Clubs: 0.83 (83%) - Moderado-Estricto
 */
@Service
public class SimilarityService {

    // Umbrales por contexto
    public static final double THRESHOLD_EMAIL = 0.75;      // Emails
    public static final double THRESHOLD_NOMBRE = 0.70;     // Nombres/Apellidos
    public static final double THRESHOLD_ROBOT = 0.80;      // Robots
    public static final double THRESHOLD_TORNEO = 0.82;     // Torneos
    public static final double THRESHOLD_CATEGORIA = 0.85;  // Categorías
    public static final double THRESHOLD_SEDE = 0.85;       // Sedes
    public static final double THRESHOLD_CLUB = 0.83;       // Clubs

    private final JaroWinklerSimilarity jaroWinkler;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");

    public SimilarityService() {
        this.jaroWinkler = new JaroWinklerSimilarity();
    }

    /**
     * Normaliza texto: sin acentos, minúsculas, sin espacios extras
     */
    public String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        
        String normalized = Normalizer.normalize(texto, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase().trim().replaceAll("\\s+", " ");
        
        return normalized;
    }

    /**
     * Calcula similitud entre dos textos (0.0 a 1.0)
     */
    public double calcularSimilitud(String texto1, String texto2) {
        String norm1 = normalizar(texto1);
        String norm2 = normalizar(texto2);
        
        if (norm1.isEmpty() || norm2.isEmpty()) {
            return 0.0;
        }
        
        return jaroWinkler.apply(norm1, norm2);
    }

    /**
     * Verifica similitud con umbral personalizado
     */
    public boolean sonSimilares(String texto1, String texto2, double threshold) {
        return calcularSimilitud(texto1, texto2) >= threshold;
    }

    /**
     * Verifica si existe texto similar en lista con umbral personalizado
     */
    public boolean existeSimilarEn(String textoNuevo, Iterable<String> textosExistentes, double threshold) {
        for (String textoExistente : textosExistentes) {
            if (sonSimilares(textoNuevo, textoExistente, threshold)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encuentra el texto más similar con umbral personalizado
     */
    public String encontrarSimilarMasCercano(String textoNuevo, Iterable<String> textosExistentes, double threshold) {
        double maxSimilitud = 0.0;
        String masSimilar = null;
        
        for (String textoExistente : textosExistentes) {
            double similitud = calcularSimilitud(textoNuevo, textoExistente);
            if (similitud > maxSimilitud && similitud >= threshold) {
                maxSimilitud = similitud;
                masSimilar = textoExistente;
            }
        }
        
        return masSimilar;
    }

    // ============ MÉTODOS DE CONTEXTO ESPECÍFICO ============

    /**
     * Para EMAILS - Muy permisivo (75%)
     */
    public boolean existeEmailSimilar(String emailNuevo, Iterable<String> emailsExistentes) {
        return existeSimilarEn(emailNuevo, emailsExistentes, THRESHOLD_EMAIL);
    }

    public String encontrarEmailSimilar(String emailNuevo, Iterable<String> emailsExistentes) {
        return encontrarSimilarMasCercano(emailNuevo, emailsExistentes, THRESHOLD_EMAIL);
    }

    /**
     * Para NOMBRES/APELLIDOS - Permisivo (70%)
     */
    public boolean existeNombreSimilar(String nombreNuevo, Iterable<String> nombresExistentes) {
        return existeSimilarEn(nombreNuevo, nombresExistentes, THRESHOLD_NOMBRE);
    }

    public String encontrarNombreSimilar(String nombreNuevo, Iterable<String> nombresExistentes) {
        return encontrarSimilarMasCercano(nombreNuevo, nombresExistentes, THRESHOLD_NOMBRE);
    }

    /**
     * Para ROBOTS - Moderado (80%)
     */
    public boolean existeRobotSimilar(String nombreRobot, Iterable<String> robotsExistentes) {
        return existeSimilarEn(nombreRobot, robotsExistentes, THRESHOLD_ROBOT);
    }

    public String encontrarRobotSimilar(String nombreRobot, Iterable<String> robotsExistentes) {
        return encontrarSimilarMasCercano(nombreRobot, robotsExistentes, THRESHOLD_ROBOT);
    }

    /**
     * Para TORNEOS - Moderado-Estricto (82%)
     */
    public boolean existeTorneoSimilar(String nombreTorneo, Iterable<String> torneosExistentes) {
        return existeSimilarEn(nombreTorneo, torneosExistentes, THRESHOLD_TORNEO);
    }

    public String encontrarTorneoSimilar(String nombreTorneo, Iterable<String> torneosExistentes) {
        return encontrarSimilarMasCercano(nombreTorneo, torneosExistentes, THRESHOLD_TORNEO);
    }

    /**
     * Para CATEGORÍAS - Estricto (85%)
     */
    public boolean existeCategoriaSimilar(String nombreCategoria, Iterable<String> categoriasExistentes) {
        return existeSimilarEn(nombreCategoria, categoriasExistentes, THRESHOLD_CATEGORIA);
    }

    public String encontrarCategoriaSimilar(String nombreCategoria, Iterable<String> categoriasExistentes) {
        return encontrarSimilarMasCercano(nombreCategoria, categoriasExistentes, THRESHOLD_CATEGORIA);
    }

    /**
     * Para SEDES - Estricto (85%)
     */
    public boolean existeSedeSimilar(String nombreSede, Iterable<String> sedesExistentes) {
        return existeSimilarEn(nombreSede, sedesExistentes, THRESHOLD_SEDE);
    }

    public String encontrarSedeSimilar(String nombreSede, Iterable<String> sedesExistentes) {
        return encontrarSimilarMasCercano(nombreSede, sedesExistentes, THRESHOLD_SEDE);
    }

    /**
     * Para CLUBS - Moderado-Estricto (83%)
     */
    public boolean existeClubSimilar(String nombreClub, Iterable<String> clubsExistentes) {
        return existeSimilarEn(nombreClub, clubsExistentes, THRESHOLD_CLUB);
    }

    public String encontrarClubSimilar(String nombreClub, Iterable<String> clubsExistentes) {
        return encontrarSimilarMasCercano(nombreClub, clubsExistentes, THRESHOLD_CLUB);
    }

    /**
     * Validación de nombres y apellidos completos
     */
    public boolean existeNombreCompletoSimilar(String nombreCompleto, Iterable<String> nombresCompletos) {
        return existeSimilarEn(nombreCompleto, nombresCompletos, THRESHOLD_NOMBRE);
    }

    public String encontrarNombreCompletoSimilar(String nombreCompleto, Iterable<String> nombresCompletos) {
        return encontrarSimilarMasCercano(nombreCompleto, nombresCompletos, THRESHOLD_NOMBRE);
    }

    // ============ VALIDACIONES DE FORMATO ============

    /**
     * Valida DNI: exactamente 8 dígitos
     */
    public boolean validarDNI(String dni) {
        if (dni == null || dni.isBlank()) {
            return false;
        }
        return DNI_PATTERN.matcher(dni.trim()).matches();
    }

    /**
     * Valida formato de email
     */
    public boolean validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valida nombre/apellido: solo letras, espacios y acentos
     */
    public boolean validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }
        
        String nombreTrim = nombre.trim();
        
        // Mínimo 2 caracteres, máximo 50
        if (nombreTrim.length() < 2 || nombreTrim.length() > 50) {
            return false;
        }
        
        // Solo letras, espacios y caracteres latinos con acentos
        return nombreTrim.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$");
    }
}