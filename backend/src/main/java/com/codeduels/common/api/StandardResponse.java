package com.codeduels.common.api;
import java.time.Instant;

public record StandardResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp
) {

    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<T>(true,data,null,Instant.now());
    }

    public static StandardResponse<Void> error( String message) {
        return new StandardResponse<>(false,null,message,Instant.now());
    }
}