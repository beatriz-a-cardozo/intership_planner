package com.interchangeplanner.importer.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lê as abas da planilha em linhas genéricas (coluna pelo cabeçalho normalizado). Fórmulas viram o valor calculado
 * mais o texto da fórmula. Proteções: só .xlsx (zip), proporção mínima de compressão contra zip-bomb e tamanho
 * máximo de texto por entrada.
 */
public final class SheetReader {

    /**
     * Valor de uma célula.
     *
     * @param number valor numérico (também para datas, como número de série do Excel)
     * @param date   preenchido quando a célula está formatada como data
     * @param formula texto da fórmula, sem "=" (nulo se for valor digitado)
     */
    public record Value(String text, BigDecimal number, LocalDate date, String formula) {

        public boolean isEmpty() {
            return ImportText.blank(text) && number == null;
        }

        /** Número de série do Excel como data (para colunas de data sem formatação). */
        public LocalDate asDate() {
            if (date != null) {
                return date;
            }
            return number == null ? null : DateUtil.getLocalDateTime(number.doubleValue()).toLocalDate();
        }
    }

    /** Linha da aba; {@code number} é o número da linha no Excel (cabeçalho = 1). */
    public record RawRow(int number, Map<String, Value> cells) {

        public Value get(String header) {
            return cells.getOrDefault(ImportText.norm(header), new Value(null, null, null, null));
        }

        public String text(String header) {
            Value v = get(header);
            return ImportText.blank(v.text()) ? null : v.text().strip();
        }

        public BigDecimal number(String header) {
            return get(header).number();
        }
    }

    public record Workbook(Map<String, List<RawRow>> sheets) {

        /** Aba pelo nome normalizado ("informacoes dos gastos"); lista vazia se não existir. */
        public List<RawRow> sheet(String name) {
            return sheets.getOrDefault(ImportText.norm(name), List.of());
        }
    }

    private SheetReader() {
    }

    public static Workbook read(byte[] bytes) throws IOException {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4) {
            throw new IOException("Não é um arquivo .xlsx");
        }
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxTextSize(10_000_000);
        Map<String, List<RawRow>> sheets = new LinkedHashMap<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            for (Sheet sheet : wb) {
                sheets.put(ImportText.norm(sheet.getSheetName()), rows(sheet));
            }
        }
        return new Workbook(sheets);
    }

    private static List<RawRow> rows(Sheet sheet) {
        List<RawRow> result = new ArrayList<>();
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) {
            return result;
        }
        Map<Integer, String> columns = new HashMap<>();
        for (Cell c : header) {
            String name = c.getCellType() == CellType.STRING ? c.getStringCellValue() : "";
            columns.put(c.getColumnIndex(), ImportText.blank(name)
                    ? "#" + CellReference.convertNumToColString(c.getColumnIndex()).toLowerCase()
                    : ImportText.norm(name));
        }
        for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, Value> cells = new HashMap<>();
            for (Cell c : row) {
                String col = columns.getOrDefault(c.getColumnIndex(),
                        "#" + CellReference.convertNumToColString(c.getColumnIndex()).toLowerCase());
                Value v = value(c);
                if (!v.isEmpty()) {
                    cells.put(col, v);
                }
            }
            if (!cells.isEmpty()) {
                result.add(new RawRow(r + 1, cells));
            }
        }
        return result;
    }

    private static Value value(Cell c) {
        String formula = c.getCellType() == CellType.FORMULA ? c.getCellFormula() : null;
        CellType type = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
        return switch (type) {
            case STRING -> new Value(c.getStringCellValue(), null, null, formula);
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                BigDecimal n = BigDecimal.valueOf(d);
                LocalDate date = DateUtil.isCellDateFormatted(c) ? c.getLocalDateTimeCellValue().toLocalDate() : null;
                yield new Value(n.stripTrailingZeros().toPlainString(), n, date, formula);
            }
            case BOOLEAN -> new Value(c.getBooleanCellValue() ? "Sim" : "Não", null, null, formula);
            default -> new Value(null, null, null, formula);
        };
    }
}
