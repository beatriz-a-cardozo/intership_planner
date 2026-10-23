package com.interchangeplanner.university.domain;

import com.interchangeplanner.shared.domain.UuidV7;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Intake de um programa: janela de inscrição recorrente (dia/mês) e mês de início das aulas. */
@Entity
@Table(name = "program_intake")
public class ProgramIntake {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.generate();

    @Enumerated(EnumType.STRING)
    @Column(name = "term", nullable = false, length = 10)
    private Term term;

    @Column(name = "open_month")
    private Short openMonth;

    @Column(name = "open_day")
    private Short openDay;

    @Column(name = "close_month")
    private Short closeMonth;

    @Column(name = "close_day")
    private Short closeDay;

    @Column(name = "start_month", nullable = false)
    private short startMonth;

    protected ProgramIntake() {
    }

    public ProgramIntake(Term term, Integer openMonth, Integer openDay, Integer closeMonth, Integer closeDay,
            int startMonth) {
        this.term = term;
        this.openMonth = toShort(openMonth);
        this.openDay = toShort(openDay);
        this.closeMonth = toShort(closeMonth);
        this.closeDay = toShort(closeDay);
        this.startMonth = (short) startMonth;
    }

    private static Short toShort(Integer v) {
        return v == null ? null : v.shortValue();
    }

    private static Integer toInt(Short v) {
        return v == null ? null : v.intValue();
    }

    public UUID getId() {
        return id;
    }

    public Term getTerm() {
        return term;
    }

    public Integer getOpenMonth() {
        return toInt(openMonth);
    }

    public Integer getOpenDay() {
        return toInt(openDay);
    }

    public Integer getCloseMonth() {
        return toInt(closeMonth);
    }

    public Integer getCloseDay() {
        return toInt(closeDay);
    }

    public int getStartMonth() {
        return startMonth;
    }
}
