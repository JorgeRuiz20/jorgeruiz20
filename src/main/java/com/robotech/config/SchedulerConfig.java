package com.robotech.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ✅ Habilita la programación de tareas automáticas
 * Necesario para que @Scheduled funcione
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Esta configuración es suficiente para habilitar @Scheduled
}