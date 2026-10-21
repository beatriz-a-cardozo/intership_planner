package com.interchangeplanner.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Moeda suportada (catálogo global, somente leitura pela API). Adicionar moeda = 1 insert via migration. */
@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "scale", nullable = false)
    private short scale;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected Currency() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public short getScale() {
        return scale;
    }

    public short getSortOrder() {
        return sortOrder;
    }
}
