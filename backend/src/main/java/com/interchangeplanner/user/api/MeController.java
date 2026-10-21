package com.interchangeplanner.user.api;

import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Usuário")
public class MeController {

    private final UserService service;

    public MeController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Dados do usuário autenticado")
    public MeResponse me(CurrentUser user) {
        return service.me(user.id());
    }

    @GetMapping("/settings")
    @Operation(summary = "Preferências do usuário autenticado")
    public UserSettingsResponse settings(CurrentUser user) {
        return service.settings(user.id());
    }

    @PutMapping("/settings")
    @Operation(summary = "Atualiza preferências (tema, idioma, fuso, moedas)")
    public UserSettingsResponse updateSettings(CurrentUser user, @Valid @RequestBody UpdateSettingsRequest request) {
        return service.updateSettings(user.id(), request);
    }
}
