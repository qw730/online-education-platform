package com.edu.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Course category such as Programming, Design or Language.
 */
@Getter
@Setter
@Entity
@Table(name = "category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_code", columnNames = "code")
})
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
