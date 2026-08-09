package com.codeduels.common.security;

public final class Authz {

    public static final String PROBLEM_CREATE="hasAuthority('CREATE_PROBLEM')";
    public static final String PROBLEM_READ="hasAuthority('READ_PROBLEM')";
    public static final String PROBLEM_UPDATE="hasAuthority('UPDATE_PROBLEM')";
    public static final String PROBLEM_DELETE="hasAuthority('DELETE_PROBLEM')";


}
