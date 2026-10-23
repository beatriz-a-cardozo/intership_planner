package com.interchangeplanner.applications.domain;

/**
 * Status da candidatura, na ordem do processo. O resultado é DERIVADO do status (evita estados
 * contraditórios como "aprovado" com "resultado: rejeitado").
 */
public enum ApplicationStatus {
    /** Aguardando abertura das inscrições (status inicial ao marcar o programa como "Escolhida"). */
    AWAITING_OPENING,
    DOCUMENTS_UNDER_REVIEW,
    EXAM_REQUESTED,
    INTERVIEW_REQUESTED,
    /** Há solicitações pendentes da universidade (ver application_request). */
    ITEM_REQUESTED,
    WAITLISTED,
    AWAITING_RESULT,
    ACCEPTED,
    REJECTED,
    ENROLLED;

    public enum Result {
        PENDING,
        ACCEPTED,
        REJECTED,
        WAITLISTED
    }

    public Result result() {
        return switch (this) {
            case ACCEPTED, ENROLLED -> Result.ACCEPTED;
            case REJECTED -> Result.REJECTED;
            case WAITLISTED -> Result.WAITLISTED;
            default -> Result.PENDING;
        };
    }

    /** Processo encerrado (não recebe mais solicitações automáticas). */
    public boolean isFinal() {
        return this == ACCEPTED || this == REJECTED || this == ENROLLED;
    }
}
