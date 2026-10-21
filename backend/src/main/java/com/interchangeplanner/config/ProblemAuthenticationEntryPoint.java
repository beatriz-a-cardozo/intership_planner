package com.interchangeplanner.config;

import com.interchangeplanner.shared.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde 401 no mesmo formato Problem Details do restante da API
 * (erros da cadeia de filtros não passam pelo @RestControllerAdvice).
 */
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        response.getWriter().write("""
                {"type":"about:blank","title":"Não autenticado","status":401,\
                "detail":"Sessão ausente ou expirada. Faça login novamente.","instance":"%s","traceId":%s}"""
                .formatted(escape(request.getRequestURI()), traceId == null ? "null" : "\"" + escape(traceId) + "\""));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
