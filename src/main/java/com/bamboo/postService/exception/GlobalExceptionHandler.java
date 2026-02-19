package com.bamboo.postService.exception;

import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.response.ApiError;
import com.bamboo.postService.common.response.RoleResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RoleNotFoundException.class, AccessDeniedException.class})
    public ResponseEntity<RoleResponse> handleRoleNotFoundResponse(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new RoleResponse(
                                false,
                                Roles.UNAUTHORIZED.toString(),
                                false,
                                HttpStatus.FORBIDDEN,
                                "User doesnot have access to this document"));
    }

    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<ApiError> handleEntityNotFoundException(
            Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<ApiError> handleUserNotFoundException(
            Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Database constraint violation";
        if (ex.getRootCause() != null) {
            message = ex.getRootCause().getMessage();
        }

        return buildError(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler({
        IOException.class,
        UnexpectedRollbackException.class,
        PersistenceException.class,
        DataAccessException.class
    })
    public ResponseEntity<ApiError> handleInternalError(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler({ResponseStatusException.class})
    public ResponseEntity<ApiError> handlerResponseMismatchError(
            Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> buildError(
            HttpStatus status, String message, HttpServletRequest request) {
        ApiError error =
                new ApiError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI());

        return ResponseEntity.status(status).body(error);
    }
}
