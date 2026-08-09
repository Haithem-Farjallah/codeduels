package com.codeduels.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionReference {
    CREATE_PROBLEM("Can create new problems"),
    READ_PROBLEM("Can view published problems"),
    READ_DRAFT_PROBLEM("Can view draft problems"),
    WRITE_PROBLEM("Can edit existing problems"),
    DELETE_PROBLEM("Can delete problems");

    private final String description;

}
