package com.interchangeplanner.shared.text;

import java.text.Normalizer;
import java.util.Locale;

/** Chave de comparação de textos: minúsculas, sem acento e com espaços simples ("  München " → "munchen"). */
public final class TextKey {

    private TextKey() {
    }

    public static String of(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
