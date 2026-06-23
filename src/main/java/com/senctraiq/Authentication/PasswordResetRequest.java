package com.senctraiq.Authentication;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String identifier;
    private String username;
    private String email;
}
