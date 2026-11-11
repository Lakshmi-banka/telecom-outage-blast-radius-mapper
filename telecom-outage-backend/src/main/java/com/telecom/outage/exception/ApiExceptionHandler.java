package com.telecom.outage.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> missing(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("time", LocalDateTime.now(), "error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> bad(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("time", LocalDateTime.now(), "error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        Map<String, String> m = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(x -> m.put(x.getField(), x.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(Map.of("time", LocalDateTime.now(), "errors", m));
    }

    // ----------------------------------------------------------------------
    // Without this, a constraint violation escapes as a raw 500 -> ERROR
    // dispatch -> 403 with an empty body. Now you get the actual reason.
    // ----------------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        String reason = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("time", LocalDateTime.now(),
                        "error", "Database constraint violated",
                        "detail", String.valueOf(reason)));
    }

    // Everything the PythonEngineClient throws (engine rejected / empty response)
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> state(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("time", LocalDateTime.now(), "error", String.valueOf(e.getMessage())));
    }

    // Catch-all so no exception can ever surface as a silent 403 again
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("time", LocalDateTime.now(),
                        "error", e.getClass().getSimpleName(),
                        "detail", String.valueOf(e.getMessage())));
    }
}
