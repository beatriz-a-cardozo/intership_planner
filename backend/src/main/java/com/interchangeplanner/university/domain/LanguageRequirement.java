package com.interchangeplanner.university.domain;

import com.interchangeplanner.shared.domain.UuidV7;
import com.interchangeplanner.university.domain.UniversityEnums.Cefr;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Requisito de idioma de um programa (ex.: inglês B2, TOEFL ≥ 90; alemão A1). Texto original preservado. */
@Entity
@Table(name = "program_language_requirement")
public class LanguageRequirement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.generate();

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_cefr", length = 2)
    private Cefr minCefr;

    @Column(name = "test_name", length = 30)
    private String testName;

    @Column(name = "min_score", precision = 6, scale = 1)
    private BigDecimal minScore;

    @Column(name = "raw_text", length = 200)
    private String rawText;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected LanguageRequirement() {
    }

    public LanguageRequirement(String language, Cefr minCefr, String testName, BigDecimal minScore, String rawText,
            int sortOrder) {
        this.language = language.strip().toLowerCase();
        this.minCefr = minCefr;
        this.testName = University.blankToNull(testName);
        this.minScore = minScore;
        this.rawText = University.blankToNull(rawText);
        this.sortOrder = (short) sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public Cefr getMinCefr() {
        return minCefr;
    }

    public String getTestName() {
        return testName;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public String getRawText() {
        return rawText;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
