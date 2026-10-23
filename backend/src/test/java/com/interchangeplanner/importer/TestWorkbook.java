package com.interchangeplanner.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Planilha sintética com a mesma estrutura da original (cabeçalhos, fórmula, datas, digitação, coluna sem cabeçalho,
 * linha só com URL, mala com valores propagados). A planilha real não fica no repositório: tem dados pessoais.
 */
final class TestWorkbook {

    static final String[] EXPENSE_HEADERS = {"Gasto", "Categoria", "Ciclo", "Preço Unitário (R$)", "Preço Total (R$)",
            "Preço Unitário (€)", "Preço Total (€)", "Tipo", "Obrigatório?", "Data", "Detalhes", "Status",
            "Data de Pagamento", "Valor Pago"};

    static final String[] UNIVERSITY_HEADERS = {"Instituição", "Ranking (deu)", "Tipo", "Natureza", "Cidade",
            "Longitude", "Latitude", "Estado", "País", "Qnt. De Habitantes", "Custo de vida", "Empregabilidade",
            "Programa", "Área", "GRE", "Apitute assessment", "Entrevista", "NC", "Inscrição", "Documentos", "Idioma",
            "Nivel de Proficiência", "Início", "Prazo de inscrição", "Duração (Sem)", "Chance", "Taxa Semestral",
            "Taxa de estrangeiro", "Bolsa", "Detalhes da bolsa", "Chance de bolsa"};

    private TestWorkbook() {
    }

    static byte[] build(LocalDate today) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

            wb.createSheet("DASHBOARD");
            Sheet g = wb.createSheet("Informações dos Gastos");
            header(g, EXPENSE_HEADERS);
            expense(g, 1, date, "Agência de Intercâmbio", "Agência", "Preparação", 9500, null, "Único", "Sim",
                    today.plusMonths(3), "Pago", today.minusMonths(2), 9500.0, "Obs. Parcelado em 10x");
            expense(g, 2, date, "TOEFL iBT", "Proeficiência", "Preparação", 1317.68, null, "Único", "Sim",
                    today.plusDays(10), "Pago", today.minusMonths(1), 1317.68, "Obs. $227,85 USD");
            expense(g, 3, date, "Viagem para a prova", "Proeficiência", "Preparação", null, "314*0.7", "Único", "Sim",
                    today.plusDays(10), "Pago", today.minusMonths(1), 224.0, null);
            expense(g, 4, date, "Comprovação financeira - Sperrkonto", "Visto", "Imigração", 75000, null, "Único", "Sim",
                    today.plusMonths(9), "Não", null, null, null);
            expense(g, 5, date, "Aluguel", "Moradia", "Vivência", 2700, null, "Mensal", "Sim", null, "Sim", null, null, null);
            expense(g, 6, date, "Taxa semestral", "Universidade", "Vivência", 2400, null, "Semestral", "Sim", null, "Não",
                    null, null, null);
            expense(g, 7, date, "Manutenção da bicicleta", "Transporte", "Vivência", 500, null, "Anual", "Não", null, "Não",
                    null, null, null);
            expense(g, 8, date, "Taxa de inscrição", "Candidaturas", "Preparação", 0, null, "Por UNI", "Não",
                    today.plusMonths(2), "Não", null, null, null);
            expense(g, 9, date, "Passagem Aérea (IDA)", "Viagem", "Imigração", 4500, null, "Único", "Sim",
                    today.minusMonths(5), "Não", null, null, null);
            expense(g, 10, date, "Seguro trimestral", "Saúde", "Vivência", 300, null, "Trimestral", "Sim", null, "Não",
                    null, null, null);
            expense(g, 11, date, "Ração do gato", "Pets", "Vivência", 100, null, "Único", "Não", null, "Não", null, null,
                    null);

            Sheet u = wb.createSheet("Análise de Universidades");
            header(u, UNIVERSITY_HEADERS);
            university(u, 1, "Technische Universität München", 1, "Pública", "Technische Universität", "München",
                    "Informatik M.Sc.", "Não", "Sim", "uni-assist", "online", "Englisch", "B2 (TOEFL >= 88)",
                    "Wintersemester / Sommersemester", "01/Fevereiro - 01/Maio", "Baixa-Média", 97, 6000);
            university(u, 2, "Technische Universität München", 1, "Pública", "Technische Universität", "München",
                    "Software Engineering M. Sc.", "Não", "Sim", "Augsburg", "online", "Englisch", "B2 (TOEFL >= 88)",
                    "Wintersemester", "01/Fevereiro - 01/Maio", "Baixa", 97, 0);
            university(u, 3, "Universität des Saarlandes", 42, "Sim", "Universität", "Saarbrücken", "Computer Science M.Sc.",
                    "Se necessário (GRE)", "Não", "direta", "online", "Englisch", "C1 (TOEFL >= 95)", "Sommersemester",
                    "x - 15/Novembro", "Média", 400, null);
            u.createRow(4).createCell(12).setCellValue("https://www.exemplo.de/master");

            Sheet m = wb.createSheet("Mala");
            header(m, new String[] {"Mala", "Qnt.", "Item", "Categoria", "Nível de peso"});
            Row m1 = m.createRow(1);
            m1.createCell(0).setCellValue("Mala de Mão");
            m1.createCell(1).setCellValue(1);
            m1.createCell(2).setCellValue("Passaporte");
            m1.createCell(3).setCellValue("Documento");
            m1.createCell(4).setCellValue("Leve");
            Row m2 = m.createRow(2);
            m2.createCell(1).setCellValue(2);
            m2.createCell(2).setCellValue("Fotos Biométricas");

            Sheet e = wb.createSheet("Economia");
            e.createRow(0).createCell(0).setCellValue("Mês");

            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void header(Sheet s, String[] headers) {
        Row r = s.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            r.createCell(i).setCellValue(headers[i]);
        }
    }

    private static void expense(Sheet s, int row, CellStyle date, String name, String category, String cycle,
            Number price, String formula, String type, String required, LocalDate due, String status, LocalDate paidOn,
            Double paid, String obs) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(name);
        r.createCell(1).setCellValue(category);
        r.createCell(2).setCellValue(cycle);
        if (formula != null) {
            r.createCell(3).setCellFormula(formula);
        } else {
            r.createCell(3).setCellValue(price.doubleValue());
        }
        r.createCell(7).setCellValue(type);
        r.createCell(8).setCellValue(required);
        if (due != null) {
            r.createCell(9).setCellValue(due);
            r.getCell(9).setCellStyle(date);
        }
        r.createCell(11).setCellValue(status);
        if (paidOn != null) {
            r.createCell(12).setCellValue(paidOn);
            r.getCell(12).setCellStyle(date);
            r.createCell(13).setCellValue(paid);
        }
        if (obs != null) {
            r.createCell(14).setCellValue(obs);
        }
    }

    private static void university(Sheet s, int row, String name, int ranking, String type, String nature, String city,
            String program, String gre, String interview, String channel, String docs, String language,
            String proficiency, String start, String deadline, String chance, double fee, Integer nonEu) {
        Row r = s.createRow(row);
        r.createCell(0).setCellValue(name);
        r.createCell(1).setCellValue(ranking);
        r.createCell(2).setCellValue(type);
        r.createCell(3).setCellValue(nature);
        r.createCell(4).setCellValue(city);
        r.createCell(5).setCellValue(11.5755);
        r.createCell(6).setCellValue(48.1374);
        r.createCell(7).setCellValue("Bayern");
        r.createCell(8).setCellValue("Alemanha");
        r.createCell(9).setCellValue(1_500_000);
        r.createCell(10).setCellValue(1500);
        r.createCell(11).setCellValue("Excelente");
        r.createCell(12).setCellValue(program);
        r.createCell(13).setCellValue("Software, AI");
        r.createCell(14).setCellValue(gre);
        r.createCell(15).setCellValue("Não");
        r.createCell(16).setCellValue(interview);
        r.createCell(17).setCellValue("Não");
        r.createCell(18).setCellValue(channel);
        r.createCell(19).setCellValue(docs);
        r.createCell(20).setCellValue(language);
        r.createCell(21).setCellValue(proficiency);
        r.createCell(22).setCellValue(start);
        r.createCell(23).setCellValue(deadline);
        r.createCell(24).setCellValue(4);
        r.createCell(25).setCellValue(chance);
        r.createCell(26).setCellValue(fee);
        if (nonEu == null) {
            r.createCell(27).setCellValue("-");
        } else {
            r.createCell(27).setCellValue(nonEu);
        }
        r.createCell(28).setCellValue("Sim");
        r.createCell(29).setCellValue("Deutschlandstipendium");
        r.createCell(30).setCellValue("Baixa-Média");
    }
}
