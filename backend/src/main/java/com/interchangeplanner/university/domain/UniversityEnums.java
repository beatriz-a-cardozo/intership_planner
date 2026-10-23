package com.interchangeplanner.university.domain;

/**
 * Enums do módulo de universidades. Escalas ordinais da planilha ("Baixa", "Baixa-Média", "Média",
 * "Média-Alta", "Alta", "Excelente") viram enums com ordem: comparar e filtrar fica trivial.
 */
public final class UniversityEnums {

    private UniversityEnums() {
    }

    /** Chance de admissão / de bolsa (5 níveis, em ordem crescente). */
    public enum Rating {
        LOW,
        LOW_MEDIUM,
        MEDIUM,
        MEDIUM_HIGH,
        HIGH
    }

    /** Empregabilidade: a planilha tem um nível acima de "Alta". */
    public enum Employability {
        LOW,
        LOW_MEDIUM,
        MEDIUM,
        MEDIUM_HIGH,
        HIGH,
        EXCELLENT
    }

    /** Requisito tri-estado ("Se necessário (GRE)" = CONDITIONAL) + desconhecido. */
    public enum Requirement {
        REQUIRED,
        NOT_REQUIRED,
        CONDITIONAL,
        UNKNOWN
    }

    public enum InstitutionType {
        PUBLIC,
        PRIVATE
    }

    /** Universität, Technische Universität, Hochschule (universidade de ciências aplicadas). */
    public enum Nature {
        UNIVERSITY,
        TU,
        HOCHSCHULE,
        OTHER
    }

    /** Canal de inscrição: direto, uni-assist, VPD ou portal próprio (C@MPUS, unisono…, ver portalName). */
    public enum ApplicationChannel {
        DIRECT,
        UNI_ASSIST,
        VPD,
        PORTAL
    }

    public enum DocumentSubmission {
        ONLINE,
        PHYSICAL,
        BOTH
    }

    public enum TuitionPeriod {
        SEMESTER,
        YEAR,
        TOTAL
    }

    /** Wintersemester (início em outubro) e Sommersemester (início em abril). */
    public enum Term {
        WINTER,
        SUMMER,
        OTHER
    }

    public enum Cefr {
        A1,
        A2,
        B1,
        B2,
        C1,
        C2
    }
}
