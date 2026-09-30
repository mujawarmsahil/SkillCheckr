package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AuthRepositoryImpl repository;

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_returnsUser_whenFound() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("john");
        user.setPassword("$2a$10$hashed");
        user.setRole("Student");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE LOWER(username) = LOWER(?)"), any(RowMapper.class), eq("john")))
                .thenReturn(List.of(user));

        Optional<User> found = repository.findByUsername("john");

        assertThat(found).contains(user);
        assertThat(found.get().getUsername()).isEqualTo("john");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_returnsEmpty_whenNotFound() {
        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE LOWER(username) = LOWER(?)"), any(RowMapper.class), eq("ghost")))
                .thenReturn(List.of());

        assertThat(repository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void findByUsername_returnsEmpty_whenInputNullOrBlank() {
        assertThat(repository.findByUsername(null)).isEmpty();
        assertThat(repository.findByUsername("   ")).isEmpty();
    }

    @Test
    void updatePassword_updatesRowAndReturnsTrue() {
        when(jdbcTemplate.update("UPDATE user SET password = ? WHERE user_id = ?", "$2a$10$newHash", 2))
                .thenReturn(1);

        assertThat(repository.updatePassword(2, "$2a$10$newHash")).isTrue();
    }

    @Test
    void updatePassword_returnsFalseWhenNoRowUpdated() {
        when(jdbcTemplate.update("UPDATE user SET password = ? WHERE user_id = ?", "$2a$10$newHash", 99))
                .thenReturn(0);

        assertThat(repository.updatePassword(99, "$2a$10$newHash")).isFalse();
    }

    @Test
    void findPasswordByUserId_returnsPassword_whenFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT password FROM user WHERE user_id = ?"), eq(String.class), eq(1)))
                .thenReturn("$2a$10$hashedPass");

        assertThat(repository.findPasswordByUserId(1)).contains("$2a$10$hashedPass");
    }

    @Test
    void findPasswordByUserId_returnsEmpty_whenEmptyResultOrInvalidId() {
        when(jdbcTemplate.queryForObject(eq("SELECT password FROM user WHERE user_id = ?"), eq(String.class), eq(99)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.findPasswordByUserId(99)).isEmpty();
        assertThat(repository.findPasswordByUserId(0)).isEmpty();
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
    void getUserProfile_returnsStudentProfile() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("student1");
        user.setRole("Student");

        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE user_id = ?"), any(RowMapper.class), eq(1)))
                .thenReturn(List.of(user));

        Optional<UserProfileDTO> profile = repository.getUserProfile(1);

        assertThat(profile).isPresent();
        assertThat(profile.get().getUsername()).isEqualTo("student1");
        assertThat(profile.get().getRole()).isEqualTo("Student");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserProfile_returnsEmpty_whenUserNotFound() {
        when(jdbcTemplate.query(eq("SELECT * FROM user WHERE user_id = ?"), any(RowMapper.class), eq(99)))
                .thenReturn(List.of());

        assertThat(repository.getUserProfile(99)).isEmpty();
    }

    @Test
    void isUsernameInUse_returnsTrue_whenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?) AND user_id != ?"), eq(Integer.class), eq("admin"), eq(2)))
                .thenReturn(1);

        assertThat(repository.isUsernameInUse("admin", 2)).isTrue();
    }

    @Test
    void isUsernameInUse_returnsFalse_whenCountZero() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?) AND user_id != ?"), eq(Integer.class), eq("free"), eq(2)))
                .thenReturn(0);

        assertThat(repository.isUsernameInUse("free", 2)).isFalse();
    }

    @Test
    void existsByUsername_returnsTrue_whenFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)"), eq(Integer.class), eq("admin")))
                .thenReturn(1);

        assertThat(repository.existsByUsername("admin")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenNotFound() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM user WHERE LOWER(username) = LOWER(?)"), eq(Integer.class), eq("newuser")))
                .thenReturn(0);

        assertThat(repository.existsByUsername("newuser")).isFalse();
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
    @SuppressWarnings("unchecked")
    void updateUserProfile_updatesUserAndStudentTable() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("updatedUser")
                .name("Updated Name")
                .email("updated@test.com")
                .contact("999")
                .password("$2a$10$preEncodedPass")
                .build();

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
                eq("updatedUser"), eq("$2a$10$preEncodedPass"), any(), eq(1));
    }

    @Test
    void updateUserProfile_propagatesDatabaseFailureInsteadOfReportingAnEmptyProfile() {
        UserProfileDTO profile = UserProfileDTO.builder().userId(1).username("updatedUser").build();

        when(jdbcTemplate.update(eq("UPDATE user SET username = ?, profile_image = ? WHERE user_id = ?"),
                any(), any(), eq(1)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.updateUserProfile(profile))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void updateUserProfile_propagatesFailureOfTheRequestTableSync() {
        UserProfileDTO profile = UserProfileDTO.builder().userId(1).username("updatedUser").build();

        when(jdbcTemplate.queryForObject(eq("SELECT user_role FROM user WHERE user_id = ?"), eq(String.class), eq(1)))
                .thenReturn("Student");
        when(jdbcTemplate.update(eq("UPDATE user SET username = ?, profile_image = ? WHERE user_id = ?"),
                any(), any(), eq(1)))
                .thenReturn(1);
        when(jdbcTemplate.update(startsWith("UPDATE student SET"), any(), any(), any(), any(), eq(1)))
                .thenReturn(1);
        when(jdbcTemplate.update(eq("UPDATE request SET name = ?, email = ?, contact = ? WHERE username = ?"),
                any(), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("read only"));

        assertThatThrownBy(() -> repository.updateUserProfile(profile))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
