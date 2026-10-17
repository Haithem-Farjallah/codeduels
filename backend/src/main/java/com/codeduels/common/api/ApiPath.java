package com.codeduels.common.api;

public final class ApiPath {
    private ApiPath() {}

    //BASE paths
    public static final String API_BASE= "/api/v1";
    public static final String AUTH_BASE= API_BASE + "/auth";
    public static final String PROBLEMS = API_BASE + "/problems";
    public static final String SUBMISSIONS = API_BASE + "/submissions";
    public static final String MATCH = API_BASE + "/match";
    public static final String DUELS = API_BASE + "/duels";
    public static final String USERS = API_BASE + "/users";

    //Path variables
    public static final String PATH_ID     = "/{id}";
    public static final String PATH_STATUS = "/{status}";
    public static final String PATH_SLUG = "/{slug}";

    //Full Combined Paths
    public static final String Register = "/register";
    public static final String Login =  "/login";
    public static final String Refresh = "/refresh";
}
