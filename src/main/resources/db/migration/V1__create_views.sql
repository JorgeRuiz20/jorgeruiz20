-- ========================================
-- VISTAS PARA REPORTES ROBOTECH
-- ========================================

-- 1️⃣ VISTA: Torneos Completos
DROP VIEW IF EXISTS vista_torneos_completa;
CREATE VIEW vista_torneos_completa AS
SELECT 
    t.id AS torneo_id,
    t.nombre AS torneo_nombre,
    t.descripcion AS torneo_descripcion,
    t.estado AS torneo_estado,
    t.modalidad,
    t.fase_actual,
    t.fecha_creacion,
    t.fecha_inicio,
    t.fecha_fin,
    c.id AS categoria_id,
    c.nombre AS categoria_nombre,
    c.edad_minima,
    c.edad_maxima,
    c.peso_maximo,
    u.id AS juez_id,
    CONCAT(u.nombre, ' ', u.apellido) AS juez_nombre,
    COUNT(DISTINCT p.id) AS total_participantes,
    COUNT(DISTINCT e.id) AS total_enfrentamientos,
    SUM(CASE WHEN e.resultado = 'PENDIENTE' THEN 1 ELSE 0 END) AS enfrentamientos_pendientes,
    SUM(CASE WHEN e.resultado != 'PENDIENTE' THEN 1 ELSE 0 END) AS enfrentamientos_completados
FROM torneos t
LEFT JOIN categorias c ON t.categoria_id = c.id
LEFT JOIN users u ON t.juez_responsable_id = u.id
LEFT JOIN participantes p ON p.torneo_id = t.id
LEFT JOIN enfrentamientos e ON e.torneo_id = t.id
GROUP BY t.id, t.nombre, t.descripcion, t.estado, t.modalidad, t.fase_actual, 
         t.fecha_creacion, t.fecha_inicio, t.fecha_fin,
         c.id, c.nombre, c.edad_minima, c.edad_maxima, c.peso_maximo,
         u.id, u.nombre, u.apellido;

-- 2️⃣ VISTA: Participantes Detallados
DROP VIEW IF EXISTS vista_participantes_detalle;
CREATE VIEW vista_participantes_detalle AS
SELECT 
    p.id AS participante_id,
    p.nombre_robot,
    p.descripcion_robot,
    p.puntuacion_total,
    p.partidos_ganados,
    p.partidos_perdidos,
    p.partidos_empatados,
    CASE 
        WHEN (p.partidos_ganados + p.partidos_perdidos + p.partidos_empatados) = 0 THEN 0.0
        ELSE ROUND(((p.partidos_ganados * 3.0) + p.partidos_empatados) / 
             ((p.partidos_ganados + p.partidos_perdidos + p.partidos_empatados) * 3.0) * 100, 2)
    END AS efectividad,
    t.id AS torneo_id,
    t.nombre AS torneo_nombre,
    t.estado AS torneo_estado,
    u.id AS usuario_id,
    CONCAT(u.nombre, ' ', u.apellido) AS usuario_nombre,
    u.dni AS usuario_dni,
    u.email AS usuario_email,
    u.telefono AS usuario_telefono,
    cl.id AS club_id,
    cl.nombre AS club_nombre,
    cl.ciudad AS club_ciudad,
    cl.pais AS club_pais,
    r.id AS robot_id,
    r.peso AS robot_peso,
    r.codigo_identificacion AS robot_codigo,
    cat.nombre AS categoria_nombre
FROM participantes p
INNER JOIN users u ON p.usuario_id = u.id
INNER JOIN torneos t ON p.torneo_id = t.id
LEFT JOIN clubs cl ON u.club_id = cl.id
LEFT JOIN robots r ON p.robot_id = r.id
LEFT JOIN categorias cat ON r.categoria_id = cat.id;

-- 3️⃣ VISTA: Ranking General
DROP VIEW IF EXISTS vista_ranking_general;
CREATE VIEW vista_ranking_general AS
SELECT 
    p.id AS participante_id,
    ROW_NUMBER() OVER (PARTITION BY p.torneo_id ORDER BY p.puntuacion_total DESC, p.partidos_ganados DESC) AS posicion,
    p.nombre_robot,
    p.puntuacion_total,
    p.partidos_ganados,
    p.partidos_perdidos,
    p.partidos_empatados,
    CASE 
        WHEN (p.partidos_ganados + p.partidos_perdidos + p.partidos_empatados) = 0 THEN 0.0
        ELSE ROUND(((p.partidos_ganados * 3.0) + p.partidos_empatados) / 
             ((p.partidos_ganados + p.partidos_perdidos + p.partidos_empatados) * 3.0) * 100, 2)
    END AS efectividad,
    t.id AS torneo_id,
    t.nombre AS torneo_nombre,
    CONCAT(u.nombre, ' ', u.apellido) AS competidor,
    cl.nombre AS club_nombre,
    cat.nombre AS categoria_nombre
FROM participantes p
INNER JOIN users u ON p.usuario_id = u.id
INNER JOIN torneos t ON p.torneo_id = t.id
LEFT JOIN clubs cl ON u.club_id = cl.id
LEFT JOIN robots r ON p.robot_id = r.id
LEFT JOIN categorias cat ON r.categoria_id = cat.id;

-- 4️⃣ VISTA: Enfrentamientos con Resultados
DROP VIEW IF EXISTS vista_enfrentamientos_resultado;
CREATE VIEW vista_enfrentamientos_resultado AS
SELECT 
    e.id AS enfrentamiento_id,
    e.fecha_enfrentamiento,
    e.ronda,
    e.resultado,
    e.puntos_participante1,
    e.puntos_participante2,
    t.id AS torneo_id,
    t.nombre AS torneo_nombre,
    t.modalidad,
    p1.id AS participante1_id,
    p1.nombre_robot AS participante1_robot,
    CONCAT(u1.nombre, ' ', u1.apellido) AS participante1_usuario,
    cl1.nombre AS participante1_club,
    p2.id AS participante2_id,
    p2.nombre_robot AS participante2_robot,
    CONCAT(u2.nombre, ' ', u2.apellido) AS participante2_usuario,
    cl2.nombre AS participante2_club,
    CONCAT(juez.nombre, ' ', juez.apellido) AS juez_nombre,
    cat.nombre AS categoria_nombre,
    CASE 
        WHEN e.resultado = 'GANA_1' THEN p1.nombre_robot
        WHEN e.resultado = 'GANA_2' THEN p2.nombre_robot
        ELSE NULL
    END AS ganador_robot
FROM enfrentamientos e
INNER JOIN torneos t ON e.torneo_id = t.id
INNER JOIN participantes p1 ON e.participante1_id = p1.id
INNER JOIN participantes p2 ON e.participante2_id = p2.id
INNER JOIN users u1 ON p1.usuario_id = u1.id
INNER JOIN users u2 ON p2.usuario_id = u2.id
LEFT JOIN clubs cl1 ON u1.club_id = cl1.id
LEFT JOIN clubs cl2 ON u2.club_id = cl2.id
LEFT JOIN users juez ON e.juez_id = juez.id
LEFT JOIN categorias cat ON t.categoria_id = cat.id;

-- 5️⃣ VISTA: Estadísticas de Clubs
DROP VIEW IF EXISTS vista_estadisticas_club;
CREATE VIEW vista_estadisticas_club AS
SELECT 
    cl.id AS club_id,
    cl.nombre AS club_nombre,
    cl.ciudad,
    cl.pais,
    CONCAT(owner.nombre, ' ', owner.apellido) AS club_owner,
    COUNT(DISTINCT u.id) AS total_miembros,
    COUNT(DISTINCT r.id) AS total_robots,
    COUNT(DISTINCT p.id) AS total_participaciones,
    SUM(CASE WHEN p.partidos_ganados > 0 THEN p.partidos_ganados ELSE 0 END) AS total_victorias,
    SUM(CASE WHEN p.partidos_perdidos > 0 THEN p.partidos_perdidos ELSE 0 END) AS total_derrotas,
    SUM(CASE WHEN p.partidos_empatados > 0 THEN p.partidos_empatados ELSE 0 END) AS total_empates,
    SUM(CASE WHEN p.puntuacion_total > 0 THEN p.puntuacion_total ELSE 0 END) AS puntuacion_acumulada,
    COUNT(DISTINCT CASE 
        WHEN ht.club_ganador_id = cl.id AND ht.tipo_evento = 'TORNEO' 
        THEN ht.torneo_id 
    END) AS torneos_ganados
FROM clubs cl
LEFT JOIN users owner ON cl.owner_id = owner.id
LEFT JOIN users u ON u.club_id = cl.id
LEFT JOIN robots r ON r.usuario_id = u.id
LEFT JOIN participantes p ON p.usuario_id = u.id
LEFT JOIN historial_torneos ht ON ht.club_ganador_id = cl.id
GROUP BY cl.id, cl.nombre, cl.ciudad, cl.pais, owner.nombre, owner.apellido;
