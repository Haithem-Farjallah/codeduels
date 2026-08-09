package com.codeduels.user.model;

import com.codeduels.common.entity.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends UuidBaseEntity {

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String avatarUrl;
}
