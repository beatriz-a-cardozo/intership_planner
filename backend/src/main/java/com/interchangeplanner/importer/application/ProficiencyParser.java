package com.interchangeplanner.importer.application;

import com.interchangeplanner.university.api.UniversityDtos.LanguageRequirementDto;
import com.interchangeplanner.university.domain.UniversityEnums.Cefr;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proficiência em texto livre → requisitos estruturados. Classe pura. O texto original fica em {@code rawText}.
 *
 * <ul>
 *   <li>{@code B2 (TOEFL >= 90)} com aulas em inglês → en B2, TOEFL ≥ 90</li>
 *   <li>{@code B2 (TOELF >= 88) + Alemão A1} → en B2 TOEFL ≥ 88 (digitação corrigida) + de A1</li>
 *   <li>{@code B2/A2} com aulas "Englisch/Deutsch" → en B2 + de A2</li>
 *   <li>{@code B2+ (TOEFL iBT >= 80)} → en B2 (o "+" fica no texto original)</li>
 * </ul>
 */
public final class ProficiencyParser {

    public record Result(List<LanguageRequirementDto> requirements, List<String> warnings) {
    }

    private static final Pattern CEFR = Pattern.compile("\\b([ABC][12])\\+?");
    private static final Pattern TEST = Pattern.compile("(TOEFL|TOELF|IELTS)(\\s*iBT)?\\s*>?=?\\s*(\\d+(?:[.,]\\d)?)",
            Pattern.CASE_INSENSITIVE);

    private ProficiencyParser() {
    }

    /**
     * @param text             coluna "Nível de Proficiência"
     * @param languageSummary  coluna "Idioma" (ex.: "Englisch", "Englisch/Deutsch") — define o idioma de cada nível
     */
    public static Result parse(String text, String languageSummary) {
        List<LanguageRequirementDto> reqs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (ImportText.blank(text)) {
            return new Result(reqs, warnings);
        }
        List<String> languages = languages(languageSummary);
        String raw = text.strip();
        int position = 0;
        for (String part : raw.split("\\+(?!\\s*\\()")) {
            String p = part.strip();
            if (p.isEmpty()) {
                continue;
            }
            String explicit = explicitLanguage(p);
            Matcher test = TEST.matcher(p);
            String testName = null;
            BigDecimal score = null;
            if (test.find()) {
                if (test.group(1).equalsIgnoreCase("TOELF")) {
                    warnings.add("\"TOELF\" lido como TOEFL.");
                }
                testName = test.group(1).equalsIgnoreCase("IELTS") ? "IELTS" : "TOEFL iBT";
                score = new BigDecimal(test.group(3).replace(',', '.'));
            }
            String withoutTest = TEST.matcher(p).replaceAll("");
            Matcher levels = CEFR.matcher(withoutTest);
            List<Cefr> found = new ArrayList<>();
            while (levels.find()) {
                found.add(Cefr.valueOf(levels.group(1)));
            }
            if (found.isEmpty()) {
                warnings.add("Proficiência \"" + p + "\" sem nível CEFR reconhecido.");
                continue;
            }
            for (int i = 0; i < found.size(); i++) {
                String lang = explicit != null ? explicit
                        : position < languages.size() ? languages.get(position) : "en";
                position++;
                boolean first = i == 0;
                reqs.add(new LanguageRequirementDto(lang, found.get(i), first ? testName : null, first ? score : null,
                        raw.length() > 200 ? raw.substring(0, 200) : raw));
            }
        }
        return new Result(reqs, warnings);
    }

    private static String explicitLanguage(String part) {
        String n = ImportText.norm(part);
        if (n.contains("alemao") || n.contains("deutsch")) {
            return "de";
        }
        if (n.contains("ingles") || n.contains("englisch") || n.contains("english")) {
            return "en";
        }
        return null;
    }

    /** "Englisch/Deutsch" → [en, de]. */
    static List<String> languages(String summary) {
        List<String> result = new ArrayList<>();
        if (ImportText.blank(summary)) {
            return result;
        }
        for (String s : summary.split("[/,;]")) {
            String n = ImportText.norm(s);
            if (n.startsWith("engl") || n.startsWith("ingl")) {
                result.add("en");
            } else if (n.startsWith("deut") || n.startsWith("alem")) {
                result.add("de");
            }
        }
        return result;
    }
}
