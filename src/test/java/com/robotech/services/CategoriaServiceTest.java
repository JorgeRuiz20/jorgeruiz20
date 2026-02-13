package com.robotech.services;

import com.robotech.models.Categoria;
import com.robotech.repositories.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private SimilarityService similarityService;

    @InjectMocks
    private CategoriaService categoriaService;

    // ============ DATOS ============
    private Categoria categoriaLigero;
    private Categoria categoriaPesado;

    @BeforeEach
    void setUp() {
        categoriaLigero = new Categoria();
        categoriaLigero.setId(1L);
        categoriaLigero.setNombre("Ligero");
        categoriaLigero.setPesoMaximo(1500);
        categoriaLigero.setDescripcion("Robots hasta 1.5kg");
        categoriaLigero.setActiva(true);

        categoriaPesado = new Categoria();
        categoriaPesado.setId(2L);
        categoriaPesado.setNombre("Pesado");
        categoriaPesado.setPesoMaximo(5000);
        categoriaPesado.setDescripcion("Robots hasta 5kg");
        categoriaPesado.setActiva(true);
    }

    // ============ TESTS ============

    @Test
    void testObtenerCategoriasActivas() {
        when(categoriaRepository.findByActivaTrue())
            .thenReturn(Arrays.asList(categoriaLigero, categoriaPesado));

        List<Categoria> resultado = categoriaService.getCategoriasActivas();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(c -> c.getActiva()));
        verify(categoriaRepository, times(1)).findByActivaTrue();

        System.out.println("✅ TEST PASADO: Obtener categorías activas");
    }

    @Test
    void testObtenerCategoriaPorId() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaLigero));

        Optional<Categoria> resultado = categoriaService.getCategoriaById(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Ligero", resultado.get().getNombre());
        assertEquals(1500, resultado.get().getPesoMaximo());

        System.out.println("✅ TEST PASADO: Obtener categoría por ID");
    }

    @Test
    void testCrearCategoria() {
        when(categoriaRepository.findAll()).thenReturn(Collections.emptyList());
        when(similarityService.existeCategoriaSimilar(anyString(), anyList())).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoriaLigero);

        Categoria resultado = categoriaService.crearCategoria(categoriaLigero);

        assertNotNull(resultado);
        assertEquals("Ligero", resultado.getNombre());
        verify(categoriaRepository, times(1)).save(categoriaLigero);

        System.out.println("✅ TEST PASADO: Crear categoría");
    }

    @Test
    void testCrearCategoria_RechazarNombreSimilar() {
        when(categoriaRepository.findAll()).thenReturn(Arrays.asList(categoriaPesado));
        when(similarityService.existeCategoriaSimilar(anyString(), anyList())).thenReturn(true);
        when(similarityService.encontrarCategoriaSimilar(anyString(), anyList())).thenReturn("Pesado");

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> categoriaService.crearCategoria(categoriaLigero)
        );

        assertTrue(exception.getMessage().contains("nombre similar"));
        verify(categoriaRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar nombre similar");
    }

    @Test
    void testActualizarCategoria() {
        Categoria actualizada = new Categoria();
        actualizada.setNombre("Ligero Plus");
        actualizada.setDescripcion("Nueva descripción");
        actualizada.setPesoMaximo(2000);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaLigero));
        when(categoriaRepository.findAll()).thenReturn(Collections.emptyList());
        when(similarityService.existeCategoriaSimilar(anyString(), anyList())).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoriaLigero);

        Categoria resultado = categoriaService.actualizarCategoria(1L, actualizada);

        assertNotNull(resultado);
        verify(categoriaRepository, times(1)).save(categoriaLigero);

        System.out.println("✅ TEST PASADO: Actualizar categoría");
    }

    @Test
    void testDesactivarCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaLigero));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> {
            Categoria cat = i.getArgument(0);
            cat.setActiva(false);
            return cat;
        });

        Categoria resultado = categoriaService.desactivarCategoria(1L);

        assertNotNull(resultado);
        assertFalse(resultado.getActiva());
        verify(categoriaRepository, times(1)).save(categoriaLigero);

        System.out.println("✅ TEST PASADO: Desactivar categoría");
    }
}