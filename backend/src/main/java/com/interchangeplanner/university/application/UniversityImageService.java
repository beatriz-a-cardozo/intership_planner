package com.interchangeplanner.university.application;

import com.interchangeplanner.shared.error.BusinessRuleException;
import com.interchangeplanner.shared.error.NotFoundException;
import com.interchangeplanner.university.api.UniversityDtos.UniversityImageResponse;
import com.interchangeplanner.university.domain.University;
import com.interchangeplanner.university.domain.UniversityImage;
import com.interchangeplanner.university.domain.UniversityImageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imagens das universidades (galeria + capa dos cards). O tipo é detectado pelos primeiros bytes do arquivo,
 * nunca pelo nome ou pelo Content-Type enviado; só JPEG, PNG e WebP de até 5 MB, no máximo 12 por universidade.
 * A primeira imagem vira a capa; excluir a capa promove a mais antiga das restantes.
 */
@Service
public class UniversityImageService {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_PER_UNIVERSITY = 12;

    public record ImageContent(String contentType, byte[] data) {
    }

    private final UniversityService universities;
    private final UniversityImageRepository images;
    private final JdbcClient jdbc;

    public UniversityImageService(UniversityService universities, UniversityImageRepository images, JdbcClient jdbc) {
        this.universities = universities;
        this.images = images;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<UniversityImageResponse> list(UUID userId, UUID universityId) {
        University u = universities.require(userId, universityId);
        return images.findByUserIdAndUniversityIdOrderByCreatedAtAsc(userId, universityId).stream()
                .map(i -> toResponse(i, u))
                .toList();
    }

    @Transactional
    public UniversityImageResponse upload(UUID userId, UUID universityId, MultipartFile file, String caption) {
        University u = universities.require(userId, universityId);
        if (images.countByUserIdAndUniversityId(userId, universityId) >= MAX_PER_UNIVERSITY) {
            throw new BusinessRuleException("Limite de " + MAX_PER_UNIVERSITY + " imagens por universidade.", "file");
        }
        byte[] data = read(file);
        if (data.length == 0) {
            throw new BusinessRuleException("O arquivo está vazio.", "file");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessRuleException("A imagem deve ter até 5 MB.", "file");
        }
        String type = detectType(data);
        if (type == null) {
            throw new BusinessRuleException("Envie uma imagem JPEG, PNG ou WebP.", "file");
        }
        if (caption != null && caption.length() > 200) {
            throw new BusinessRuleException("Legenda de até 200 caracteres.", "caption");
        }
        UniversityImage image = images.saveAndFlush(new UniversityImage(userId, universityId, type, data.length, caption));
        jdbc.sql("INSERT INTO university_image_content (image_id, data) VALUES (?, ?)")
                .params(image.getId(), data)
                .update();
        if (u.getCoverImageId() == null) {
            u.setCoverImageId(image.getId());
        }
        return toResponse(image, u);
    }

    @Transactional(readOnly = true)
    public ImageContent content(UUID userId, UUID universityId, UUID imageId) {
        UniversityImage image = require(userId, universityId, imageId);
        byte[] data = jdbc.sql("SELECT data FROM university_image_content WHERE image_id = ?")
                .param(image.getId())
                .query(byte[].class)
                .optional()
                .orElseThrow(() -> new NotFoundException("Imagem"));
        return new ImageContent(image.getContentType(), data);
    }

    @Transactional
    public List<UniversityImageResponse> setCover(UUID userId, UUID universityId, UUID imageId) {
        University u = universities.require(userId, universityId);
        u.setCoverImageId(require(userId, universityId, imageId).getId());
        return list(userId, universityId);
    }

    @Transactional
    public UniversityImageResponse updateCaption(UUID userId, UUID universityId, UUID imageId, String caption) {
        University u = universities.require(userId, universityId);
        UniversityImage image = require(userId, universityId, imageId);
        image.setCaption(caption);
        return toResponse(image, u);
    }

    @Transactional
    public void delete(UUID userId, UUID universityId, UUID imageId) {
        University u = universities.require(userId, universityId);
        UniversityImage image = require(userId, universityId, imageId);
        boolean wasCover = image.getId().equals(u.getCoverImageId());
        if (wasCover) {
            u.setCoverImageId(null);
        }
        images.delete(image);
        images.flush();
        if (wasCover) {
            images.findByUserIdAndUniversityIdOrderByCreatedAtAsc(userId, universityId).stream()
                    .findFirst()
                    .ifPresent(next -> u.setCoverImageId(next.getId()));
        }
    }

    /** Tipo pelo "número mágico" do arquivo; nulo se não for JPEG, PNG nem WebP. */
    static String detectType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (d.length >= 8 && Arrays.equals(Arrays.copyOf(d, 8), png)) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static byte[] read(MultipartFile file) {
        if (file == null) {
            throw new BusinessRuleException("Escolha uma imagem.", "file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("A imagem deve ter até 5 MB.", "file");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UniversityImage require(UUID userId, UUID universityId, UUID imageId) {
        return images.findByIdAndUserIdAndUniversityId(imageId, userId, universityId)
                .orElseThrow(() -> new NotFoundException("Imagem"));
    }

    private static UniversityImageResponse toResponse(UniversityImage i, University u) {
        return new UniversityImageResponse(i.getId(), i.getContentType(), i.getSizeBytes(), i.getCaption(),
                i.getId().equals(u.getCoverImageId()), i.getCreatedAt());
    }
}
