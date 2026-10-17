package com.pasalapeli.bff.exception;

/**
 * El usuario autenticado no pudo resolverse (sin JWT valido o sin cuenta).
 * GlobalExceptionHandler la traduce al mismo 401 JSON que devolvia el BFF antes.
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String message) {
        super(message);
    }
}
