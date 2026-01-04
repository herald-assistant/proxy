package com.acme.herald.web;

import com.acme.herald.web.dto.CommonDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonDtos.ApiError handleValidation(MethodArgumentNotValidException e) {
        List<CommonDtos.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        return new CommonDtos.ApiError(
                "VALIDATION_ERROR",
                "Niepoprawne dane wejściowe.",
                fields,
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonDtos.ApiError handleIllegalArg(IllegalArgumentException e) {
        return new CommonDtos.ApiError(
                "BAD_REQUEST",
                e.getMessage(),
                null,
                null
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public CommonDtos.ApiError handleForbidden(AccessDeniedException e) {
        return new CommonDtos.ApiError(
                "FORBIDDEN",
                "Brak uprawnień.",
                null,
                null
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public CommonDtos.ApiError handleAny(Throwable e, HttpServletRequest req) {
        // tu nie wypluwaj szczegółów w prod; loguj po swojej stronie
        return new CommonDtos.ApiError(
                "INTERNAL_ERROR",
                "Wystąpił błąd serwera.",
                null,
                null
        );
    }

    private CommonDtos.FieldError toFieldError(FieldError fe) {
        return new CommonDtos.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}
