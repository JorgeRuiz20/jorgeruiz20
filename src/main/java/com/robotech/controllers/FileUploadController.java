package com.robotech.controllers;

import com.robotech.dto.FileUploadResponse;
import com.robotech.services.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Gestión de subida de archivos")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Subir archivo", description = "Sube un archivo al servidor")
    @PostMapping
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/upload/")
                .path(fileName)
                .toUriString();

        FileUploadResponse response = new FileUploadResponse(
                fileName, 
                fileDownloadUri,
                file.getContentType(),
                file.getSize()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Descargar archivo", description = "Descarga un archivo del servidor")
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("No se pudo leer el archivo");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Endpoint de prueba", description = "Verifica que el servicio de upload funciona")
    @GetMapping("/test")
    public String test() {
        return "Upload endpoint funcionando - " + java.time.LocalDateTime.now();
    }
}