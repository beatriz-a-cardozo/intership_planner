package com.interchangeplanner.university.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.university.domain.UniversityEnums.Employability;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "university")
public class University extends BaseEntity {

    public record Data(String name, Integer ranking, InstitutionType institutionType, Nature nature, String city,
            String region, String countryCode, Integer population, BigDecimal latitude, BigDecimal longitude,
            Money costOfLiving, Employability employability, String website, String notes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "ranking")
    private Integer ranking;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", length = 10)
    private InstitutionType institutionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", length = 12)
    private Nature nature;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "region", length = 120)
    private String region;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "population")
    private Integer population;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "cost_of_living_amount", precision = 19, scale = 4)
    private BigDecimal costOfLivingAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cost_of_living_currency", length = 3)
    private String costOfLivingCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "employability", length = 12)
    private Employability employability;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "notes", length = 2000)
    private String notes;

    /** Imagem exibida nos cards (gerenciada pelo UniversityImageService). */
    @Column(name = "cover_image_id")
    private UUID coverImageId;

    protected University() {
    }

    public University(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.name = d.name().strip();
        this.ranking = d.ranking();
        this.institutionType = d.institutionType();
        this.nature = d.nature();
        this.city = blankToNull(d.city());
        this.region = blankToNull(d.region());
        this.countryCode = blankToNull(d.countryCode());
        this.population = d.population();
        this.latitude = d.latitude();
        this.longitude = d.longitude();
        this.costOfLivingAmount = d.costOfLiving() == null ? null : d.costOfLiving().amount();
        this.costOfLivingCurrency = d.costOfLiving() == null ? null : d.costOfLiving().currency();
        this.employability = d.employability();
        this.website = blankToNull(d.website());
        this.notes = blankToNull(d.notes());
    }

    static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    public Money costOfLiving() {
        return costOfLivingAmount == null ? null : Money.of(costOfLivingAmount, costOfLivingCurrency);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Integer getRanking() {
        return ranking;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public Nature getNature() {
        return nature;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Integer getPopulation() {
        return population;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Employability getEmployability() {
        return employability;
    }

    public String getWebsite() {
        return website;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getCoverImageId() {
        return coverImageId;
    }

    public void setCoverImageId(UUID coverImageId) {
        this.coverImageId = coverImageId;
    }
}
