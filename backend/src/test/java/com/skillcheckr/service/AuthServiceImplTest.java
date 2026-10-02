package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillcheckr.exception.AccountDisabledException;
import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.repository.AuthRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_authenticatesUser_whenBcryptPasswordMatches() {
        User expectedUser = user(1, "Student", "Active");
        expectedUser.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("student")).thenReturn(Optional.of(expectedUser));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThat(authService.login("student", "pass")).containsSame(expectedUser);
        verify(authRepository).findByUsername("student");
    }

    @Test
    void login_upgradesPlaintextPassword_whenMatching() {
        User legacyUser = user(2, "Teacher", "Active");
        legacyUser.setPassword("plainPass");
        when(authRepository.findByUsername("legacyTeacher")).thenReturn(Optional.of(legacyUser));
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$newHash");

        Optional<User> loggedIn = authService.login("legacyTeacher", "plainPass");

        assertThat(loggedIn).containsSame(legacyUser);
        assertThat(legacyUser.getPassword()).isEqualTo("$2a$10$newHash");
        verify(authRepository).updatePassword(2, "$2a$10$newHash");
    }

    @Test
    void login_returnsEmptyWhenUserNotFound() {
        when(authRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(authService.login("nobody", "nope")).isEmpty();
    }

    @Test
    void login_returnsEmptyWhenPasswordMismatches() {
        User user = user(1, "Student", "Active");
        user.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertThat(authService.login("student", "wrong")).isEmpty();
    }

    @Test
    void login_returnsEmptyWhenUsernameOrPasswordNull() {
        assertThat(authService.login(null, "pass")).isEmpty();
        assertThat(authService.login("user", null)).isEmpty();
    }

    @Test
    void login_allowsAnActiveUser() {
        User active = user(1, "Student", "Active");
        active.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("active")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThat(authService.login("active", "pass")).containsSame(active);
    }

    @Test
    void login_allowsAUserWhoseStatusIsMissing() {
        User legacy = user(2, "Student", null);
        legacy.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("legacy")).thenReturn(Optional.of(legacy));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThat(authService.login("legacy", "pass")).containsSame(legacy);
    }

    @Test
    void login_rejectsAnInactiveUser() {
        User blocked = user(3, "Student", "Inactive");
        blocked.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("blocked")).thenReturn(Optional.of(blocked));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("blocked", "pass"))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessage("This account has been deactivated. Please contact your administrator.");
    }

    @Test
    void login_rejectsAnInactiveUserRegardlessOfStatusCasing() {
        User blocked = user(4, "Teacher", "inactive");
        blocked.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("blocked")).thenReturn(Optional.of(blocked));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("blocked", "pass"))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void login_doesNotReportADisabledAccountAsAWrongPassword() {
        User blocked = user(5, "Student", "Inactive");
        blocked.setPassword("$2a$10$hashed");
        when(authRepository.findByUsername("blocked")).thenReturn(Optional.of(blocked));
        when(passwordEncoder.matches("pass", "$2a$10$hashed")).thenReturn(true);

        assertThatExceptionOfType(AccountDisabledException.class)
                .isThrownBy(() -> authService.login("blocked", "pass"));
    }

    private User user(int id, String role, String status) {
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    @Test
    void getStudentIdByUserId_delegatesToRepository() {
        when(authRepository.getStudentIdByUserId(10)).thenReturn(7);

        assertThat(authService.getStudentIdByUserId(10)).isEqualTo(7);
        verify(authRepository).getStudentIdByUserId(10);
    }

    @Test
    void getTeacherIdByUserId_delegatesToRepository() {
        when(authRepository.getTeacherIdByUserId(20)).thenReturn(5);

        assertThat(authService.getTeacherIdByUserId(20)).isEqualTo(5);
        verify(authRepository).getTeacherIdByUserId(20);
    }

    @Test
    void getAdminIdByUserId_delegatesToRepository() {
        when(authRepository.getAdminIdByUserId(30)).thenReturn(1);

        assertThat(authService.getAdminIdByUserId(30)).isEqualTo(1);
        verify(authRepository).getAdminIdByUserId(30);
    }

    @Test
    void getUserProfile_delegatesToRepository() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("user1")
                .name("User One")
                .email("user1@test.com")
                .build();
        when(authRepository.getUserProfile(1)).thenReturn(Optional.of(profile));

        assertThat(authService.getUserProfile(1)).containsSame(profile);
        verify(authRepository).getUserProfile(1);
    }

    @Test
    void updateUserProfile_encodesPasswordAndDelegatesToRepository() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("user1_updated")
                .name("User One Updated")
                .email("user1_updated@test.com")
                .password("plainSecret")
                .build();
        when(passwordEncoder.encode("plainSecret")).thenReturn("$2a$10$encoded");
        when(authRepository.updateUserProfile(profile)).thenReturn(Optional.of(profile));

        assertThat(authService.updateUserProfile(profile)).containsSame(profile);
        assertThat(profile.getPassword()).isEqualTo("$2a$10$encoded");
        verify(authRepository).updateUserProfile(profile);
    }

    @Test
    void updateUserProfile_leavesNullPasswordUnchanged() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("user1_updated")
                .password(null)
                .build();
        when(authRepository.updateUserProfile(profile)).thenReturn(Optional.of(profile));

        assertThat(authService.updateUserProfile(profile)).containsSame(profile);
        verify(passwordEncoder, never()).encode(any());
        verify(authRepository).updateUserProfile(profile);
    }

    @Test
    void isUsernameInUse_delegatesToRepository() {
        when(authRepository.isUsernameInUse("user1", 1)).thenReturn(true);

        assertThat(authService.isUsernameInUse("user1", 1)).isTrue();
        verify(authRepository).isUsernameInUse("user1", 1);
    }

    @Test
    void isEmailInUse_delegatesToRepository() {
        when(authRepository.isEmailInUse("user1@test.com", 1)).thenReturn(false);

        assertThat(authService.isEmailInUse("user1@test.com", 1)).isFalse();
        verify(authRepository).isEmailInUse("user1@test.com", 1);
    }

    @Test
    void isUserActive_delegatesToRepository() {
        when(authRepository.isUserActive(5)).thenReturn(false);

        assertThat(authService.isUserActive(5)).isFalse();
        verify(authRepository).isUserActive(5);
    }

    @Test
    void verifyCurrentPassword_returnsTrue_whenBcryptMatches() {
        when(authRepository.findPasswordByUserId(1)).thenReturn(Optional.of("$2a$10$hashedPass"));
        when(passwordEncoder.matches("secret", "$2a$10$hashedPass")).thenReturn(true);

        assertThat(authService.verifyCurrentPassword(1, "secret")).isTrue();
    }

    @Test
    void verifyCurrentPassword_returnsTrue_whenPlaintextMatches() {
        when(authRepository.findPasswordByUserId(1)).thenReturn(Optional.of("plainSecret"));

        assertThat(authService.verifyCurrentPassword(1, "plainSecret")).isTrue();
    }

    @Test
    void verifyCurrentPassword_returnsFalse_whenMismatchesOrNull() {
        when(authRepository.findPasswordByUserId(1)).thenReturn(Optional.of("$2a$10$hashedPass"));
        when(passwordEncoder.matches("wrong", "$2a$10$hashedPass")).thenReturn(false);

        assertThat(authService.verifyCurrentPassword(1, "wrong")).isFalse();
        assertThat(authService.verifyCurrentPassword(1, null)).isFalse();
        assertThat(authService.verifyCurrentPassword(0, "secret")).isFalse();
    }
}