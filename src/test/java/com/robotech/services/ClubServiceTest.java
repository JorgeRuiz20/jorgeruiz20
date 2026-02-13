package com.robotech.services;

import com.robotech.controllers.ClubController;
import com.robotech.models.*;
import com.robotech.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimilarityService similarityService; // ✅ FIX NPE

    @InjectMocks
    private ClubService clubService;

    private Club club1;
    private User admin;
    private User clubOwner;
    private Role roleClubOwner;

    @BeforeEach
    void setUp() {

        roleClubOwner = new Role();
        roleClubOwner.setId(3L);
        roleClubOwner.setNombre("ROLE_CLUB_OWNER");

        clubOwner = new User();
        clubOwner.setId(2L);
        clubOwner.setNombre("Juan");
        clubOwner.setApellido("Perez");
        clubOwner.setEmail("juan@club.com");
        clubOwner.setEstado("APROBADO");
        clubOwner.setRoles(new HashSet<>(List.of(roleClubOwner)));

        club1 = new Club();
        club1.setId(1L);
        club1.setNombre("RoboTech Lima");
        club1.setDescripcion("Club robotica");
        club1.setMaxParticipantes(20);
        club1.setActiva(true);
        club1.setMiembros(new ArrayList<>());
        club1.setOwner(clubOwner);

        admin = new User();
        admin.setId(1L);
    }

    // ================= TESTS =================

    @Test
    void testConsultarTodosLosClubs() {

        when(clubRepository.findAll()).thenReturn(List.of(club1));

        List<Club> result = clubService.getAllClubs();

        assertEquals(1, result.size());
        verify(clubRepository).findAll();
    }

    @Test
    void testCrearClubPorAdmin_OK() {

        ClubController.CreateClubRequest request =
                new ClubController.CreateClubRequest();

        request.setNombre("Nuevo Club");
        request.setDescripcion("Desc");
        request.setCiudad("Lima");
        request.setPais("Peru");
        request.setOwnerId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(clubOwner));
        when(clubRepository.findByOwnerId(2L)).thenReturn(Optional.empty());
        when(clubRepository.findAll()).thenReturn(List.of(club1));

        // ✅ MOCK similarity
        when(similarityService.existeClubSimilar(any(), any()))
                .thenReturn(false);

        when(clubRepository.save(any())).thenReturn(club1);

        Club result = clubService.createClubByAdmin(request);

        assertNotNull(result);
        verify(clubRepository).save(any());
    }

    @Test
    void testCrearClubPorAdmin_OwnerNoExiste() {

        ClubController.CreateClubRequest request =
                new ClubController.CreateClubRequest();

        request.setOwnerId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> clubService.createClubByAdmin(request));
    }

    @Test
    void testUpdateClub_OK() {

        Club newData = new Club();
        newData.setNombre("Nuevo Nombre");
        newData.setDescripcion("Nueva Desc");

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club1));
        when(clubRepository.save(any())).thenReturn(club1);

        Club result = clubService.updateClub(1L, newData, 2L);

        assertNotNull(result);
        verify(clubRepository).save(any());
    }

    @Test
    void testGetClubById_OK() {

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club1));

        Club result = clubService.getClubById(1L);

        assertEquals("RoboTech Lima", result.getNombre());
    }

    @Test
    void testGetClubByOwner_OK() {

        when(clubRepository.findClubByOwner(2L))
                .thenReturn(Optional.of(club1));

        Club result = clubService.getClubByOwner(2L);

        assertNotNull(result);
    }

    @Test
    void testGetMiembrosClub() {

        when(clubRepository.findByOwnerId(2L))
                .thenReturn(Optional.of(club1));

        when(userRepository.findByClubIdAndEstado(1L, "APROBADO"))
                .thenReturn(List.of(clubOwner));

        List<User> result = clubService.getMiembrosDelClub(2L);

        assertEquals(1, result.size());
    }
}
