package com.galileo.cu.operaciones.controladores;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex, HttpServletRequest req) {
        if (ex.getMessage().contains("Fallo") || ex.getMessage().contains(" es nulo")) {
            req.setAttribute("ex", ex.getMessage());
            return new ResponseEntity<>("{\"message\":\"" + ex.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (ex.getMessage().contains("could not execute statement;")) {
            if (ex.getMessage().contains("constraint [uk_nombre_idUnidad];")) {
                String err = "{\"message\":\"Fallo, ya existe una operación con este nombre, en esta unidad.\"}";
                log.error(err, ex);
                req.setAttribute("ex", "Fallo, ya existe una operación con este nombre, en esta unidad.");
                return new ResponseEntity<>(err, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // Personaliza el mensaje de error
        log.error(ex.getMessage());
        req.setAttribute("ex", "Fallo, ocurrió un error inesperado.");
        return new ResponseEntity<>("{\"message\":\"Ocurrió un error inesperado\"}",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejar excepciones específicas de la base de datos
    // @ExceptionHandler(DataIntegrityViolationException.class)
    // public ResponseEntity<String>
    // handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    // return new ResponseEntity<>("Error de integridad de datos: " +
    // ex.getMostSpecificCause().getMessage(), HttpStatus.BAD_REQUEST);
    // }
}