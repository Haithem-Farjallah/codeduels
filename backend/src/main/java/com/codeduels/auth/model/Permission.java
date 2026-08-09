package com.codeduels.auth.model;

import com.codeduels.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(nullable = false,unique = true, name = "reference")
    @Enumerated(EnumType.STRING)
    private PermissionReference PermissionReference;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String Description;

}
