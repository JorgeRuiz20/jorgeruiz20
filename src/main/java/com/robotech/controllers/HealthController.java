package com.robotech.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping
    public String healthCheck() {
        return "✅ Servicio RoboTech funcionando - " + java.time.LocalDateTime.now();
    }
    
    @GetMapping("/db")
    public String dbHealthCheck() {
        return "✅ Base de datos conectada - " + java.time.LocalDateTime.now();
    }
}