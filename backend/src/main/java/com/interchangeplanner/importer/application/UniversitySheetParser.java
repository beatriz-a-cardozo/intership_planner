package com.interchangeplanner.importer.application;

import com.interchangeplanner.importer.application.ImportModel.ProgramDraft;
import com.interchangeplanner.importer.application.ImportModel.Row;
import com.interchangeplanner.importer.application.ImportModel.RowStatus;
import com.interchangeplanner.importer.application.ImportModel.UniversityDraft;
import com.interchangeplanner.importer.application.ImportModel.UniversityRowDraft;
import com.interchangeplanner.importer.application.SheetReader.RawRow;
import com.interchangeplanner.university.api.UniversityDtos.IntakeDto;
import com.interchangeplanner.university.domain.UniversityEnums.ApplicationChannel;
import com.interchangeplanner.university.domain.UniversityEnums.DocumentSubmission;
import com.interchangeplanner.university.domain.UniversityEnums.InstitutionType;
import com.interchangeplanner.university.domain.UniversityEnums.Nature;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aba "Análise de Universidades" → universidades e programas (uma linha = um programa). Classe pura.
 *
 * <ul>
 *   <li>Latitude/longitude ignoradas; Tipo "Sim" vira Pública (aviso); ranking 0 = sem ranking; taxa "-" = vazio.</li>
 *   <li>Requisitos tri-estado: Sim / Não / "Se necessário (...)" → condicional.</li>
 *   <li>Inscrição: "direta", "uni-assist", "VPD"; qualquer outro nome é um portal (C@MPUS, unisono...).</li>
 *   <li>Prazo em texto vira janela (ver {@link DeadlineParser}); com dois semestres, a janela vai para o semestre cujo
 *       início combina com o prazo (prazo de abril a setembro → inverno; de outubro a março → verão).</li>
 *   <li>Linha sem instituição (ex.: só uma URL) é descartada.</li>
 * </ul>
 */
public final class UniversitySheetParser {

    public static final String SHEET = "Análise de Universidades";
    static final int WINTER_START = 10;
    static final int SUMMER_START = 4;
    private static final Pattern DEGREE = Pattern.compile("\\b(M\\.?\\s?Sc\\.?|M\\.?\\s?A\\.?|MBA|M\\.?\\s?Eng\\.?)(?=\\s|$)");

    private UniversitySheetParser() {
    }

    public static List<Row<UniversityRowDraft>> parse(List<RawRow> rows) {
        List<Row<UniversityRowDraft>> result = new ArrayList<>();
        for (RawRow r : rows) {
            if (r.text("Instituição") == null) {
                continue;
            }
            result.add(parse(r));
        }
        return result;
    }

    static Row<UniversityRowDraft> parse(RawRow r) {
        List<String> warnings = new ArrayList<>();
        String institution = r.text("Instituição");
        String programRaw = r.text("Programa");
        if (programRaw == null) {
            return new Row<>(r.number(), RowStatus.ERROR, institution, List.of("Linha sem programa."), null);
        }

        InstitutionType type = switch (ImportText.norm(r.text("Tipo"))) {
            case "publica" -> InstitutionType.PUBLIC;
            case "privada" -> InstitutionType.PRIVATE;
            default -> {
                warnings.add("Tipo \"" + r.text("Tipo") + "\" não reconhecido: considerada pública.");
                yield InstitutionType.PUBLIC;
            }
        };
        String nat = ImportText.norm(r.text("Natureza"));
        Nature nature = nat.contains("technische") ? Nature.TU : nat.contains("hochschule") ? Nature.HOCHSCHULE
                : nat.contains("universit") ? Nature.UNIVERSITY : Nature.OTHER;

        String city = r.text("Cidade");
        String fixed = city == null ? null : ImportText.CITY_FIXES.get(ImportText.norm(city));
        if (fixed != null) {
            warnings.add("Cidade \"" + city + "\" corrigida para \"" + fixed + "\".");
            city = fixed;
        }
        String country = r.text("País");
        String countryCode = country == null ? null : ImportText.COUNTRIES.get(ImportText.norm(country));
        if (country != null && countryCode == null) {
            warnings.add("País \"" + country + "\" não reconhecido.");
        }
        BigDecimal ranking = r.number("Ranking (deu)");
        Integer rank = ranking == null || ranking.signum() <= 0 ? null : ranking.intValue();
        BigDecimal population = r.number("Qnt. De Habitantes");

        UniversityDraft university = new UniversityDraft(institution, rank, type, nature, city, r.text("Estado"),
                countryCode, population == null ? null : population.intValue(), money(r, "Custo de vida", warnings),
                ImportText.employability(r.text("Empregabilidade")));

        Matcher degree = DEGREE.matcher(programRaw);
        String degreeText = null;
        String programName = programRaw;
        if (degree.find()) {
            degreeText = degree(degree.group(1));
            programName = (programRaw.substring(0, degree.start()) + programRaw.substring(degree.end())).strip()
                    .replaceAll("\\s{2,}", " ");
        }

        String channelText = r.text("Inscrição");
        ApplicationChannel channel = null;
        String portal = null;
        if (channelText != null) {
            switch (ImportText.norm(channelText)) {
                case "direta" -> channel = ApplicationChannel.DIRECT;
                case "uni-assist", "uniassist" -> channel = ApplicationChannel.UNI_ASSIST;
                case "vpd" -> channel = ApplicationChannel.VPD;
                default -> {
                    channel = ApplicationChannel.PORTAL;
                    portal = channelText;
                }
            }
        }
        String docs = ImportText.norm(r.text("Documentos"));
        DocumentSubmission submission = docs.contains("online") && docs.contains("fisico") ? DocumentSubmission.BOTH
                : docs.contains("fisico") ? DocumentSubmission.PHYSICAL
                : docs.contains("online") ? DocumentSubmission.ONLINE : null;

        String languageSummary = r.text("Idioma");
        var proficiency = ProficiencyParser.parse(r.text("Nivel de Proficiência"), languageSummary);
        warnings.addAll(proficiency.warnings());

        String deadlineText = r.text("Prazo de inscrição");
        DeadlineParser.Window window = DeadlineParser.parse(deadlineText);
        if (window.warning() != null) {
            warnings.add(window.warning());
        }
        List<IntakeDto> intakes = intakes(r.text("Início"), window);

        BigDecimal duration = r.number("Duração (Sem)");
        BigDecimal nonEu = money(r, "Taxa de estrangeiro", warnings);
        ProgramDraft program = new ProgramDraft(programName, degreeText, r.text("Área"), languageSummary,
                duration == null ? null : duration.intValue(), ImportText.requirement(r.text("GRE")),
                ImportText.requirement(r.text("Apitute assessment")), ImportText.requirement(r.text("Entrevista")),
                ImportText.requirement(r.text("NC")), channel, portal, submission, money(r, "Taxa Semestral", warnings),
                nonEu != null && nonEu.signum() == 0 ? null : nonEu, ImportText.yesNo(r.text("Bolsa")),
                r.text("Detalhes da bolsa"), ImportText.rating(r.text("Chance de bolsa")),
                ImportText.rating(r.text("Chance")), deadlineText, proficiency.requirements(), intakes);

        String label = institution + " · " + programName;
        return new Row<>(r.number(), warnings.isEmpty() ? RowStatus.VALID : RowStatus.WARNING, label, warnings,
                new UniversityRowDraft(university, program));
    }

    /** "M. Sc." / "MSc" → "M.Sc."; "MA" → "M.A."; "M.Eng" → "M.Eng."; "MBA" fica igual. */
    static String degree(String raw) {
        return switch (raw.replaceAll("[\\s.]", "").toUpperCase()) {
            case "MSC" -> "M.Sc.";
            case "MA" -> "M.A.";
            case "MENG" -> "M.Eng.";
            default -> raw.strip();
        };
    }

    /** Semestres de "Início" com a janela no semestre que combina com o mês do prazo. */
    static List<IntakeDto> intakes(String start, DeadlineParser.Window window) {
        String n = ImportText.norm(start);
        boolean winter = n.contains("winter");
        boolean summer = n.contains("sommer") || n.contains("summer");
        if (!winter && !summer) {
            winter = true;
        }
        Term withWindow;
        if (winter && summer) {
            Integer close = window.closeMonth();
            withWindow = close != null && (close >= 10 || close <= 3) ? Term.SUMMER : Term.WINTER;
        } else {
            withWindow = winter ? Term.WINTER : Term.SUMMER;
        }
        List<IntakeDto> result = new ArrayList<>();
        if (winter) {
            result.add(intake(Term.WINTER, withWindow == Term.WINTER ? window : null));
        }
        if (summer) {
            result.add(intake(Term.SUMMER, withWindow == Term.SUMMER ? window : null));
        }
        return result;
    }

    private static IntakeDto intake(Term term, DeadlineParser.Window w) {
        int startMonth = term == Term.WINTER ? WINTER_START : SUMMER_START;
        return w == null
                ? new IntakeDto(term, null, null, null, null, startMonth)
                : new IntakeDto(term, w.openMonth(), w.openDay(), w.closeMonth(), w.closeDay(), startMonth);
    }

    /** Valor em EUR; "-" ou texto vira vazio com aviso. */
    private static BigDecimal money(RawRow r, String header, List<String> warnings) {
        BigDecimal n = r.number(header);
        if (n != null) {
            return n.setScale(2, RoundingMode.HALF_EVEN);
        }
        String text = r.text(header);
        if (text != null) {
            warnings.add(header + " \"" + text + "\" não é um valor: ficou vazio.");
        }
        return null;
    }
}
