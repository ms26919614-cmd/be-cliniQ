package com.cliniq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String type;
    private String username;
    private String fullName;
    private String role;

    public LoginResponse(String token, String username, String fullName, String role) {
        this.token = token;
        this.type = "Bearer";
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }
}
