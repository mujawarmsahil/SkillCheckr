package com.skillcheckr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String message;

    private String username;

    private String name;

    private String email;

    private String contact;

    private String profileImage;

    private String role;

    private int userId;

    private int roleId;

    private String token;
}