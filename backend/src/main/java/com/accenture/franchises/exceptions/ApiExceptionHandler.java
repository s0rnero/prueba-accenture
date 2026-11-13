package com.accenture.franchises.exceptions;

import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException exception) {
        return problem(exception.status(), exception.type(), exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, "concurrent-modification",
                "El registro cambio desde que lo leiste. Recarga y vuelve a intentar.");
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException exception) {
        String detail = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", detail);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicate(DuplicateKeyException exception) {
        return problem(HttpStatus.CONFLICT, "duplicate",
                "Ya existe un registro con ese nombre en este nivel.");
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ProblemDetail handleInput(ServerWebInputException exception) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Cuerpo de la peticion invalido.");
    }

    private ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://acento.dev/problems/" + type));
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
