package com.interchangeplanner.notification.application;

import com.interchangeplanner.notification.api.NotificationDtos.PreferenceRequest;
import com.interchangeplanner.notification.api.NotificationDtos.PreferenceResponse;
import com.interchangeplanner.notification.domain.NotificationCategory;
import com.interchangeplanner.notification.domain.NotificationPreference;
import com.interchangeplanner.notification.domain.NotificationPreferenceRepository;
import com.interchangeplanner.shared.error.BusinessRuleException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Preferências por categoria; sem registro gravado valem os padrões da categoria. */
@Service
public class NotificationPreferenceService {

    public record Effective(boolean enabled, List<Integer> leadDays) {
    }

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<NotificationCategory, Effective> effective(UUID userId) {
        Map<NotificationCategory, NotificationPreference> saved = saved(userId);
        Map<NotificationCategory, Effective> result = new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory c : NotificationCategory.values()) {
            NotificationPreference p = saved.get(c);
            result.put(c, p == null ? new Effective(true, c.defaultLeadDays()) : new Effective(p.isEnabled(), p.getLeadDays()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> list(UUID userId) {
        Map<NotificationCategory, Effective> eff = effective(userId);
        return Arrays.stream(NotificationCategory.values())
                .map(c -> new PreferenceResponse(c, eff.get(c).enabled(), eff.get(c).leadDays(), c.defaultLeadDays(),
                        c.usesLeadDays()))
                .toList();
    }

    @Transactional
    public PreferenceResponse update(UUID userId, NotificationCategory category, PreferenceRequest r) {
        List<Integer> leads = category.usesLeadDays()
                ? (r.leadDays() == null ? category.defaultLeadDays() : r.leadDays())
                : List.of();
        if (category.usesLeadDays() && r.enabled() && leads.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos uma antecedência.", "leadDays");
        }
        NotificationPreference p = repository.findByUserIdAndCategory(userId, category)
                .orElseGet(() -> repository.save(new NotificationPreference(userId, category, r.enabled(), leads)));
        p.update(r.enabled(), leads);
        return new PreferenceResponse(category, p.isEnabled(), p.getLeadDays(), category.defaultLeadDays(),
                category.usesLeadDays());
    }

    private Map<NotificationCategory, NotificationPreference> saved(UUID userId) {
        return repository.findByUserId(userId).stream()
                .collect(Collectors.toMap(NotificationPreference::getCategory, Function.identity()));
    }
}
