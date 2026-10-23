package com.interchangeplanner.importer.application;

import com.interchangeplanner.expense.domain.Recurrence;
import com.interchangeplanner.importer.application.ImportModel.ExpenseDraft;
import com.interchangeplanner.importer.application.ImportModel.Row;
import com.interchangeplanner.importer.application.ImportModel.RowStatus;
import com.interchangeplanner.importer.application.SheetReader.RawRow;
import com.interchangeplanner.importer.application.SheetReader.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Aba "Informações dos Gastos" → gastos (plano técnico §3). Classe pura.
 *
 * <ul>
 *   <li>Categoria/ciclo com erro de digitação são unificados com o catálogo por similaridade (aviso).</li>
 *   <li>Tipo: Único, Mensal, Semestral, Anual, Por UNI (quantidade 5); as ocorrências ficam para o backend sugerir
 *       pelo horizonte do objetivo. Tipo desconhecido é erro (nunca total zero silencioso).</li>
 *   <li>Só é pago o gasto com Data de Pagamento: vira um pagamento com o Valor Pago (ou o preço, se vazio).</li>
 *   <li>Valor com fórmula (=314*0.7) usa o valor calculado; a fórmula vai para as observações.</li>
 *   <li>Data de gasto não pago que já passou é movida para o ano seguinte (aviso) — corrige as datas de 2026 da
 *       passagem e da mala extra.</li>
 * </ul>
 */
public final class ExpenseSheetParser {

    public static final String SHEET = "Informações dos Gastos";
    static final int PER_UNIT_QUANTITY = 5;

    private ExpenseSheetParser() {
    }

    public static List<Row<ExpenseDraft>> parse(List<RawRow> rows, Collection<String> categories,
            Collection<String> cycles, LocalDate today) {
        List<Row<ExpenseDraft>> result = new ArrayList<>();
        for (RawRow r : rows) {
            result.add(parse(r, categories, cycles, today));
        }
        return result;
    }

    static Row<ExpenseDraft> parse(RawRow r, Collection<String> categories, Collection<String> cycles, LocalDate today) {
        List<String> warnings = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        String name = r.text("Gasto");
        if (name == null) {
            return error(r, null, "Linha sem nome do gasto.");
        }
        Value price = r.get("Preço Unitário (R$)");
        if (price.number() == null) {
            return error(r, name, "Preço unitário ausente ou inválido.");
        }
        BigDecimal unit = money(price.number());
        if (unit.signum() < 0) {
            return error(r, name, "Preço negativo.");
        }
        if (price.formula() != null) {
            notes.add("Valor calculado na planilha: =" + price.formula());
        }
        Recurrence recurrence = recurrence(r.text("Tipo"));
        if (recurrence == null) {
            return error(r, name, "Tipo \"" + r.text("Tipo") + "\" desconhecido (use Único, Mensal, Semestral, Anual ou Por UNI).");
        }
        int quantity = recurrence == Recurrence.PER_UNIT ? PER_UNIT_QUANTITY : 1;
        if (recurrence == Recurrence.PER_UNIT) {
            warnings.add("\"Por UNI\": quantidade " + PER_UNIT_QUANTITY + " (uma por universidade); ajuste se precisar.");
        }

        String category = match(r.text("Categoria"), categories, "Categoria", warnings);
        String cycle = match(r.text("Ciclo"), cycles, "Ciclo", warnings);
        Boolean required = ImportText.yesNo(r.text("Obrigatório?"));
        if (required == null) {
            required = true;
            warnings.add("\"Obrigatório?\" vazio: considerado obrigatório.");
        }

        LocalDate paidOn = r.get("Data de Pagamento").asDate();
        BigDecimal paidAmount = null;
        if (paidOn != null) {
            BigDecimal paid = r.number("Valor Pago");
            paidAmount = money(paid != null ? paid : unit);
            if (paidAmount.signum() <= 0) {
                paidOn = null;
                paidAmount = null;
            }
        } else if ("pago".equals(ImportText.norm(r.text("Status")))) {
            warnings.add("Marcado como pago sem data de pagamento: fica pendente.");
        }

        LocalDate due = r.get("Data").asDate();
        if (due != null && paidOn == null && due.isBefore(today)) {
            LocalDate moved = due.plusYears(1);
            warnings.add("Data " + due + " já passou e o gasto não foi pago: movida para " + moved + ".");
            due = moved;
        }

        String extra = r.text("#o");
        if (extra != null) {
            notes.add(extra.replaceFirst("(?i)^obs\\.?\\s*", ""));
        }
        ExpenseDraft draft = new ExpenseDraft(name, category, cycle, "BRL", unit, recurrence, quantity, required, due,
                r.text("Detalhes"), notes.isEmpty() ? null : String.join("\n", notes), paidOn, paidAmount);
        return new Row<>(r.number(), warnings.isEmpty() ? RowStatus.VALID : RowStatus.WARNING, name, warnings, draft);
    }

    static Recurrence recurrence(String tipo) {
        return switch (ImportText.norm(tipo)) {
            case "unico" -> Recurrence.ONE_TIME;
            case "mensal" -> Recurrence.MONTHLY;
            case "semestral" -> Recurrence.SEMIANNUAL;
            case "anual" -> Recurrence.ANNUAL;
            case "por uni", "por unidade" -> Recurrence.PER_UNIT;
            default -> null;
        };
    }

    /** Nome do catálogo mais parecido; sem parecido, mantém o da planilha (será criado na importação). */
    private static String match(String value, Collection<String> known, String what, List<String> warnings) {
        if (value == null) {
            return null;
        }
        Optional<String> closest = ImportText.closest(value, known);
        if (closest.isEmpty()) {
            warnings.add(what + " \"" + value + "\" não existe: será criada.");
            return value.strip();
        }
        if (!ImportText.norm(closest.get()).equals(ImportText.norm(value))) {
            warnings.add(what + " \"" + value + "\" unificada com \"" + closest.get() + "\".");
        }
        return closest.get();
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static Row<ExpenseDraft> error(RawRow r, String label, String message) {
        return new Row<>(r.number(), RowStatus.ERROR, label == null ? "Linha " + r.number() : label, List.of(message), null);
    }
}
