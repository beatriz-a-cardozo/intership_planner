package com.interchangeplanner.auth.api;

import com.interchangeplanner.auth.application.AuthService;
import com.interchangeplanner.auth.application.InvalidRefreshTokenException;
import com.interchangeplanner.shared.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
@SecurityRequirements // rotas públicas: sem Bearer na documentação
public class AuthController {

    private final AuthService service;
    private final RefreshTokenCookies cookies;

    public AuthController(AuthService service, RefreshTokenCookies cookies) {
        this.service = service;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Cria uma conta e já inicia a sessão")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withSession(HttpStatus.CREATED, service.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica com e-mail e senha")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(HttpStatus.OK, service.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando o cookie de refresh (rotativo)")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        return withSession(HttpStatus.OK, service.refresh(cookies.read(request)));
    }

    /** Refresh inválido: além do 401, limpa o cookie para o navegador parar de reenviá-lo. */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> invalidRefresh(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.SET_COOKIE, cookies.clear())
                .body(GlobalExceptionHandler.toProblem(ex));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão atual")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        service.logout(cookies.read(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear()).build();
    }

    private ResponseEntity<AuthResponse> withSession(HttpStatus status, AuthService.Session session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        cookies.create(session.refreshToken().rawToken(), session.refreshToken().ttl()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(session.response());
    }
}
