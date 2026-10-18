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

    public List<?> listarPeliculas(String busqueda, boolean todas) {
        org.springframework.web.util.UriComponentsBuilder builder =
                org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(movieServiceUrl + "/api/movies");
        if (busqueda != null && !busqueda.isBlank()) {
            builder.queryParam("busqueda", busqueda);
        }
        if (todas) {
            builder.queryParam("todas", "true");
        }
        // toUri() (no toUriString): evita que RestTemplate vuelva a codificar '%'
        java.net.URI uri = builder.build().encode().toUri();
        return restTemplate.getForObject(uri, List.class);
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

    public Object actualizarPeliculaJson(Long id, Object request) {
        ResponseEntity<Object> resp = restTemplate.exchange(
                movieServiceUrl + "/api/movies/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Object.class);
        return resp.getBody();
    }

    public Object actualizarPeliculaMultipart(Long id, Object request, MultipartFile imagen) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("datos", new HttpEntity<>(request, jsonHeaders));

        if (imagen != null && !imagen.isEmpty()) {
            ByteArrayResource fileResource = new ByteArrayResource(imagen.getBytes()) {
                @Override
                public String getFilename() {
                    return imagen.getOriginalFilename();
                }
            };
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    imagen.getContentType() != null ? imagen.getContentType() : "application/octet-stream"));
            body.add("imagen", new HttpEntity<>(fileResource, fileHeaders));
        }

        ResponseEntity<Object> resp = restTemplate.exchange(
                movieServiceUrl + "/api/movies/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Object.class);
        return resp.getBody();
    }

    public Object crearFuncion(Object funcionRequest) {
        return restTemplate.postForObject(movieServiceUrl + "/api/funciones", funcionRequest, Object.class);
    }

    public Object actualizarFuncion(Long id, Object funcionRequest) {
        ResponseEntity<Object> resp = restTemplate.exchange(
                movieServiceUrl + "/api/funciones/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(funcionRequest),
                Object.class);
        return resp.getBody();
    }

    public void eliminarFuncion(Long id) {
        restTemplate.delete(movieServiceUrl + "/api/funciones/" + id);
    }
}
