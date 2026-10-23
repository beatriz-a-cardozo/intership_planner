package com.interchangeplanner.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.interchangeplanner.importer.application.DeadlineParser;
import com.interchangeplanner.importer.application.ImportText;
import com.interchangeplanner.importer.application.ProficiencyParser;
import com.interchangeplanner.university.api.UniversityDtos.LanguageRequirementDto;
import com.interchangeplanner.university.domain.UniversityEnums.Cefr;
import com.interchangeplanner.university.domain.UniversityEnums.Employability;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportRulesTest {

    @Test
    void digitacaoDeCategoriaEhUnificadaPorSimilaridade() {
        List<String> catalog = List.of("Proficiência", "Candidatura", "Documentos", "Viagem");
        assertThat(ImportText.closest("Proeficiência", catalog)).contains("Proficiência");
        assertThat(ImportText.closest("Candidaturas", catalog)).contains("Candidatura");
        assertThat(ImportText.closest("documentos", catalog)).contains("Documentos");
        assertThat(ImportText.closest("Emergência", catalog)).isEmpty();
    }

    @Test
    void vocabularioDaPlanilha() {
        assertThat(ImportText.yesNo("Sim")).isTrue();
        assertThat(ImportText.yesNo("Não")).isFalse();
        assertThat(ImportText.yesNo("Pago")).isNull();
        assertThat(ImportText.rating("Média-Alta")).isEqualTo(Rating.MEDIUM_HIGH);
        assertThat(ImportText.rating("Baixa-Média")).isEqualTo(Rating.LOW_MEDIUM);
        assertThat(ImportText.employability("Excelente")).isEqualTo(Employability.EXCELLENT);
        assertThat(ImportText.requirement("Se necessário (GRE)")).isEqualTo(Requirement.CONDITIONAL);
        assertThat(ImportText.requirement("Sim")).isEqualTo(Requirement.REQUIRED);
        assertThat(ImportText.requirement("")).isEqualTo(Requirement.UNKNOWN);
    }

    @Test
    void prazosEmTextoLivre() {
        assertThat(DeadlineParser.parse("01/Dezembro - 01/Março"))
                .isEqualTo(new DeadlineParser.Window(1, 12, 1, 3, null));
        assertThat(DeadlineParser.parse("01/Junho-15/Julho")).isEqualTo(new DeadlineParser.Window(1, 6, 15, 7, null));
        assertThat(DeadlineParser.parse("x - 15/Novembro")).isEqualTo(new DeadlineParser.Window(null, null, 15, 11, null));
        var ambiguous = DeadlineParser.parse("30/Abril-Junho");
        assertThat(ambiguous.closeMonth()).isEqualTo(6);
        assertThat(ambiguous.closeDay()).isEqualTo(30);
        assertThat(ambiguous.warning()).contains("assumido dia 30");
        assertThat(DeadlineParser.parse("x").empty()).isTrue();
        assertThat(DeadlineParser.parse("em breve").warning()).contains("não reconhecido");
    }

    @Test
    void proficienciaEmTextoLivre() {
        assertThat(ProficiencyParser.parse("B2 (TOEFL >= 90)", "Englisch").requirements()).containsExactly(
                new LanguageRequirementDto("en", Cefr.B2, "TOEFL iBT", new BigDecimal("90"), "B2 (TOEFL >= 90)"));

        var withGerman = ProficiencyParser.parse("B2 (TOELF >= 88) + Alemão A1", "Englisch");
        assertThat(withGerman.requirements()).extracting(LanguageRequirementDto::language, LanguageRequirementDto::minCefr,
                LanguageRequirementDto::minScore)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("en", Cefr.B2, new BigDecimal("88")),
                        org.assertj.core.groups.Tuple.tuple("de", Cefr.A1, null));
        assertThat(withGerman.warnings()).anyMatch(w -> w.contains("TOELF"));

        assertThat(ProficiencyParser.parse("B2/A2", "Englisch/Deutsch").requirements())
                .extracting(LanguageRequirementDto::language, LanguageRequirementDto::minCefr)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("en", Cefr.B2),
                        org.assertj.core.groups.Tuple.tuple("de", Cefr.A2));
        assertThat(ProficiencyParser.parse("B2+ (TOEFL iBT >= 80)", "Englisch").requirements().getFirst().minScore())
                .isEqualByComparingTo("80");
        assertThat(ProficiencyParser.parse("", "Englisch").requirements()).isEmpty();
    }
}
