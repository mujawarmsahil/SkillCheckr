package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.repository.AuthRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_delegatesToRepository() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setRole("Student");
        when(authRepository.login("student", "pass")).thenReturn(Optional.of(expectedUser));

        assertThat(authService.login("student", "pass")).containsSame(expectedUser);
        verify(authRepository).login("student", "pass");
    }

    @Test
    void login_returnsEmptyWhenRepositoryDoesNotMatch() {
        when(authRepository.login("nobody", "nope")).thenReturn(Optional.empty());

        assertThat(authService.login("nobody", "nope")).isEmpty();
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
    void updateUserProfile_delegatesToRepository() {
        UserProfileDTO profile = UserProfileDTO.builder()
                .userId(1)
                .username("user1_updated")
                .name("User One Updated")
                .email("user1_updated@test.com")
                .build();
        when(authRepository.updateUserProfile(profile)).thenReturn(Optional.of(profile));

        assertThat(authService.updateUserProfile(profile)).containsSame(profile);
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
    void verifyCurrentPassword_delegatesToRepository() {
        when(authRepository.verifyCurrentPassword(1, "secret")).thenReturn(true);

        assertThat(authService.verifyCurrentPassword(1, "secret")).isTrue();
        verify(authRepository).verifyCurrentPassword(1, "secret");
    }
}