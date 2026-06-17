package com.learnhub.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}
