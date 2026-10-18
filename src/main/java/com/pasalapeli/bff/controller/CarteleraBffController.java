package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.client.MovieClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cartelera")
@RequiredArgsConstructor
public class CarteleraBffController {

    private final MovieClient movieClient;

    @GetMapping
    public ResponseEntity<List<?>> listarCartelera(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false, defaultValue = "false") boolean todas) {
        return ResponseEntity.ok(movieClient.listarPeliculas(busqueda, todas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPelicula(@PathVariable Long id) {
        return ResponseEntity.ok(movieClient.obtenerPelicula(id));
    }

    @GetMapping("/{id}/funciones")
    public ResponseEntity<List<?>> listarFunciones(@PathVariable Long id) {
        return ResponseEntity.ok(movieClient.listarFunciones(id));
    }

    @GetMapping("/funciones/{funcionId}")
    public ResponseEntity<?> obtenerFuncion(@PathVariable Long funcionId) {
        return ResponseEntity.ok(movieClient.obtenerFuncion(funcionId));
    }
}
