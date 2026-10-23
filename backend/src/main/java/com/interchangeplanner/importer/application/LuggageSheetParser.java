package com.interchangeplanner.importer.application;

import com.interchangeplanner.importer.application.ImportModel.LuggageDraft;
import com.interchangeplanner.importer.application.ImportModel.Row;
import com.interchangeplanner.importer.application.ImportModel.RowStatus;
import com.interchangeplanner.importer.application.SheetReader.RawRow;
import com.interchangeplanner.luggage.domain.LuggageBag;
import com.interchangeplanner.luggage.domain.LuggageItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aba "Mala" → malas e itens. Classe pura. O nome da mala, a categoria e o nível de peso só aparecem na primeira
 * linha de cada grupo e são propagados para as de baixo. "Nível de peso" vira um peso estimado (Leve 100 g, Médio
 * 500 g, Pesado 1,5 kg), editável depois.
 */
public final class LuggageSheetParser {

    public static final String SHEET = "Mala";

    private LuggageSheetParser() {
    }

    public static List<Row<LuggageDraft>> parse(List<RawRow> rows) {
        List<Row<LuggageDraft>> result = new ArrayList<>();
        String bag = null;
        String category = null;
        String weight = null;
        for (RawRow r : rows) {
            bag = r.text("Mala") != null ? r.text("Mala") : bag;
            category = r.text("Categoria") != null ? r.text("Categoria") : category;
            weight = r.text("Nível de peso") != null ? r.text("Nível de peso") : weight;
            String item = r.text("Item");
            if (item == null) {
                result.add(new Row<>(r.number(), RowStatus.ERROR, "Linha " + r.number(), List.of("Linha sem item."), null));
                continue;
            }
            List<String> warnings = new ArrayList<>();
            if (bag == null) {
                warnings.add("Sem mala: o item fica em \"ainda não alocados\".");
            }
            BigDecimal qty = r.number("Qnt.");
            int quantity = qty == null || qty.intValue() < 1 ? 1 : Math.min(qty.intValue(), 999);
            Integer grams = estimatedGrams(weight);
            String notes = weight == null ? null : "Peso estimado pelo nível \"" + weight + "\" da planilha.";
            result.add(new Row<>(r.number(), warnings.isEmpty() ? RowStatus.VALID : RowStatus.WARNING,
                    (bag == null ? "" : bag + " · ") + item, warnings,
                    new LuggageDraft(bag, bagType(bag), quantity, item, category(category), grams, notes)));
        }
        return result;
    }

    static LuggageBag.Type bagType(String bag) {
        String n = ImportText.norm(bag);
        if (n.contains("mao")) {
            return LuggageBag.Type.CARRY_ON;
        }
        if (n.contains("mochila") || n.contains("pessoal")) {
            return LuggageBag.Type.PERSONAL_ITEM;
        }
        return LuggageBag.Type.CHECKED;
    }

    static LuggageItem.Category category(String c) {
        String n = ImportText.norm(c);
        if (n.startsWith("document")) {
            return LuggageItem.Category.DOCUMENTS;
        }
        if (n.startsWith("roupa")) {
            return LuggageItem.Category.CLOTHING;
        }
        if (n.startsWith("eletron")) {
            return LuggageItem.Category.ELECTRONICS;
        }
        if (n.startsWith("higien")) {
            return LuggageItem.Category.HYGIENE;
        }
        if (n.startsWith("saude") || n.startsWith("remed")) {
            return LuggageItem.Category.HEALTH;
        }
        return LuggageItem.Category.OTHER;
    }

    static Integer estimatedGrams(String level) {
        return switch (ImportText.norm(level)) {
            case "leve" -> 100;
            case "medio" -> 500;
            case "pesado" -> 1500;
            default -> null;
        };
    }
}
