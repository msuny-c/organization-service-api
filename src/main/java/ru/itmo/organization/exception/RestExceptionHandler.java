package ru.itmo.organization.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class RestExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }
    
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(Exception ex) {
        if (ex instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .findFirst()
                    .map(violation -> violation.getMessage())
                    .orElse("Некорректные данные");
            return Map.of("error", message);
        }
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentials(BadCredentialsException ex) {
        return Map.of("error", "Неверные учетные данные");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbidden(AccessDeniedException ex) {
        return Map.of("error", "Доступ запрещен");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Некорректные данные");
        
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return Map.of("error", "Внутренняя ошибка сервера");
    }
}
