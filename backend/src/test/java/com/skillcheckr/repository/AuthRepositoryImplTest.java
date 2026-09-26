package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthRepositoryImpl repository;

    @Test
    @SuppressWarnings("unchecked")
    void login_returnsUser_whenBcryptPasswordMatches() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("john");
        user.setPassword("$2a$10$hashed");
        user.setRole("Student");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE username = ?"), any(RowMapper.class), eq("john")))
                .thenReturn(List.of(user));
        when(passwordEncoder.matches("secret", "$2a$10$hashed")).thenReturn(true);

        Optional<User> loggedIn = repository.login("john", "secret");

        assertThat(loggedIn).contains(user);
        assertThat(loggedIn.get().getUsername()).isEqualTo("john");
    }

    @Test
    @SuppressWarnings("unchecked")
    void login_returnsUser_andUpgradesPlaintextPassword_whenMatching() {
        User user = new User();
        user.setUserId(2);
        user.setUsername("plainUser");
        user.setPassword("plainPass");
        user.setRole("Teacher");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE username = ?"), any(RowMapper.class), eq("plainUser")))
                .thenReturn(List.of(user));
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$newHash");

        Optional<User> loggedIn = repository.login("plainUser", "plainPass");

        assertThat(loggedIn).contains(user);
        verify(jdbcTemplate).update("UPDATE user SET password = ? WHERE user_id = ?", "$2a$10$newHash", 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void login_returnsEmpty_whenUserNotFoundOrPasswordMismatches() {
        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE username = ?"), any(RowMapper.class), eq("ghost")))
                .thenReturn(List.of());

        assertThat(repository.login("ghost", "pass")).isEmpty();
    }

    @Test
    void getRoleIds_returnExpectedIds() {
        when(jdbcTemplate.queryForObject(eq("SELECT student_id FROM student WHERE user_id = ?"), eq(Integer.class), eq(1))).thenReturn(10);
        when(jdbcTemplate.queryForObject(eq("SELECT teacher_id FROM teacher WHERE user_id = ?"), eq(Integer.class), eq(2))).thenReturn(20);
        when(jdbcTemplate.queryForObject(eq("SELECT admin_id FROM admin WHERE user_id = ?"), eq(Integer.class), eq(3))).thenReturn(30);

        assertThat(repository.getStudentIdByUserId(1)).isEqualTo(10);
        assertThat(repository.getTeacherIdByUserId(2)).isEqualTo(20);
        assertThat(repository.getAdminIdByUserId(3)).isEqualTo(30);
    }

    @Test
    void getRoleIds_returnZero_onEmptyResult() {
        when(jdbcTemplate.queryForObject(eq("SELECT student_id FROM student WHERE user_id = ?"), eq(Integer.class), eq(99)))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForObject(eq("SELECT teacher_id FROM teacher WHERE user_id = ?"), eq(Integer.class), eq(99)))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForObject(eq("SELECT admin_id FROM admin WHERE user_id = ?"), eq(Integer.class), eq(99)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.getStudentIdByUserId(99)).isEqualTo(0);
        assertThat(repository.getTeacherIdByUserId(99)).isEqualTo(0);
        assertThat(repository.getAdminIdByUserId(99)).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserProfile_returnsProfile_forStudent() {
        User user = new User();
        user.setUserId(5);
        user.setUsername("student5");
        user.setRole("Student");
        user.setProfileImage("avatar.png");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE user_id = ?"), any(RowMapper.class), eq(5)))
                .thenReturn(List.of(user));

        Optional<UserProfileDTO> profile = repository.getUserProfile(5);

        assertThat(profile).isPresent();
        assertThat(profile.get().getUserId()).isEqualTo(5);
        assertThat(profile.get().getUsername()).isEqualTo("student5");
        assertThat(profile.get().getRole()).isEqualTo("Student");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserProfile_returnsEmpty_whenUserNotFound() {
        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE user_id = ?"), any(RowMapper.class), eq(999)))
                .thenReturn(List.of());

        assertThat(repository.getUserProfile(999)).isEmpty();
    }

    @Test
    void isUsernameInUse_returnsTrue_whenTaken() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("takenUser"), eq(1))).thenReturn(1);

        assertThat(repository.isUsernameInUse("takenUser", 1)).isTrue();
    }

    @Test
    void isUsernameInUse_returnsFalse_whenFree() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("freeUser"), eq(1))).thenReturn(0);

        assertThat(repository.isUsernameInUse("freeUser", 1)).isFalse();
    }

    @Test
    void existsByUsername_returnsTrue_whenTaken() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "takenUser"))
                .thenReturn(1);

        assertThat(repository.existsByUsername("takenUser")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenFree() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "freeUser"))
                .thenReturn(0);

        assertThat(repository.existsByUsername("freeUser")).isFalse();
    }

    @Test
    void isEmailInUse_returnsTrue_whenFoundInStudent() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM student WHERE email = ? AND (user_id != ? OR user_id IS NULL)"), eq(Integer.class), eq("test@test.com"), eq(1)))
                .thenReturn(1);

        assertThat(repository.isEmailInUse("test@test.com", 1)).isTrue();
    }

    @Test
    void isEmailInUse_returnsFalse_whenEmptyOrFree() {
        assertThat(repository.isEmailInUse("", 1)).isFalse();
        assertThat(repository.isEmailInUse(null, 1)).isFalse();
    }

    @Test
    void verifyCurrentPassword_returnsTrue_whenMatches() {
        when(jdbcTemplate.queryForObject(eq("SELECT password FROM user WHERE user_id = ?"), eq(String.class), eq(1)))
                .thenReturn("$2a$10$hashedPass");
        when(passwordEncoder.matches("secret", "$2a$10$hashedPass")).thenReturn(true);

        assertThat(repository.verifyCurrentPassword(1, "secret")).isTrue();
    }

    @Test
    void verifyCurrentPassword_returnsFalse_whenMismatchesOrNull() {
        when(jdbcTemplate.queryForObject(eq("SELECT password FROM user WHERE user_id = ?"), eq(String.class), eq(1)))
                .thenReturn("$2a$10$hashedPass");
        when(passwordEncoder.matches("wrong", "$2a$10$hashedPass")).thenReturn(false);

        assertThat(repository.verifyCurrentPassword(1, "wrong")).isFalse();
        assertThat(repository.verifyCurrentPassword(1, null)).isFalse();
        assertThat(repository.verifyCurrentPassword(0, "secret")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateUserProfile_updatesUserAndStudentTable() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("updatedUser")
                .name("Updated Name")
                .email("updated@test.com")
                .contact("999")
                .password("newPass")
                .build();

        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$encodedNewPass");
        when(jdbcTemplate.queryForObject(eq("SELECT user_role FROM user WHERE user_id = ?"), eq(String.class), eq(1)))
                .thenReturn("Student");

        User user = new User();
        user.setUserId(1);
        user.setUsername("updatedUser");
        user.setRole("Student");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE user_id = ?"), any(RowMapper.class), eq(1)))
                .thenReturn(List.of(user));

        Optional<UserProfileDTO> result = repository.updateUserProfile(profile);

        assertThat(result).isPresent();
        verify(jdbcTemplate).update(eq("UPDATE user SET username = ?, password = ?, profile_image = ? WHERE user_id = ?"),
                eq("updatedUser"), eq("$2a$10$encodedNewPass"), any(), eq(1));
    }
}
