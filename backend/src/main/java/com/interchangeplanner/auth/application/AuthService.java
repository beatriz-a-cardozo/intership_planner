package com.interchangeplanner.auth.application;

import com.interchangeplanner.auth.api.AuthResponse;
import com.interchangeplanner.auth.api.LoginRequest;
import com.interchangeplanner.auth.api.RegisterRequest;
import com.interchangeplanner.shared.error.ConflictException;
import com.interchangeplanner.user.application.UserService;
import com.interchangeplanner.user.domain.AppUser;
import com.interchangeplanner.user.domain.UserRegisteredEvent;
import com.interchangeplanner.user.domain.UserRepository;
import com.interchangeplanner.user.domain.UserSettings;
import com.interchangeplanner.user.domain.UserSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de autenticação. O controller só cuida de HTTP (cookie); aqui ficam as regras. */
@Service
public class AuthService {

    /** Resultado interno: corpo da resposta + refresh token em claro para o cookie. */
    public record Session(AuthResponse response, RefreshTokenService.Issued refreshToken) {
    }

    private final UserRepository users;
    private final UserSettingsRepository settings;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokens;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptService loginAttempts;
    private final ApplicationEventPublisher events;
    /** Hash fixo usado quando o e-mail não existe, para o tempo de resposta não revelar contas cadastradas. */
    private final String dummyHash;

    public AuthService(UserRepository users, UserSettingsRepository settings, UserService userService,
            PasswordEncoder passwordEncoder, AccessTokenService accessTokens, RefreshTokenService refreshTokens,
            LoginAttemptService loginAttempts, ApplicationEventPublisher events) {
        this.users = users;
        this.settings = settings;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
        this.events = events;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Session register(RegisterRequest request) {
        String email = AppUser.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailTaken();
        }
        AppUser user = AppUser.local(email, passwordEncoder.encode(request.password()), request.displayName());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Corrida entre dois cadastros simultâneos com o mesmo e-mail: o índice único decide.
            throw emailTaken();
        }
        settings.save(UserSettings.defaultsFor(user.getId()));
        events.publishEvent(new UserRegisteredEvent(user.getId()));
        return openSession(user.getId());
    }

    @Transactional
    public Session login(LoginRequest request) {
        String email = AppUser.normalizeEmail(request.email());
        loginAttempts.checkAllowed(email);

        Optional<AppUser> user = users.findByEmail(email);
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            loginAttempts.recordFailure(email);
            throw new InvalidCredentialsException();
        }
        loginAttempts.recordSuccess(email);
        return openSession(user.get().getId());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Session refresh(String rawRefreshToken) {
        RefreshTokenService.Issued rotated = refreshTokens.rotate(rawRefreshToken);
        return new Session(buildResponse(rotated.userId()), rotated);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private Session openSession(UUID userId) {
        RefreshTokenService.Issued refresh = refreshTokens.issueForNewSession(userId);
        return new Session(buildResponse(userId), refresh);
    }

    private AuthResponse buildResponse(UUID userId) {
        AccessTokenService.IssuedToken access = accessTokens.issue(userId);
        return new AuthResponse(access.value(), "Bearer", access.ttl().toSeconds(), userService.me(userId));
    }

    private static ConflictException emailTaken() {
        return new ConflictException("E-mail já cadastrado.", "email");
    }
}
