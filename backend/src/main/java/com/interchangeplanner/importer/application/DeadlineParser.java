package com.interchangeplanner.importer.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prazo em texto livre da planilha → janela de inscrição (dia/mês de abertura e de prazo). Classe pura.
 *
 * <ul>
 *   <li>{@code 01/Dezembro - 01/Março} → abre 1/12, fecha 1/3</li>
 *   <li>{@code x - 15/Novembro} → abertura desconhecida, fecha 15/11</li>
 *   <li>{@code 30/Abril-Junho} → abre 30/4, fecha em junho (dia assumido = 30, com aviso)</li>
 *   <li>{@code x} ou vazio → sem datas</li>
 * </ul>
 * O que não for reconhecido vira só texto (e aviso); o texto original é sempre preservado no programa.
 */
public final class DeadlineParser {

    /** @param warning nulo quando a leitura foi direta */
    public record Window(Integer openDay, Integer openMonth, Integer closeDay, Integer closeMonth, String warning) {

        public boolean empty() {
            return openMonth == null && closeMonth == null;
        }
    }

    private static final Pattern DAY_MONTH = Pattern.compile("^(?:(\\d{1,2})\\s*/\\s*)?([\\p{L}]+)$");

    private DeadlineParser() {
    }

    public static Window parse(String text) {
        if (ImportText.blank(text) || ImportText.norm(text).equals("x")) {
            return new Window(null, null, null, null, null);
        }
        String[] parts = text.split("-", -1);
        if (parts.length != 2) {
            return unreadable(text);
        }
        Part open = part(parts[0]);
        Part close = part(parts[1]);
        if (open == null || close == null || close.month == null) {
            return unreadable(text);
        }
        String warning = null;
        Integer closeDay = close.day;
        if (closeDay == null && open.day != null) {
            closeDay = open.day;
            warning = "Prazo \"" + text.strip() + "\" sem dia de fechamento: assumido dia " + closeDay + ".";
        }
        return new Window(open.day, open.month, closeDay, close.month, warning);
    }

    private record Part(Integer day, Integer month) {
    }

    /** "15/Novembro", "Junho" ou "x"/vazio (desconhecido); nulo se não reconhecer. */
    private static Part part(String raw) {
        String s = raw.strip();
        if (s.isEmpty() || ImportText.norm(s).equals("x")) {
            return new Part(null, null);
        }
        Matcher m = DAY_MONTH.matcher(s);
        if (!m.matches()) {
            return null;
        }
        Integer month = ImportText.month(m.group(2));
        if (month == null) {
            return null;
        }
        Integer day = m.group(1) == null ? null : Integer.valueOf(m.group(1));
        return day != null && (day < 1 || day > 31) ? null : new Part(day, month);
    }

    private static Window unreadable(String text) {
        return new Window(null, null, null, null,
                "Prazo \"" + text.strip() + "\" não reconhecido: ficou só como texto.");
    }
}
