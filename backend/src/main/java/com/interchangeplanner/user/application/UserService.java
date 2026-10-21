package com.interchangeplanner.user.application;

import com.interchangeplanner.currency.CurrencyService;
import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.user.api.MeResponse;
import com.interchangeplanner.user.api.UpdateSettingsRequest;
import com.interchangeplanner.user.api.UserSettingsResponse;
import com.interchangeplanner.user.domain.AppUser;
import com.interchangeplanner.user.domain.UserRepository;
import com.interchangeplanner.user.domain.UserSettings;
import com.interchangeplanner.user.domain.UserSettingsRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    static final Set<String> SUPPORTED_LOCALES = Set.of("pt-BR", "en-US");

    private final UserRepository users;
    private final UserSettingsRepository settingsRepository;
    private final CurrencyService currencies;
    private final UserMapper mapper;

    public UserService(UserRepository users, UserSettingsRepository settingsRepository, CurrencyService currencies,
            UserMapper mapper) {
        this.users = users;
        this.settingsRepository = settingsRepository;
        this.currencies = currencies;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário"));
        return mapper.toMe(user, settingsOf(userId));
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse settings(UUID userId) {
        return mapper.toResponse(settingsOf(userId));
    }

    @Transactional
    public UserSettingsResponse updateSettings(UUID userId, UpdateSettingsRequest request) {
        if (!SUPPORTED_LOCALES.contains(request.locale())) {
            throw new BusinessRuleException("Idioma não suportado.", "locale");
        }
        if (!isValidZone(request.timezone())) {
            throw new BusinessRuleException("Fuso horário inválido.", "timezone");
        }
        if (!currencies.isSupported(request.primaryCurrency())) {
            throw new BusinessRuleException("Moeda não suportada.", "primaryCurrency");
        }
        if (!currencies.isSupported(request.secondaryCurrency())) {
            throw new BusinessRuleException("Moeda não suportada.", "secondaryCurrency");
        }
        if (request.primaryCurrency().equals(request.secondaryCurrency())) {
            throw new BusinessRuleException("A moeda secundária deve ser diferente da principal.", "secondaryCurrency");
        }

        UserSettings settings = settingsOf(userId);
        settings.update(request.theme(), request.locale(), request.timezone(), request.primaryCurrency(),
                request.secondaryCurrency());
        return mapper.toResponse(settings);
    }

    private UserSettings settingsOf(UUID userId) {
        return settingsRepository.findById(userId).orElseThrow(() -> new NotFoundException("Configurações"));
    }

    private static boolean isValidZone(String zone) {
        try {
            ZoneId.of(zone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
