package com.yhcanbay.opsbridge.exception;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GitHubException.class)
    public ProblemDetail gitHubError(GitHubException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(GitRequestException.class)
    public ProblemDetail invalidGitRequest(GitRequestException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail projectNotFound(ProjectNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new TreeMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            String fieldName = requestFieldName(fieldError.getField());
            fields.putIfAbsent(fieldName, fieldError.getDefaultMessage());
        }

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Proje alanlarını kontrol edin.");
        problem.setProperty("fields", fields);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Geçerli JSON ve desteklenen yetenek değerleri gönderin.");
    }

    private String requestFieldName(String validationProperty) {
        if ("repositoryUrlValid".equals(validationProperty)) {
            return "repositoryUrl";
        }

        if ("healthUrlValid".equals(validationProperty)) {
            return "healthUrl";
        }

        return validationProperty;
    }
}
