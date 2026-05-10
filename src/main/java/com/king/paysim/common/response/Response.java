package com.king.paysim.common.response;

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

    public static <T> Response<T> success(String message, T data) {
        Response<T> response = new Response<>();
        response.success = true;
        response.statusCode = 200;
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> Response<T> error(String message, int statusCode) {
        Response<T> response = new Response<>();
        response.success = false;
        response.statusCode = statusCode;
        response.message = message;
        response.data = null;
        return response;
    }

}
