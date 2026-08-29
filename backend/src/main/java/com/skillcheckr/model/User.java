package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @JsonProperty("user_id")
    @JsonAlias({"user_id", "userId", "id"})
    private int userId;

    private String username;

    private String password;

    @JsonProperty("user_role")
    @JsonAlias({"user_role", "role", "userRole"})
    private String role;

    @JsonProperty("profile_image")
    @JsonAlias({"profile_image", "profileImage"})
    private String profileImage;

    @JsonProperty("auth_provider")
    @JsonAlias({"auth_provider", "authProvider"})
    private String authProvider;

    @JsonProperty("provider_id")
    @JsonAlias({"provider_id", "providerId"})
    private String providerId;

    @JsonProperty("status")
    @JsonAlias({"status", "is_active", "isActive"})
    private String status;
}
