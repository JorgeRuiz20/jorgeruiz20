package com.robotech.config;

import com.robotech.models.*;
import com.robotech.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ DataInitializer - Pobla la base de datos con datos de prueba
 * 
 * Crea:
 * - Roles del sistema
 * - Usuarios (ADMIN, JUDGE, COMPETITORS, CLUB_OWNERS)
 * - Clubs de robótica
 * - Categorías de competencia
 * - Sedes de torneos
 * - Robots aprobados para competidores
 * - Torneo ACTIVO con competidores inscritos
 * 
 * Para probar el flujo del juez con enfrentamientos
 * 
 * 🔄 MODIFICADO: Ahora crea 16 competidores en lugar de 8
 */
@Component
@RequiredArgsConstructor
@Profile("!prod") // Solo ejecutar en desarrollo, no en producción
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final CategoriaRepository categoriaRepository;
    private final SedeRepository sedeRepository;
    private final RobotRepository robotRepository;
    private final TorneoRepository torneoRepository;
    private final ParticipanteRepository participanteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Solo inicializar si la base de datos está vacía
        if (roleRepository.count() > 0) {
            System.out.println("⏭️  Base de datos ya inicializada. Saltando DataInitializer...");
            return;
        }

        System.out.println("🚀 Iniciando DataInitializer...");

        // 1. Crear roles
        Map<String, Role> roles = crearRoles();

        // 2. Crear categorías
        Map<String, Categoria> categorias = crearCategorias();

        // 3. Crear sedes
        Map<String, Sede> sedes = crearSedes();

        // 4. Crear clubs
        Map<String, Club> clubs = crearClubs();

        // 5. Crear usuarios (admin, jueces, club owners, competidores)
        Map<String, User> usuarios = crearUsuarios(roles, clubs);

        // 6. Crear robots para los competidores
        Map<String, Robot> robots = crearRobots(usuarios, categorias);

        // 7. Crear torneo ACTIVO con participantes
        Torneo torneo = crearTorneoConParticipantes(usuarios, sedes, categorias, robots);

        System.out.println("✅ DataInitializer completado exitosamente!");
        System.out.println("\n📊 RESUMEN:");
        System.out.println("   - Roles: " + roles.size());
        System.out.println("   - Categorías: " + categorias.size());
        System.out.println("   - Sedes: " + sedes.size());
        System.out.println("   - Clubs: " + clubs.size());
        System.out.println("   - Usuarios: " + usuarios.size());
        System.out.println("   - Robots: " + robots.size());
        System.out.println("   - Torneo creado: " + torneo.getNombre());
        System.out.println("   - Participantes: " + participanteRepository.countByTorneoId(torneo.getId()));

        imprimirCredenciales(usuarios);
    }

    private Map<String, Role> crearRoles() {
        System.out.println("🔐 Creando roles...");
        Map<String, Role> roles = new HashMap<>();

        String[] nombreRoles = { "ROLE_ADMIN", "ROLE_JUDGE", "ROLE_COMPETITOR", "ROLE_CLUB_OWNER", "ROLE_USER" };

        for (String nombreRole : nombreRoles) {
            Role role = new Role();
            role.setNombre(nombreRole);
            roles.put(nombreRole, roleRepository.save(role));
        }

        return roles;
    }

    private Map<String, Categoria> crearCategorias() {
        System.out.println("🏆 Creando categorías...");
        Map<String, Categoria> categorias = new HashMap<>();

        // Categoría Mini Sumo
        Categoria miniSumo = new Categoria();
        miniSumo.setNombre("Mini Sumo");
        miniSumo.setDescripcion("Robots pequeños de combate, estilo sumo");
        miniSumo.setEdadMinima(12);
        miniSumo.setEdadMaxima(18);
        miniSumo.setPesoMaximo(500); // gramos
        miniSumo.setActiva(true);
        categorias.put("MINI_SUMO", categoriaRepository.save(miniSumo));

        // Categoría Seguidor de Línea
        Categoria seguidorLinea = new Categoria();
        seguidorLinea.setNombre("Seguidor de Línea");
        seguidorLinea.setDescripcion("Robots que siguen una línea negra en el suelo");
        seguidorLinea.setEdadMinima(10);
        seguidorLinea.setEdadMaxima(16);
        seguidorLinea.setPesoMaximo(1000);
        seguidorLinea.setActiva(true);
        categorias.put("SEGUIDOR_LINEA", categoriaRepository.save(seguidorLinea));

        // Categoría Combate
        Categoria combate = new Categoria();
        combate.setNombre("Combate");
        combate.setDescripcion("Robots de combate estilo BattleBots");
        combate.setEdadMinima(15);
        combate.setEdadMaxima(25);
        combate.setPesoMaximo(3000);
        combate.setActiva(true);
        categorias.put("COMBATE", categoriaRepository.save(combate));

        return categorias;
    }

    private Map<String, Sede> crearSedes() {
        System.out.println("🏢 Creando sedes...");
        Map<String, Sede> sedes = new HashMap<>();

        // Sede 1: PUCP
        Sede pucp = new Sede();
        pucp.setNombre("PUCP - Pabellón de Ingeniería");
        pucp.setDireccion("Av. Universitaria 1801");
        pucp.setDistrito("San Miguel");
        pucp.setReferencia("Al costado del estadio de la PUCP");
        pucp.setCapacidadMaxima(200);
        pucp.setTieneEstacionamiento(true);
        pucp.setActiva(true);
        sedes.put("PUCP", sedeRepository.save(pucp));

        // Sede 2: UNI
        Sede uni = new Sede();
        uni.setNombre("UNI - Facultad de Ingeniería Mecánica");
        uni.setDireccion("Av. Túpac Amaru 210");
        uni.setDistrito("Rímac");
        uni.setReferencia("Ingreso por puerta 3");
        uni.setCapacidadMaxima(150);
        uni.setTieneEstacionamiento(true);
        uni.setActiva(true);
        sedes.put("UNI", sedeRepository.save(uni));

        // Sede 3: UTEC
        Sede utec = new Sede();
        utec.setNombre("UTEC - Campus Principal");
        utec.setDireccion("Jr. Medrano Silva 165");
        utec.setDistrito("Barranco");
        utec.setReferencia("A 2 cuadras del Parque Municipal");
        utec.setCapacidadMaxima(100);
        utec.setTieneEstacionamiento(false);
        utec.setActiva(true);
        sedes.put("UTEC", sedeRepository.save(utec));

        return sedes;
    }

    private Map<String, Club> crearClubs() {
        System.out.println("🏅 Creando clubs...");
        Map<String, Club> clubs = new HashMap<>();

        // Club 1: RoboTech Lima
        Club robotechLima = new Club();
        robotechLima.setNombre("RoboTech Lima");
        robotechLima.setDescripcion("Club pionero de robótica en Lima");
        robotechLima.setCiudad("Lima");
        robotechLima.setPais("Perú");
        robotechLima.setActiva(true);
        clubs.put("ROBOTECH_LIMA", clubRepository.save(robotechLima));

        // Club 2: MecaTronics
        Club mecatronics = new Club();
        mecatronics.setNombre("MecaTronics");
        mecatronics.setDescripcion("Especialistas en robots de combate");
        mecatronics.setCiudad("Lima");
        mecatronics.setPais("Perú");
        mecatronics.setActiva(true);
        clubs.put("MECATRONICS", clubRepository.save(mecatronics));

        // Club 3: TechnoKids
        Club technokids = new Club();
        technokids.setNombre("TechnoKids");
        technokids.setDescripcion("Robótica educativa para jóvenes");
        technokids.setCiudad("Lima");
        technokids.setPais("Perú");
        technokids.setActiva(true);
        clubs.put("TECHNOKIDS", clubRepository.save(technokids));

        // Club 4: InnovaBot
        Club innovabot = new Club();
        innovabot.setNombre("InnovaBot");
        innovabot.setDescripcion("Innovación en robótica competitiva");
        innovabot.setCiudad("Lima");
        innovabot.setPais("Perú");
        innovabot.setActiva(true);
        clubs.put("INNOVABOT", clubRepository.save(innovabot));

        return clubs;
    }

    private Map<String, User> crearUsuarios(Map<String, Role> roles, Map<String, Club> clubs) {
        System.out.println("👥 Creando usuarios...");
        Map<String, User> usuarios = new HashMap<>();

        // ADMIN
        User admin = crearUsuario(
                "admin@robotech.com", "Admin123!", "Carlos", "Mendoza", "12345678",
                "+51987654321", LocalDate.of(1985, 5, 15), null,
                Set.of(roles.get("ROLE_ADMIN")));
        admin.setEstado("APROBADO");
        usuarios.put("ADMIN", userRepository.save(admin));

        // JUEZ 1
        User juez1 = crearUsuario(
                "juez1@robotech.com", "Juez123!", "María", "Rodríguez", "23456789",
                "+51987654322", LocalDate.of(1980, 8, 20), null,
                Set.of(roles.get("ROLE_JUDGE")));
        juez1.setEstado("APROBADO");
        usuarios.put("JUEZ1", userRepository.save(juez1));

        // JUEZ 2
        User juez2 = crearUsuario(
                "juez2@robotech.com", "Juez123!", "Roberto", "Vargas", "34567890",
                "+51987654323", LocalDate.of(1982, 3, 10), null,
                Set.of(roles.get("ROLE_JUDGE")));
        juez2.setEstado("APROBADO");
        usuarios.put("JUEZ2", userRepository.save(juez2));

        // CLUB OWNERS
        User owner1 = crearUsuario(
                "owner1@robotech.com", "Owner123!", "Ana", "García", "45678901",
                "+51987654324", LocalDate.of(1978, 12, 5), clubs.get("ROBOTECH_LIMA"),
                Set.of(roles.get("ROLE_CLUB_OWNER")));
        owner1.setEstado("APROBADO");
        clubs.get("ROBOTECH_LIMA").setOwner(owner1);
        usuarios.put("OWNER1", userRepository.save(owner1));

        User owner2 = crearUsuario(
                "owner2@robotech.com", "Owner123!", "Luis", "Sánchez", "56789012",
                "+51987654325", LocalDate.of(1975, 6, 18), clubs.get("MECATRONICS"),
                Set.of(roles.get("ROLE_CLUB_OWNER")));
        owner2.setEstado("APROBADO");
        clubs.get("MECATRONICS").setOwner(owner2);
        usuarios.put("OWNER2", userRepository.save(owner2));

        // 🔄 MODIFICADO: COMPETIDORES - RoboTech Lima (6 competidores)
        String[][] competidoresRoboTech = {
                { "comp1@robotech.com", "Juan", "Pérez", "67890123" },
                { "comp2@robotech.com", "Pedro", "López", "78901234" },
                { "comp3@robotech.com", "Diego", "Martín", "89012345" },
                { "comp4@robotech.com", "Sofía", "Ramírez", "90123456" },
                { "comp5@robotech.com", "Carlos", "Hernández", "01234567" },
                { "comp6@robotech.com", "Laura", "González", "11223344" }
        };

        int counter = 1;
        for (String[] datos : competidoresRoboTech) {
            User comp = crearUsuario(
                    datos[0], "Comp123!", datos[1], datos[2], datos[3],
                    "+5198765" + String.format("%04d", 4326 + counter),
                    LocalDate.of(2005 + (counter % 3), 1 + (counter % 12), 1 + (counter % 28)),
                    clubs.get("ROBOTECH_LIMA"),
                    Set.of(roles.get("ROLE_COMPETITOR")));
            comp.setEstado("APROBADO");
            usuarios.put("COMP_RT_" + counter, userRepository.save(comp));
            counter++;
        }

        // 🔄 MODIFICADO: COMPETIDORES - MecaTronics (4 competidores)
        String[][] competidoresMeca = {
                { "comp7@robotech.com", "Lucía", "Torres", "22334455" },
                { "comp8@robotech.com", "Miguel", "Flores", "33445566" },
                { "comp9@robotech.com", "Carmen", "Silva", "44556677" },
                { "comp10@robotech.com", "Andrés", "Castro", "55667788" }
        };

        counter = 1;
        for (String[] datos : competidoresMeca) {
            User comp = crearUsuario(
                    datos[0], "Comp123!", datos[1], datos[2], datos[3],
                    "+5198765" + String.format("%04d", 5000 + counter),
                    LocalDate.of(2006 + (counter % 3), 1 + (counter % 12), 1 + (counter % 28)),
                    clubs.get("MECATRONICS"),
                    Set.of(roles.get("ROLE_COMPETITOR")));
            comp.setEstado("APROBADO");
            usuarios.put("COMP_MC_" + counter, userRepository.save(comp));
            counter++;
        }

        // 🔄 MODIFICADO: COMPETIDORES - TechnoKids (4 competidores)
        String[][] competidoresTech = {
                { "comp11@robotech.com", "Valeria", "Rojas", "66778899" },
                { "comp12@robotech.com", "Javier", "Morales", "77889900" },
                { "comp13@robotech.com", "Isabella", "Vega", "88990011" },
                { "comp14@robotech.com", "Daniel", "Méndez", "99001122" }
        };

        counter = 1;
        for (String[] datos : competidoresTech) {
            User comp = crearUsuario(
                    datos[0], "Comp123!", datos[1], datos[2], datos[3],
                    "+5198765" + String.format("%04d", 6000 + counter),
                    LocalDate.of(2007 + (counter % 3), 1 + (counter % 12), 1 + (counter % 28)),
                    clubs.get("TECHNOKIDS"),
                    Set.of(roles.get("ROLE_COMPETITOR")));
            comp.setEstado("APROBADO");
            usuarios.put("COMP_TK_" + counter, userRepository.save(comp));
            counter++;
        }

        // 🔄 NUEVO: COMPETIDORES - InnovaBot (2 competidores)
        String[][] competidoresInnova = {
                { "comp15@robotech.com", "Martín", "Fuentes", "10203040" },
                { "comp16@robotech.com", "Camila", "Ortiz", "20304050" }
        };

        counter = 1;
        for (String[] datos : competidoresInnova) {
            User comp = crearUsuario(
                    datos[0], "Comp123!", datos[1], datos[2], datos[3],
                    "+5198765" + String.format("%04d", 7000 + counter),
                    LocalDate.of(2008 + (counter % 3), 1 + (counter % 12), 1 + (counter % 28)),
                    clubs.get("INNOVABOT"),
                    Set.of(roles.get("ROLE_COMPETITOR")));
            comp.setEstado("APROBADO");
            usuarios.put("COMP_IB_" + counter, userRepository.save(comp));
            counter++;
        }

        return usuarios;
    }

    private User crearUsuario(String email, String password, String nombre, String apellido,
            String dni, String telefono, LocalDate fechaNacimiento,
            Club club, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNombre(nombre);
        user.setApellido(apellido);
        user.setDni(dni);
        user.setTelefono(telefono);
        user.setFechaNacimiento(fechaNacimiento);
        user.setClub(club);
        user.setRoles(roles);
        user.setEstado("APROBADO");
        return user;
    }

    private Map<String, Robot> crearRobots(Map<String, User> usuarios, Map<String, Categoria> categorias) {
        System.out.println("🤖 Creando robots...");
        Map<String, Robot> robots = new HashMap<>();

        Categoria miniSumo = categorias.get("MINI_SUMO");

        // 🔄 MODIFICADO: Robots para competidores de RoboTech Lima (6 robots)
        String[][] robotsRoboTech = {
                { "Thunder Bot", "Robot ágil con sensores ultrasónicos", "450" },
                { "Lightning", "Velocidad y precisión combinadas", "480" },
                { "Mega Destroyer", "Fuerza bruta optimizada", "495" },
                { "Speed Racer", "El más rápido del ring", "420" },
                { "Plasma Strike", "Ataque de plasma controlado", "465" },
                { "Nova Warrior", "Guerrero estelar", "455" }
        };

        for (int i = 0; i < robotsRoboTech.length; i++) {
            String[] datos = robotsRoboTech[i];
            User owner = usuarios.get("COMP_RT_" + (i + 1));
            Robot robot = crearRobot(
                    datos[0], datos[1], Integer.parseInt(datos[2]),
                    "Arduino Mega + Sensores Sharp", owner, miniSumo);
            robots.put("ROBOT_RT_" + (i + 1), robotRepository.save(robot));
        }

        // Robots para competidores de MecaTronics (4 robots)
        String[][] robotsMeca = {
                { "Iron Warrior", "Construcción robusta en aluminio", "490" },
                { "Shadow Fighter", "Movimientos impredecibles", "470" },
                { "Titan Force", "Potencia máxima garantizada", "500" },
                { "Vortex", "Spinning attack specialist", "485" }
        };

        for (int i = 0; i < robotsMeca.length; i++) {
            String[] datos = robotsMeca[i];
            User owner = usuarios.get("COMP_MC_" + (i + 1));
            Robot robot = crearRobot(
                    datos[0], datos[1], Integer.parseInt(datos[2]),
                    "ESP32 + Sensores IR + Motor DC", owner, miniSumo);
            robots.put("ROBOT_MC_" + (i + 1), robotRepository.save(robot));
        }

        // Robots para competidores de TechnoKids (4 robots)
        String[][] robotsTech = {
                { "Cyber Ninja", "Estrategia y velocidad", "430" },
                { "Robo Samurai", "Precisión japonesa", "475" },
                { "Tech Gladiator", "Luchador tecnológico", "460" },
                { "Electron Storm", "Tormenta eléctrica", "440" }
        };

        for (int i = 0; i < robotsTech.length; i++) {
            String[] datos = robotsTech[i];
            User owner = usuarios.get("COMP_TK_" + (i + 1));
            Robot robot = crearRobot(
                    datos[0], datos[1], Integer.parseInt(datos[2]),
                    "Raspberry Pi Pico + Motores Servo", owner, miniSumo);
            robots.put("ROBOT_TK_" + (i + 1), robotRepository.save(robot));
        }

        // 🔄 NUEVO: Robots para competidores de InnovaBot (2 robots)
        String[][] robotsInnova = {
                { "Quantum Crusher", "Tecnología cuántica aplicada", "478" },
                { "Nebula Fighter", "Combatiente cósmico", "492" }
        };

        for (int i = 0; i < robotsInnova.length; i++) {
            String[] datos = robotsInnova[i];
            User owner = usuarios.get("COMP_IB_" + (i + 1));
            Robot robot = crearRobot(
                    datos[0], datos[1], Integer.parseInt(datos[2]),
                    "STM32 + Giroscopio MPU6050", owner, miniSumo);
            robots.put("ROBOT_IB_" + (i + 1), robotRepository.save(robot));
        }

        return robots;
    }

    private Robot crearRobot(String nombre, String descripcion, Integer peso,
            String especificaciones, User usuario, Categoria categoria) {
        Robot robot = new Robot();
        robot.setNombre(nombre);
        robot.setDescripcion(descripcion);
        robot.setPeso(peso);
        robot.setEspecificacionesTecnicas(especificaciones);
        robot.setCodigoIdentificacion("ROB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        robot.setUsuario(usuario);
        robot.setCategoria(categoria);
        robot.setEstado("APROBADO"); // ✅ IMPORTANTE: Robots aprobados para poder inscribirse
        return robot;
    }

    private Torneo crearTorneoConParticipantes(Map<String, User> usuarios, Map<String, Sede> sedes,
            Map<String, Categoria> categorias, Map<String, Robot> robots) {
        System.out.println("🏆 Creando torneo con participantes...");

        // 🔄 MODIFICADO: Crear torneo ACTIVO para 16 participantes
        Torneo torneo = new Torneo();
        torneo.setNombre("Copa Nacional Mini Sumo 2025");
        torneo.setDescripcion("Torneo de robots mini sumo - 16 participantes");
        torneo.setSede(sedes.get("PUCP"));
        torneo.setCategoria(categorias.get("MINI_SUMO"));
        torneo.setEstado("ACTIVO"); // ✅ Estado ACTIVO para que el juez pueda trabajar
        torneo.setJuezResponsable(usuarios.get("JUEZ1"));
        torneo.setModalidad(null); // ✅ Sin modalidad - el juez la asignará
        torneo.setFechaInicio(LocalDateTime.now());
        torneo.setActivacionAutomatica(false);

        torneo = torneoRepository.save(torneo);

        // 🔄 MODIFICADO: Inscribir 16 participantes (potencia de 2 - válido para
        // ELIMINATORIA)
        // 6 de RoboTech Lima, 4 de MecaTronics, 4 de TechnoKids, 2 de InnovaBot

        List<String> participantesKeys = Arrays.asList(
                "COMP_RT_1", "COMP_RT_2", "COMP_RT_3", "COMP_RT_4", "COMP_RT_5", "COMP_RT_6",
                "COMP_MC_1", "COMP_MC_2", "COMP_MC_3", "COMP_MC_4",
                "COMP_TK_1", "COMP_TK_2", "COMP_TK_3", "COMP_TK_4",
                "COMP_IB_1", "COMP_IB_2");

        for (int i = 0; i < participantesKeys.size(); i++) {
            String userKey = participantesKeys.get(i);
            String robotKey = userKey.replace("COMP_", "ROBOT_");

            User competidor = usuarios.get(userKey);
            Robot robot = robots.get(robotKey);

            Participante participante = new Participante();
            participante.setUsuario(competidor);
            participante.setTorneo(torneo);
            participante.setRobot(robot);
            participante.setNombreRobot(robot.getNombre());
            participante.setDescripcionRobot(robot.getDescripcion());
            participante.setFotoRobot(robot.getFotoRobot());
            participante.setPuntuacionTotal(0);
            participante.setPartidosGanados(0);
            participante.setPartidosPerdidos(0);
            participante.setPartidosEmpatados(0);

            participanteRepository.save(participante);

            System.out.println("   ✅ Participante " + (i + 1) + ": " +
                    competidor.getNombre() + " " + competidor.getApellido() +
                    " con robot " + robot.getNombre());
        }

        System.out.println("\n🎮 Torneo listo para que el juez asigne modalidad y genere enfrentamientos!");
        System.out.println("   - Nombre: " + torneo.getNombre());
        System.out.println("   - Estado: " + torneo.getEstado());
        System.out
                .println("   - Modalidad: " + (torneo.getModalidad() != null ? torneo.getModalidad() : "SIN ASIGNAR"));
        System.out.println("   - Juez responsable: " + torneo.getJuezResponsable().getNombre() + " " +
                torneo.getJuezResponsable().getApellido());
        System.out.println("   - Participantes inscritos: 16");
        System.out.println("   - Bracket potencial ELIMINATORIA: OCTAVOS DE FINAL (16 participantes)");
        System.out.println("   - Bracket potencial TODOS_CONTRA_TODOS: 120 enfrentamientos");

        return torneo;
    }

    private void imprimirCredenciales(Map<String, User> usuarios) {
        System.out.println("\n🔑 CREDENCIALES DE ACCESO:");
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("ADMIN:");
        System.out.println("  Email: admin@robotech.com");
        System.out.println("  Password: Admin123!");
        System.out.println("\nJUEZ (para probar el sistema):");
        System.out.println("  Email: juez1@robotech.com");
        System.out.println("  Password: Juez123!");
        System.out.println("\nCOMPETIDORES (ejemplos):");
        System.out.println("  Email: comp1@robotech.com");
        System.out.println("  Password: Comp123!");
        System.out.println("\n  Email: comp8@robotech.com");
        System.out.println("  Password: Comp123!");
        System.out.println("\n  Email: comp16@robotech.com");
        System.out.println("  Password: Comp123!");
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("\n📋 PRÓXIMOS PASOS PARA EL JUEZ:");
        System.out.println("1. Iniciar sesión como juez1@robotech.com");
        System.out.println("2. Ir al torneo 'Copa Nacional Mini Sumo 2025'");
        System.out.println("3. Asignar modalidad (POST /api/torneos/{id}/asignar-modalidad)");
        System.out.println("   - ELIMINATORIA: bracket de eliminación directa");
        System.out.println("   - TODOS_CONTRA_TODOS: todos juegan contra todos");
        System.out.println("4. Generar enfrentamientos (POST /api/torneos/{id}/generar-enfrentamientos)");
        System.out.println("5. Registrar resultados (PUT /api/torneos/{id}/enfrentamientos/{enfId}/resultado)");
        System.out.println("6. Avanzar ganadores si es ELIMINATORIA (POST /api/torneos/{id}/avanzar-ganadores)");
        System.out.println("══════════════════════════════════════════════════\n");
    }
}