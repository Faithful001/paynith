package com.king.paysim.common.responses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Response<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private T data;

    private Response(boolean success, int statusCode, String message, T data) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(true, 200, null, data);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(true, 200, message, data);
    }

    public static <T> Response<T> success(int statusCode, String message, T data) {
        return new Response<>(true, statusCode, message, data);
    }

    public static Response<?> error(String message) {
        return new Response<>(false, 400, message, null);
    }

    public static Response<?> error(String message, int statusCode) {
        return new Response<>(false, statusCode, message, null);
    }
}
