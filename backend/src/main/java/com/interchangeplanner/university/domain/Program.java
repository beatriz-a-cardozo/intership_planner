package com.interchangeplanner.university.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import com.interchangeplanner.shared.money.Money;
import com.interchangeplanner.university.domain.UniversityEnums.ApplicationChannel;
import com.interchangeplanner.university.domain.UniversityEnums.DocumentSubmission;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import com.interchangeplanner.university.domain.UniversityEnums.TuitionPeriod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Programa (curso) de uma universidade. Requisitos de idioma e intakes são filhos (substituídos na edição). */
@Entity
@Table(name = "program")
public class Program extends BaseEntity {

    public record Data(UUID universityId, String name, String area, String description, String officialUrl,
            String degree, String languageSummary, Integer durationSemesters, Requirement gre,
            Requirement aptitudeAssessment, Requirement interview, Requirement nc,
            ApplicationChannel applicationChannel, String portalName, DocumentSubmission documentSubmission,
            Money semesterFee, Money nonEuTuition, TuitionPeriod nonEuTuitionPeriod, Boolean scholarshipAvailable,
            String scholarshipDetails, Rating scholarshipChance, Rating admissionChance, String applicationDeadlineText,
            String notes, List<LanguageRequirement> languageRequirements, List<ProgramIntake> intakes) {
    }

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "university_id", nullable = false)
    private UUID universityId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "area", length = 500)
    private String area;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "degree", length = 40)
    private String degree;

    @Column(name = "language_summary", length = 60)
    private String languageSummary;

    @Column(name = "duration_semesters")
    private Short durationSemesters;

    @Enumerated(EnumType.STRING)
    @Column(name = "gre", nullable = false, length = 13)
    private Requirement gre;

    @Enumerated(EnumType.STRING)
    @Column(name = "aptitude_assessment", nullable = false, length = 13)
    private Requirement aptitudeAssessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview", nullable = false, length = 13)
    private Requirement interview;

    @Enumerated(EnumType.STRING)
    @Column(name = "nc", nullable = false, length = 13)
    private Requirement nc;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_channel", length = 12)
    private ApplicationChannel applicationChannel;

    @Column(name = "portal_name", length = 60)
    private String portalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_submission", length = 10)
    private DocumentSubmission documentSubmission;

    @Column(name = "semester_fee_amount", precision = 19, scale = 4)
    private BigDecimal semesterFeeAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "semester_fee_currency", length = 3)
    private String semesterFeeCurrency;

    @Column(name = "non_eu_tuition_amount", precision = 19, scale = 4)
    private BigDecimal nonEuTuitionAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "non_eu_tuition_currency", length = 3)
    private String nonEuTuitionCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "non_eu_tuition_period", length = 10)
    private TuitionPeriod nonEuTuitionPeriod;

    @Column(name = "scholarship_available")
    private Boolean scholarshipAvailable;

    @Column(name = "scholarship_details", length = 500)
    private String scholarshipDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "scholarship_chance", length = 12)
    private Rating scholarshipChance;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_chance", length = 12)
    private Rating admissionChance;

    @Column(name = "application_deadline_text", length = 200)
    private String applicationDeadlineText;

    @Column(name = "chosen", nullable = false)
    private boolean chosen;

    @Column(name = "notes", length = 2000)
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "program_id", nullable = false)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 64)
    private List<LanguageRequirement> languageRequirements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "program_id", nullable = false)
    @OrderBy("startMonth ASC")
    @BatchSize(size = 64)
    private List<ProgramIntake> intakes = new ArrayList<>();

    protected Program() {
    }

    public Program(UUID userId, Data data) {
        this.userId = userId;
        update(data);
    }

    public void update(Data d) {
        this.universityId = d.universityId();
        this.name = d.name().strip();
        this.area = University.blankToNull(d.area());
        this.description = University.blankToNull(d.description());
        this.officialUrl = University.blankToNull(d.officialUrl());
        this.degree = University.blankToNull(d.degree());
        this.languageSummary = University.blankToNull(d.languageSummary());
        this.durationSemesters = d.durationSemesters() == null ? null : d.durationSemesters().shortValue();
        this.gre = orUnknown(d.gre());
        this.aptitudeAssessment = orUnknown(d.aptitudeAssessment());
        this.interview = orUnknown(d.interview());
        this.nc = orUnknown(d.nc());
        this.applicationChannel = d.applicationChannel();
        this.portalName = University.blankToNull(d.portalName());
        this.documentSubmission = d.documentSubmission();
        this.semesterFeeAmount = d.semesterFee() == null ? null : d.semesterFee().amount();
        this.semesterFeeCurrency = d.semesterFee() == null ? null : d.semesterFee().currency();
        this.nonEuTuitionAmount = d.nonEuTuition() == null ? null : d.nonEuTuition().amount();
        this.nonEuTuitionCurrency = d.nonEuTuition() == null ? null : d.nonEuTuition().currency();
        this.nonEuTuitionPeriod = d.nonEuTuition() == null ? null : d.nonEuTuitionPeriod();
        this.scholarshipAvailable = d.scholarshipAvailable();
        this.scholarshipDetails = University.blankToNull(d.scholarshipDetails());
        this.scholarshipChance = d.scholarshipChance();
        this.admissionChance = d.admissionChance();
        this.applicationDeadlineText = University.blankToNull(d.applicationDeadlineText());
        this.notes = University.blankToNull(d.notes());
        this.languageRequirements.clear();
        this.languageRequirements.addAll(d.languageRequirements());
        this.intakes.clear();
        this.intakes.addAll(d.intakes());
    }

    private static Requirement orUnknown(Requirement r) {
        return r == null ? Requirement.UNKNOWN : r;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }

    public Money semesterFee() {
        return semesterFeeAmount == null ? null : Money.of(semesterFeeAmount, semesterFeeCurrency);
    }

    public Money nonEuTuition() {
        return nonEuTuitionAmount == null ? null : Money.of(nonEuTuitionAmount, nonEuTuitionCurrency);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getUniversityId() {
        return universityId;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public String getDescription() {
        return description;
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public String getDegree() {
        return degree;
    }

    public String getLanguageSummary() {
        return languageSummary;
    }

    public Integer getDurationSemesters() {
        return durationSemesters == null ? null : durationSemesters.intValue();
    }

    public Requirement getGre() {
        return gre;
    }

    public Requirement getAptitudeAssessment() {
        return aptitudeAssessment;
    }

    public Requirement getInterview() {
        return interview;
    }

    public Requirement getNc() {
        return nc;
    }

    public ApplicationChannel getApplicationChannel() {
        return applicationChannel;
    }

    public String getPortalName() {
        return portalName;
    }

    public DocumentSubmission getDocumentSubmission() {
        return documentSubmission;
    }

    public TuitionPeriod getNonEuTuitionPeriod() {
        return nonEuTuitionPeriod;
    }

    public Boolean getScholarshipAvailable() {
        return scholarshipAvailable;
    }

    public String getScholarshipDetails() {
        return scholarshipDetails;
    }

    public Rating getScholarshipChance() {
        return scholarshipChance;
    }

    public Rating getAdmissionChance() {
        return admissionChance;
    }

    public String getApplicationDeadlineText() {
        return applicationDeadlineText;
    }

    public boolean isChosen() {
        return chosen;
    }

    public String getNotes() {
        return notes;
    }

    public List<LanguageRequirement> getLanguageRequirements() {
        return languageRequirements;
    }

    public List<ProgramIntake> getIntakes() {
        return intakes;
    }
}
