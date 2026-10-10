package com.king.paynith.shared.util;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    public static <T> T deserialize(String value, Class<T> type) {
        return objectMapper.readValue(value, type);
    }
}
