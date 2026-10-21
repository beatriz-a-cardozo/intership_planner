# ADR 0007 — Gráficos em HTML/SVG próprios (sem biblioteca) nas Fases 2–3

**Status:** aceita · 2026-10-21

## Contexto
O plano listava Recharts. Os gráficos destas fases são: barras horizontais de série única (total por ciclo,
categoria ou periodicidade, com o trecho pago dentro), um medidor de duas partes (pago × a pagar) e uma linha da cotação.

## Decisão
Componentes próprios em `components/charts/` (`SplitBarList`, `SplitMeter`, `RateSparkline`), usando os tokens de
cor do tema: barras finas com pontas de 4px, 2px de espaço entre segmentos, legenda sempre presente, valores em
texto (identidade nunca só pela cor), tooltip no hover/foco por teclado e crosshair na linha. Cor de categoria só
como marcador ao lado do nome; a magnitude usa um único tom.

## Consequências
- Menos ~100 kB de JS e tema claro/escuro automático, sem configurar a biblioteca.
- Gráficos de séries temporais com eixos (Fase 4: receitas × despesas, sobra acumulada) podem justificar o Recharts;
  a decisão será revista lá.
