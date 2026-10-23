package com.interchangeplanner.luggage.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Peso de uma mala, em gramas inteiros (sem arredondamento). Classe pura.
 *
 * <ul>
 *   <li>Peso = Σ quantidade × peso unitário; itens sem peso informado não somam e são contados à parte, para a tela
 *       avisar que o total pode estar subestimado.</li>
 *   <li>Estado: sem limite → NO_LIMIT; até 90% → OK; acima de 90% até 100% → WARNING; acima de 100% → OVER.</li>
 *   <li>Excesso = max(peso − limite, 0); livre = max(limite − peso, 0).</li>
 * </ul>
 */
public final class LuggageWeightCalculator {

    public enum State {
        NO_LIMIT,
        OK,
        WARNING,
        OVER
    }

    /** @param unitWeightG nulo quando o peso não foi informado */
    public record Line(int quantity, Integer unitWeightG) {
    }

    /**
     * @param percent       peso/limite em % com 1 casa (nulo sem limite)
     * @param remainingG    quanto ainda cabe (nulo sem limite)
     */
    public record Result(int weightG, int itemsWithoutWeight, BigDecimal percent, State state, int excessG,
            Integer remainingG) {
    }

    private LuggageWeightCalculator() {
    }

    public static Result calculate(Integer maxWeightG, Collection<Line> lines) {
        long weight = 0;
        int withoutWeight = 0;
        for (Line l : lines) {
            if (l.unitWeightG() == null) {
                withoutWeight++;
            } else {
                weight += (long) l.quantity() * l.unitWeightG();
            }
        }
        int w = (int) Math.min(weight, Integer.MAX_VALUE);
        if (maxWeightG == null || maxWeightG <= 0) {
            return new Result(w, withoutWeight, null, State.NO_LIMIT, 0, null);
        }
        BigDecimal percent = BigDecimal.valueOf(w).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxWeightG), 1, RoundingMode.HALF_EVEN);
        // estado pelos gramas exatos, não pelo percentual arredondado (9.001 g de 10 kg = 90,0% mas já passou de 90%)
        State state = w > maxWeightG ? State.OVER : (long) w * 10 > (long) maxWeightG * 9 ? State.WARNING : State.OK;
        return new Result(w, withoutWeight, percent, state, Math.max(w - maxWeightG, 0), Math.max(maxWeightG - w, 0));
    }
}
