package com.king.paysim.common.interceptor;

import com.king.paysim.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@RestControllerAdvice
public class ResponseInterceptor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return !ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType contentType,
            Class converterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        HttpServletRequest req =
                ((ServletServerHttpRequest) request).getServletRequest();

        var servletResponse =
                ((ServletServerHttpResponse) response).getServletResponse();

        int currentStatus = servletResponse.getStatus();

        Integer overrideStatus = null;
        Object data = null;
        String message = null;

        if (body instanceof Response<?> api) {
            overrideStatus = api.getStatusCode();
            data = api.getData();
            message = api.getMessage();
        } else {
            data = body;
        }

        // 1️⃣ Status code rule
        int finalStatus = currentStatus;
        if (overrideStatus != null && overrideStatus >= 400) {
            finalStatus = overrideStatus;
            servletResponse.setStatus(finalStatus);
        }

        // 2️⃣ Data rule
        Object finalData = data != null ? data : null;

        // 3️⃣ Message rule
        String finalMessage =
                message != null
                        ? message
                        : resolveDefaultMessage(finalStatus, req.getMethod());

        return Map.of(
                "statusCode", finalStatus,
                "message", finalMessage,
                "data", finalData
        );
    }

    private String resolveDefaultMessage(int status, String method) {
        if (method.equals("POST") && status == 201) {
            return "Resource created successfully";
        }
        if (status >= 200 && status < 300) {
            return "Request successful";
        }
        return "Request failed";
    }
}
