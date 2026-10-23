package com.interchangeplanner.importer.application;

import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageItem;
import com.interchangeplanner.university.api.UniversityDtos.IntakeDto;
import com.interchangeplanner.university.api.UniversityDtos.LanguageRequirementDto;
import com.interchangeplanner.university.domain.UniversityEnums.ApplicationChannel;
import com.interchangeplanner.university.domain.UniversityEnums.DocumentSubmission;
import com.interchangeplanner.university.domain.UniversityEnums.Employability;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rascunhos lidos da planilha. São guardados na sessão de importação e aplicados no "importar". */
public final class ImportModel {

    private ImportModel() {
    }

    /** VALID entra; WARNING entra com ressalva; ERROR não entra; DUPLICATE já existe (pular ou atualizar). */
    public enum RowStatus {
        VALID,
        WARNING,
        ERROR,
        DUPLICATE
    }

    public enum Sheet {
        EXPENSES,
        UNIVERSITIES,
        LUGGAGE
    }

    /** Linha lida: o rascunho é nulo quando a linha tem erro. */
    public record Row<T>(int row, RowStatus status, String label, List<String> messages, T draft) {

        public boolean importable() {
            return status != RowStatus.ERROR && draft != null;
        }

        public Row<T> asDuplicate(String message) {
            List<String> m = new ArrayList<>(messages);
            m.add(message);
            return new Row<>(row, RowStatus.DUPLICATE, label, m, draft);
        }
    }

    /** @param paidOn pagamento só quando a planilha tem Data de Pagamento */
    public record ExpenseDraft(String name, String category, String cycle, String currency, BigDecimal unitPrice,
            Recurrence recurrence, int quantity, boolean required, LocalDate dueDate, String description, String notes,
            LocalDate paidOn, BigDecimal paidAmount) {
    }

    public record UniversityDraft(String name, Integer ranking, InstitutionType institutionType, Nature nature,
            String city, String region, String countryCode, Integer population, BigDecimal costOfLiving,
            Employability employability) {
    }

    public record ProgramDraft(String name, String degree, String area, String languageSummary,
            Integer durationSemesters, Requirement gre, Requirement aptitudeAssessment, Requirement interview,
            Requirement nc, ApplicationChannel applicationChannel, String portalName,
            DocumentSubmission documentSubmission, BigDecimal semesterFee, BigDecimal nonEuTuition,
            Boolean scholarshipAvailable, String scholarshipDetails, Rating scholarshipChance, Rating admissionChance,
            String applicationDeadlineText, List<LanguageRequirementDto> languageRequirements, List<IntakeDto> intakes) {
    }

    public record UniversityRowDraft(UniversityDraft university, ProgramDraft program) {
    }

    /** @param unitWeightG peso estimado a partir do "Nível de peso" (editável depois) */
    public record LuggageDraft(String bagName, LuggageBag.Type bagType, int quantity, String name,
            LuggageItem.Category category, Integer unitWeightG, String notes) {
    }

    /** Tudo o que foi lido, guardado na sessão. */
    public record Payload(List<Row<ExpenseDraft>> expenses, List<Row<UniversityRowDraft>> universities,
            List<Row<LuggageDraft>> luggage, List<String> ignoredSheets) {
    }
}
