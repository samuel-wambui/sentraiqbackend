package com.senctraiq.Authentication;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
    private Boolean passwordChangeRequired = false;
    private String passwordChangeToken;

    public LoginResponse(String accessToken,
                         String refreshToken,
                         String username,
                         String firstname,
                         String email,
                         String lastname,
                         Boolean loggedIn,
                         Boolean isOnShift,
                         Boolean isOnLeave) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.firstname = firstname;
        this.email = email;
        this.lastname = lastname;
        this.loggedIn = loggedIn;
        this.isOnShift = isOnShift;
        this.isOnLeave = isOnLeave;
        this.passwordChangeRequired = false;
    }

    public static LoginResponse passwordChangeRequired(String username,
                                                       String firstname,
                                                       String email,
                                                       String lastname,
                                                       String passwordChangeToken) {
        LoginResponse response = new LoginResponse();
        response.setUsername(username);
        response.setFirstname(firstname);
        response.setEmail(email);
        response.setLastname(lastname);
        response.setLoggedIn(false);
        response.setPasswordChangeRequired(true);
        response.setPasswordChangeToken(passwordChangeToken);
        return response;
    }
}
