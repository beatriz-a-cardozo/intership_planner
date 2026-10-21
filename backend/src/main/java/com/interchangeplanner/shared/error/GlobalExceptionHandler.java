package com.interchangeplanner.shared.error;

import com.interchangeplanner.shared.web.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte toda exceção em RFC 9457 Problem Details com formato único:
 * {@code type, title, status, detail, instance, traceId, errors[]}.
 * Nunca expõe stack trace nem mensagens internas em erros 5xx.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).headers(ex.getHeaders()).body(toProblem(ex));
    }

    /** Converte uma {@link ApiException} no Problem Details padrão (reutilizável por handlers locais). */
    public static ProblemDetail toProblem(ApiException ex) {
        ProblemDetail problem = problem(ex.getStatus(), ex.getTitle(), ex.getMessage());
        if (ex.getField() != null) {
            problem.setProperty("errors", List.of(new FieldError(ex.getField(), ex.getMessage())));
        }
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Conflito de edição",
                "Este registro foi alterado em outra sessão. Recarregue e tente novamente.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado", "Faça login para continuar.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para esta operação.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Se persistir, informe o código " + MDC.get(RequestIdFilter.MDC_KEY) + ".");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Erros tratados pela classe base (JSON malformado, método não suportado...) também ganham traceId. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("traceId", MDC.get(RequestIdFilter.MDC_KEY));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("traceId", MDC.get(RequestIdFilter.MDC_KEY));
        return problem;
    }

    private static String lastNode(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}
