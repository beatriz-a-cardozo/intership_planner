package com.interchangeplanner.university.api;

import com.interchangeplanner.shared.security.CurrentUser;
import com.interchangeplanner.university.api.UniversityDtos.ImageCaptionRequest;
import com.interchangeplanner.university.api.UniversityDtos.UniversityImageResponse;
import com.interchangeplanner.university.application.UniversityImageService;
import com.interchangeplanner.university.application.UniversityImageService.ImageContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/universities/{universityId}/images")
@Tag(name = "Universidades e programas")
public class UniversityImageController {

    private final UniversityImageService service;

    public UniversityImageController(UniversityImageService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as imagens da universidade (metadados; a capa vem marcada)")
    public List<UniversityImageResponse> list(CurrentUser user, @PathVariable UUID universityId) {
        return service.list(user.id(), universityId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma imagem (JPEG, PNG ou WebP, até 5 MB; a primeira vira a capa)")
    public UniversityImageResponse upload(CurrentUser user, @PathVariable UUID universityId,
            @RequestPart("file") MultipartFile file, @RequestParam(required = false) String caption) {
        return service.upload(user.id(), universityId, file, caption);
    }

    @GetMapping("/{imageId}/content")
    @Operation(summary = "Conteúdo da imagem (imutável: o id muda se a imagem mudar)")
    public ResponseEntity<byte[]> content(CurrentUser user, @PathVariable UUID universityId, @PathVariable UUID imageId) {
        ImageContent c = service.content(user.id(), universityId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(c.data());
    }

    @PatchMapping("/{imageId}/cover")
    @Operation(summary = "Define a imagem de capa")
    public List<UniversityImageResponse> setCover(CurrentUser user, @PathVariable UUID universityId,
            @PathVariable UUID imageId) {
        return service.setCover(user.id(), universityId, imageId);
    }

    @PatchMapping("/{imageId}")
    @Operation(summary = "Edita a legenda")
    public UniversityImageResponse caption(CurrentUser user, @PathVariable UUID universityId, @PathVariable UUID imageId,
            @Valid @RequestBody ImageCaptionRequest r) {
        return service.updateCaption(user.id(), universityId, imageId, r.caption());
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Exclui a imagem (a capa passa para a mais antiga)")
    public ResponseEntity<Void> delete(CurrentUser user, @PathVariable UUID universityId, @PathVariable UUID imageId) {
        service.delete(user.id(), universityId, imageId);
        return ResponseEntity.noContent().build();
    }
}
