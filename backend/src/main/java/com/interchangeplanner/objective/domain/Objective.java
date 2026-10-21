package com.interchangeplanner.objective.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "objective")
public class Objective extends BaseEntity {

    public static final int DEFAULT_HORIZON_MONTHS = 24;

    /** Dados editáveis do objetivo (mesmo formato na criação e na edição). */
    public record Data(String name, String description, String countryCode, String city, LocalDate startDate,
            LocalDate targetDate, String currency, TargetMode targetMode, BigDecimal targetAmount,
            BigDecimal plannedMonthlySaving, ObjectiveStatus status, String color, String icon, int horizonMonths,
            ExpenseScope expenseScope) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_mode", nullable = false, length = 10)
    private TargetMode targetMode;

    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "planned_monthly_saving", precision = 19, scale = 4)
    private BigDecimal plannedMonthlySaving;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private ObjectiveStatus status;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "icon", length = 40)
    private String icon;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "horizon_months", nullable = false)
    private short horizonMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_scope", nullable = false, length = 15)
    private ExpenseScope expenseScope;

    protected Objective() {
    }

    public static Objective create(UUID userId, Data data) {
        Objective o = new Objective();
        o.userId = userId;
        o.apply(data);
        return o;
    }

    public void update(Data data) {
        apply(data);
    }

    private void apply(Data d) {
        this.name = d.name().strip();
        this.description = blankToNull(d.description());
        this.countryCode = blankToNull(d.countryCode());
        this.city = blankToNull(d.city());
        this.startDate = d.startDate();
        this.targetDate = d.targetDate();
        this.currency = d.currency();
        this.targetMode = d.targetMode();
        // Meta derivada não guarda valor manual (evita um número "fantasma" divergente dos gastos)
        this.targetAmount = d.targetMode() == TargetMode.MANUAL ? d.targetAmount() : null;
        this.plannedMonthlySaving = d.plannedMonthlySaving();
        this.status = d.status();
        this.color = blankToNull(d.color());
        this.icon = blankToNull(d.icon());
        this.horizonMonths = (short) d.horizonMonths();
        this.expenseScope = d.expenseScope();
    }

    public void markPrimary(boolean primary) {
        this.primary = primary;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getCurrency() {
        return currency;
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getPlannedMonthlySaving() {
        return plannedMonthlySaving;
    }

    public ObjectiveStatus getStatus() {
        return status;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getHorizonMonths() {
        return horizonMonths;
    }

    public ExpenseScope getExpenseScope() {
        return expenseScope;
    }
}
