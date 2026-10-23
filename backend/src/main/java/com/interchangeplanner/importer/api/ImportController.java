package com.interchangeplanner.importer.api;

import com.interchangeplanner.importer.api.ImportDtos.CommitRequest;
import com.interchangeplanner.importer.api.ImportDtos.ImportPreview;
import com.interchangeplanner.importer.application.ImportService;
import com.interchangeplanner.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@Tag(name = "Importação")
public class ImportController {

    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia a planilha (.xlsx, até 5 MB) e devolve a prévia linha a linha (nada é gravado ainda)")
    public ImportPreview preview(CurrentUser user, @RequestPart("file") MultipartFile file) {
        return service.preview(user.id(), file);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Prévia (ou resumo, se já importada)")
    public ImportPreview get(CurrentUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PostMapping("/{id}/commit")
    @Operation(summary = "Importa (abas escolhidas, objetivo de destino, duplicados: pular ou atualizar)")
    public ImportPreview commit(CurrentUser user, @PathVariable UUID id, @RequestBody(required = false) CommitRequest r) {
        return service.commit(user.id(), id, r);
    }
}
