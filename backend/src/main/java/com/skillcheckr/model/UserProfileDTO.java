package com.skillcheckr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {

    @JsonProperty("user_id")
    private int userId;

    private String username;

    private String name;

    private String email;

    private String contact;

    private String password;

    private String role;

    @JsonProperty("role_id")
    private int roleId;

    @JsonProperty("profile_image")
    private String profileImage;
}
