package com.interchangeplanner.importer.application;

import com.interchangeplanner.shared.text.TextKey;
import com.interchangeplanner.university.domain.UniversityEnums.Employability;
import com.interchangeplanner.university.domain.UniversityEnums.Rating;
import com.interchangeplanner.university.domain.UniversityEnums.Requirement;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Regras de texto da importação (puras): normalização, correção de digitação por similaridade e vocabulário. */
public final class ImportText {

    /** Similaridade mínima para considerar dois nomes o mesmo (Proeficiência ≈ Proficiência = 0,92). */
    static final double SAME_NAME = 0.8;

    static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("fevereiro", 2), Map.entry("marco", 3), Map.entry("abril", 4),
            Map.entry("maio", 5), Map.entry("junho", 6), Map.entry("julho", 7), Map.entry("agosto", 8),
            Map.entry("setembro", 9), Map.entry("outubro", 10), Map.entry("novembro", 11), Map.entry("dezembro", 12));

    static final Map<String, String> COUNTRIES = Map.ofEntries(
            Map.entry("alemanha", "DE"), Map.entry("austria", "AT"), Map.entry("belgica", "BE"), Map.entry("canada", "CA"),
            Map.entry("espanha", "ES"), Map.entry("estados unidos", "US"), Map.entry("finlandia", "FI"),
            Map.entry("franca", "FR"), Map.entry("holanda", "NL"), Map.entry("irlanda", "IE"), Map.entry("italia", "IT"),
            Map.entry("noruega", "NO"), Map.entry("portugal", "PT"), Map.entry("reino unido", "GB"),
            Map.entry("suecia", "SE"), Map.entry("suica", "CH"));

    /** Erros de digitação conhecidos de nomes de cidade. */
    static final Map<String, String> CITY_FIXES = Map.of("bayreich", "Bayreuth");

    private ImportText() {
    }

    /** Minúsculas, sem acento e com espaços simples (para comparar nomes). */
    public static String norm(String s) {
        return TextKey.of(s);
    }

    public static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** 1 − distância de edição / maior comprimento, sobre os nomes normalizados. */
    public static double similarity(String a, String b) {
        String x = norm(a);
        String y = norm(b);
        if (x.equals(y)) {
            return 1;
        }
        int max = Math.max(x.length(), y.length());
        return max == 0 ? 1 : 1 - (double) levenshtein(x, y) / max;
    }

    /** O nome conhecido mais parecido, se for parecido o bastante. */
    public static Optional<String> closest(String name, Collection<String> known) {
        String best = null;
        double score = 0;
        for (String k : known) {
            double s = similarity(name, k);
            if (s > score) {
                score = s;
                best = k;
            }
        }
        return score >= SAME_NAME ? Optional.of(best) : Optional.empty();
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** "Sim" → true, "Não" → false, outro → nulo. */
    public static Boolean yesNo(String s) {
        String n = norm(s);
        return n.equals("sim") ? Boolean.TRUE : n.equals("nao") ? Boolean.FALSE : null;
    }

    /** "Baixa", "Baixa-Média", "Média", "Média-Alta", "Alta". */
    public static Rating rating(String s) {
        return switch (norm(s).replace(" ", "")) {
            case "baixa" -> Rating.LOW;
            case "baixa-media" -> Rating.LOW_MEDIUM;
            case "media" -> Rating.MEDIUM;
            case "media-alta" -> Rating.MEDIUM_HIGH;
            case "alta" -> Rating.HIGH;
            default -> null;
        };
    }

    public static Employability employability(String s) {
        if (norm(s).equals("excelente")) {
            return Employability.EXCELLENT;
        }
        Rating r = rating(s);
        return r == null ? null : Employability.valueOf(r.name());
    }

    /** "Sim" exige, "Não" não exige, "Se necessário (...)" é condicional; vazio = desconhecido. */
    public static Requirement requirement(String s) {
        String n = norm(s);
        if (n.startsWith("se necessario")) {
            return Requirement.CONDITIONAL;
        }
        Boolean yes = yesNo(s);
        return yes == null ? Requirement.UNKNOWN : yes ? Requirement.REQUIRED : Requirement.NOT_REQUIRED;
    }

    public static Integer month(String s) {
        return MONTHS.get(norm(s));
    }
}
