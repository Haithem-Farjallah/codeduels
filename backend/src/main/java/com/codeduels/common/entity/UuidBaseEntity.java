package com.codeduels.common.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class UuidBaseEntity extends  AuditableEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
