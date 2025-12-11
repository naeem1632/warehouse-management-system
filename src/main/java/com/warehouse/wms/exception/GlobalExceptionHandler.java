package com.warehouse.wms.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application
 * Handles exceptions and provides user-friendly error messages
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException ex, Model model, HttpServletRequest request) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        model.addAttribute("errorTitle", "Something Went Wrong");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("requestUrl", request.getRequestURI());

        return "error/error";
    }

    /**
     * Handle data integrity violations (e.g., foreign key constraints, unique constraints)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, Model model, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);

        String userMessage = "Cannot complete the operation. This record is being used by other records in the system.";

        // Check for specific constraint violations
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("foreign key constraint")) {
                userMessage = "Cannot delete this record because it is being referenced by other records. Please remove the related records first.";
            } else if (ex.getMessage().contains("unique constraint") || ex.getMessage().contains("duplicate key")) {
                userMessage = "A record with the same information already exists. Please use a different value.";
            }
        }

        model.addAttribute("errorTitle", "Data Conflict");
        model.addAttribute("errorMessage", userMessage);
        model.addAttribute("errorCode", HttpStatus.CONFLICT.value());
        model.addAttribute("requestUrl", request.getRequestURI());

        return "error/error";
    }

    /**
     * Handle access denied exceptions (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        log.warn("Access denied: {} for URL: {}", ex.getMessage(), request.getRequestURI());

        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You don't have permission to access this resource. Please contact your administrator if you believe this is a mistake.");
        model.addAttribute("errorCode", HttpStatus.FORBIDDEN.value());
        model.addAttribute("requestUrl", request.getRequestURI());

        return "error/error";
    }

    /**
     * Handle 404 Not Found exceptions
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model, HttpServletRequest request) {
        log.warn("Page not found: {}", request.getRequestURI());

        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "The page you are looking for could not be found. It may have been moved or deleted.");
        model.addAttribute("errorCode", HttpStatus.NOT_FOUND.value());
        model.addAttribute("requestUrl", request.getRequestURI());

        return "error/error";
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        model.addAttribute("errorTitle", "Invalid Request");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorCode", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("requestUrl", request.getRequestURI());

        return "error/error";
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        model.addAttribute("errorTitle", "Unexpected Error");
        model.addAttribute("errorMessage", "An unexpected error occurred while processing your request. Please try again later.");
        model.addAttribute("errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("requestUrl", request.getRequestURI());
        model.addAttribute("technicalDetails", ex.getMessage());

        return "error/error";
    }
}
