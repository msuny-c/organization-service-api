package ru.itmo.organization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("errorType", "Ресурс не найден");
        return "error";
    }
    
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationErrors(BindException ex, RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new HashMap<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        
        redirectAttributes.addFlashAttribute("validationErrors", errors);
        redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации данных");
        
        return "redirect:/organizations";
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleConstraintViolation(ConstraintViolationException ex, RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new HashMap<>();
        
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        
        redirectAttributes.addFlashAttribute("validationErrors", errors);
        redirectAttributes.addFlashAttribute("errorMessage", "Ошибка валидации данных");
        
        return "redirect:/organizations";
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/organizations";
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("error", "Произошла внутренняя ошибка сервера: " + ex.getMessage());
        model.addAttribute("errorType", "Внутренняя ошибка");
        return "error";
    }
}
