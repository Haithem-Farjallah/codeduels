package com.codeduels.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.codeduels.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
public class Account extends BaseEntity  {

    @Column(unique = true, nullable = false)
    private String email;

    private boolean emailVerified=false;
    private String emailVerificationToken;
    private LocalDateTime emailVerificationTokenExpires;

    @JsonIgnore
    private String password;

    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpires;

    private String RefreshToken;

    @ManyToMany
    @JoinTable(
            name = "account_role",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles=new HashSet<>();

}
