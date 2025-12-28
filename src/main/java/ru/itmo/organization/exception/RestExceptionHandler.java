package ru.itmo.organization.exception;

import java.sql.SQLException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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

    @ExceptionHandler(PessimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConcurrency(Exception ex) {
        return Map.of("error", "Конфликт параллельных изменений. Повторите запрос.");
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleStorageUnavailable(StorageUnavailableException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Хранилище временно недоступно. Повторите попытку позже.";
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoResourceFound(NoResourceFoundException ex) {
        return Map.of("error", "Ресурс не найден");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return Map.of("error", "Метод не поддерживается для этого ресурса");
    }

    @ExceptionHandler({TransactionSystemException.class, JpaSystemException.class})
    public ResponseEntity<Map<String, String>> handleTransactionalWrappers(RuntimeException ex) {
        if (isSerializationFailure(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Конфликт параллельных изменений. Повторите запрос."));
        }
        log.error("Unexpected transactional error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера"));
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

    private static boolean isSerializationFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof PessimisticLockingFailureException) {
                return true;
            }
            if (current instanceof SQLException sqlEx) {
                String sqlState = sqlEx.getSQLState();
                if ("40001".equals(sqlState) || "40P01".equals(sqlState)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        String message = ex.getMessage();
        return message != null && message.contains("could not serialize access");
    }
}
