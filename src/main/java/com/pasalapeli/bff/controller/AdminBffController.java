package com.pasalapeli.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasalapeli.bff.client.MovieClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminBffController {

    private final MovieClient movieClient;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/movies", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> crearPeliculaConImagen(
            @RequestPart("datos") String datosJson,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) throws IOException {
        log.info("BFF: Solicitud de creación de película con imagen (Admin)");
        Object datosObj = objectMapper.readValue(datosJson, Object.class);
        Object resultado = movieClient.crearPeliculaMultipart(datosObj, imagen);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping(value = "/movies", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> crearPeliculaJson(@RequestBody Object request) {
        log.info("BFF: Solicitud de creación de película JSON (Admin)");
        return ResponseEntity.status(HttpStatus.CREATED).body(movieClient.crearPeliculaJson(request));
    }

    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Void> eliminarPelicula(@PathVariable Long id) {
        log.info("BFF: Solicitud de eliminación de película ID: {}", id);
        movieClient.eliminarPelicula(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/funciones")
    public ResponseEntity<?> crearFuncion(@RequestBody Object funcionRequest) {
        log.info("BFF: Solicitud de creación de función (Admin)");
        return ResponseEntity.status(HttpStatus.CREATED).body(movieClient.crearFuncion(funcionRequest));
    }
}
