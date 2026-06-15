package com.senctraiq.Authentication;
import lombok.AllArgsConstructor;
import lombok.Data;


    @Data
    @AllArgsConstructor
    public class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String username;
        private String firstname;
        private String email;
        private String lastname;
        private Boolean loggedIn;
        private Boolean isOnShift;
        private Boolean isOnLeave;
    }
