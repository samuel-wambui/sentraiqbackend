package com.senctraiq.ApiResponse;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginApiResponse <T> {
    private String message;
    private Integer statusCode;

    private T token;
    private T refreshToken;
}
