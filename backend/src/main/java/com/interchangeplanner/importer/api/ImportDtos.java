package com.interchangeplanner.importer.api;

import com.interchangeplanner.importer.application.ImportModel.RowStatus;
import com.interchangeplanner.importer.application.ImportModel.Sheet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ImportDtos {

    private ImportDtos() {
    }

    public enum DuplicateStrategy {
        /** Mantém o que já existe. */
        SKIP,
        /** Atualiza com os dados da planilha. */
        UPDATE
    }

    public record PreviewRow(int row, RowStatus status, String label, List<String> messages) {
    }

    public record SheetPreview(Sheet sheet, String title, int valid, int warnings, int errors, int duplicates,
            List<PreviewRow> rows) {
    }

    /**
     * @param objectiveId    objetivo usado para marcar gastos duplicados na prévia (o principal)
     * @param ignoredSheets  abas que não são importadas (ex.: Economia, Dashboard)
     */
    public record ImportPreview(
            UUID id,
            String fileName,
            String status,
            Instant expiresAt,
            @Schema(types = {"string", "null"}, format = "uuid") UUID objectiveId,
            List<SheetPreview> sheets,
            List<String> ignoredSheets,
            @Schema(types = {"object", "null"}) CommitSummary summary) {
    }

    /**
     * @param sheets               abas a importar (padrão: todas)
     * @param objectiveId          objetivo que recebe os gastos (padrão: o principal)
     * @param sperrkontoAdjustment cria o ajuste "−Sperrkonto" no objetivo (padrão: sim)
     */
    public record CommitRequest(List<Sheet> sheets, UUID objectiveId, DuplicateStrategy duplicates,
            Boolean sperrkontoAdjustment) {
    }

    public record SheetSummary(Sheet sheet, int created, int updated, int skipped) {
    }

    /** @param notes o que mais foi criado (categorias, universidades, malas, ajuste) */
    public record CommitSummary(List<SheetSummary> sheets, List<String> notes) {
    }
}
