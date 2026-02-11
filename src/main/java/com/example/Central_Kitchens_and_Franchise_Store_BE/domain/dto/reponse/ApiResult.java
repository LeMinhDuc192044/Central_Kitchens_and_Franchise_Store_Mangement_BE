package com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    int statusCode;
    String message;
    T data;

    @Builder.Default
    LocalDateTime responseAt = LocalDateTime.now();

    public static <T> ApiResult<T> success(String message, T data) {
        return ApiResult.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResult<T> success(T data) {
        return success("Success", data);
    }

    public static ApiResult<Void> success(String message) {
        return success(message, null);
    }

    public static <T> ApiResult<T> error(int statusCode, String message) {
        return ApiResult.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .build();
    }
}

