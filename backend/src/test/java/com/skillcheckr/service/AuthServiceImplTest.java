package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.User;
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
        when(authRepository.login("student", "pass")).thenReturn(expectedUser);

        User actual = authService.login("student", "pass");

        assertThat(actual).isSameAs(expectedUser);
        verify(authRepository).login("student", "pass");
    }

    @Test
    void login_returnsNullWhenRepositoryDoesNotMatch() {
        when(authRepository.login("nobody", "nope")).thenReturn(null);

        assertThat(authService.login("nobody", "nope")).isNull();
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
}