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
public class UserProfileDTO {

    @JsonProperty("user_id")
    private int userId;

    private String username;

    @JsonProperty("name")
    @JsonAlias({"name", "student_name", "teacher_name", "admin_name", "studentName", "teacherName", "adminName"})
    private String name;

    @JsonProperty("email")
    @JsonAlias({"email", "student_email", "teacher_email", "admin_email", "studentEmail", "teacherEmail", "adminEmail", "mail"})
    private String email;

    @JsonProperty("contact")
    @JsonAlias({"contact", "student_contact", "teacher_contact", "admin_contact", "studentContact", "teacherContact", "adminContact", "phone"})
    private String contact;

    @JsonProperty("password")
    @JsonAlias({"password", "newPassword", "new_password"})
    private String password;

    @JsonProperty("current_password")
    @JsonAlias({"currentPassword", "current_password", "oldPassword", "old_password"})
    private String currentPassword;

    public String getOldPassword() {
        return currentPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.currentPassword = oldPassword;
    }

    private String role;

    @JsonProperty("role_id")
    private int roleId;

    @JsonProperty("profile_image")
    private String profileImage;
}
