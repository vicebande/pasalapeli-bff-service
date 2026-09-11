package com.pasalapeli.bff.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieClient {

    private final RestTemplate restTemplate;

    @Value("${services.movie-service.url:http://localhost:8082}")
    private String movieServiceUrl;

    public List<?> listarPeliculas(String busqueda) {
        String url = movieServiceUrl + "/api/movies";
        if (busqueda != null && !busqueda.isBlank()) {
            url += "?busqueda=" + busqueda;
        }
        return restTemplate.getForObject(url, List.class);
    }

    public Object obtenerPelicula(Long id) {
        return restTemplate.getForObject(movieServiceUrl + "/api/movies/" + id, Object.class);
    }

    public List<?> listarFunciones(Long peliculaId) {
        return restTemplate.getForObject(movieServiceUrl + "/api/movies/" + peliculaId + "/funciones", List.class);
    }

    public Object obtenerFuncion(Long funcionId) {
        return restTemplate.getForObject(movieServiceUrl + "/api/funciones/" + funcionId, Object.class);
    }

    public Object crearPeliculaJson(Object request) {
        return restTemplate.postForObject(movieServiceUrl + "/api/movies", request, Object.class);
    }

    public Object crearPeliculaMultipart(Object request, MultipartFile imagen) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> datosEntity = new HttpEntity<>(request, jsonHeaders);
        body.add("datos", datosEntity);

        if (imagen != null && !imagen.isEmpty()) {
            ByteArrayResource fileResource = new ByteArrayResource(imagen.getBytes()) {
                @Override
                public String getFilename() {
                    return imagen.getOriginalFilename();
                }
            };
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(imagen.getContentType() != null ? imagen.getContentType() : "application/octet-stream"));
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, fileHeaders);
            body.add("imagen", fileEntity);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Object> resp = restTemplate.exchange(movieServiceUrl + "/api/movies", HttpMethod.POST, requestEntity, Object.class);
        return resp.getBody();
    }

    public void eliminarPelicula(Long id) {
        restTemplate.delete(movieServiceUrl + "/api/movies/" + id);
    }

    public Object crearFuncion(Object funcionRequest) {
        return restTemplate.postForObject(movieServiceUrl + "/api/funciones", funcionRequest, Object.class);
    }
}
