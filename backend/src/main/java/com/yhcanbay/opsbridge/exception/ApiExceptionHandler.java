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

import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.enums.IntegrationState;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DockerIntegrationException.class)
    public ProblemDetail dockerError(DockerIntegrationException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (exception.getState() == IntegrationState.UNREACHABLE) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }

    @ExceptionHandler(JenkinsException.class)
    public ProblemDetail jenkinsError(JenkinsException exception) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        if (exception.getState() == IntegrationState.NOT_CONNECTED
                || exception.getState() == IntegrationState.DISABLED
                || exception.getState() == IntegrationState.NOT_SUPPORTED) {
            status = HttpStatus.BAD_REQUEST;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, exception.getMessage());
        problem.setProperty("jenkinsState", exception.getState());
        return problem;
    }

    @ExceptionHandler(GitHubConnectionNotFoundException.class)
    public ProblemDetail connectionNotFound(GitHubConnectionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(GitHubConnectionInUseException.class)
    public ProblemDetail connectionInUse(GitHubConnectionInUseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

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

        String detail = "Proje alanlarını kontrol edin.";
        if (exception.getBindingResult().getTarget() instanceof GitHubConnectionRequest) {
            detail = "GitHub bağlantı alanlarını kontrol edin.";
        }
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("fields", fields);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Geçerli JSON ve desteklenen yetenek değerleri gönderin.");
    }

    private String requestFieldName(String validationProperty) {
        if ("jenkinsBaseUrlRefValid".equals(validationProperty)) {
            return "jenkinsBaseUrlRef";
        }
        if ("repositoryUrlValid".equals(validationProperty)) {
            return "repositoryUrl";
        }

        if ("healthUrlValid".equals(validationProperty)) {
            return "healthUrl";
        }

        return validationProperty;
    }
}
