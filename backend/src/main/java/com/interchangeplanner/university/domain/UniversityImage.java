package com.interchangeplanner.university.domain;

import com.interchangeplanner.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Metadados de uma imagem da universidade. Os bytes ficam em {@code university_image_content} e só são lidos
 * ao servir a imagem (listar nunca carrega o conteúdo).
 */
@Entity
@Table(name = "university_image")
public class UniversityImage extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "university_id", nullable = false, updatable = false)
    private UUID universityId;

    @Column(name = "content_type", nullable = false, updatable = false, length = 20)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private int sizeBytes;

    @Column(name = "caption", length = 200)
    private String caption;

    protected UniversityImage() {
    }

    public UniversityImage(UUID userId, UUID universityId, String contentType, int sizeBytes, String caption) {
        this.userId = userId;
        this.universityId = universityId;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        setCaption(caption);
    }

    public void setCaption(String caption) {
        this.caption = caption == null || caption.isBlank() ? null : caption.strip();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getUniversityId() {
        return universityId;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public String getCaption() {
        return caption;
    }
}
