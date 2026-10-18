package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.User;
import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.persistence.entity.StudentProfileEntity;
import com.skillcheckr.persistence.entity.UserAccountEntity;
import com.skillcheckr.persistence.repository.AdminProfileJpaRepository;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.TeacherProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryImplTest {

    @Mock
    private UserAccountJpaRepository accountRepository;

    @Mock
    private StudentProfileJpaRepository studentRepository;

    @Mock
    private TeacherProfileJpaRepository teacherRepository;

    @Mock
    private AdminProfileJpaRepository adminRepository;

    @InjectMocks
    private AuthRepositoryImpl repository;

    @Test
    void findByUsernameMapsTheStoredAccount() {
        UserAccountEntity account = account(1, "student", "Student", "Active");
        account.setPassword("$2a$10$hash");
        when(accountRepository.findByUsernameIgnoreCase("student")).thenReturn(Optional.of(account));

        Optional<User> result = repository.findByUsername(" student ");

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1);
        assertThat(result.get().getPassword()).isEqualTo("$2a$10$hash");
        assertThat(result.get().getRole()).isEqualTo("Student");
    }

    @Test
    void getUserProfileMapsRoleProfileAndAccountImageFallback() {
        UserAccountEntity account = account(1, "student", "Student", "Active");
        account.setProfileImage("account-image");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(12);
        student.setUser(account);
        student.setName("Student Name");
        student.setEmail("student@example.test");
        student.setContact("555");
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(studentRepository.findByUser_UserId(1)).thenReturn(Optional.of(student));

        UserProfileDTO profile = repository.getUserProfile(1).orElseThrow();

        assertThat(profile.getRoleId()).isEqualTo(12);
        assertThat(profile.getName()).isEqualTo("Student Name");
        assertThat(profile.getEmail()).isEqualTo("student@example.test");
        assertThat(profile.getProfileImage()).isEqualTo("account-image");
    }

    @Test
    void updateUserProfileUpdatesAccountAndExistingRoleProfile() {
        UserAccountEntity account = account(1, "student", "Student", "Active");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(12);
        student.setUser(account);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(studentRepository.findByUser_UserId(1)).thenReturn(Optional.of(student));

        UserProfileDTO update = UserProfileDTO.builder()
                .userId(1)
                .username("new-name")
                .name("New Name")
                .email("new@example.test")
                .contact("123")
                .profileImage("new-image")
                .password("$2a$10$new-hash")
                .build();

        UserProfileDTO result = repository.updateUserProfile(update).orElseThrow();

        assertThat(account.getUsername()).isEqualTo("new-name");
        assertThat(account.getPassword()).isEqualTo("$2a$10$new-hash");
        assertThat(student.getName()).isEqualTo("New Name");
        assertThat(student.getEmail()).isEqualTo("new@example.test");
        assertThat(result.getRoleId()).isEqualTo(12);
        verify(accountRepository).save(account);
        verify(studentRepository).save(student);
    }

    @Test
    void uniquenessAndActiveChecksDelegateToJpaRepositories() {
        when(accountRepository.existsByUsernameIgnoreCaseAndUserIdNot("other", 1)).thenReturn(true);
        when(studentRepository.existsByEmailIgnoreCaseAndUser_UserIdNot("mail@example.test", 1)).thenReturn(false);
        when(teacherRepository.existsByEmailIgnoreCaseAndUser_UserIdNot("mail@example.test", 1)).thenReturn(true);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account(1, "student", "Student", "Inactive")));

        assertThat(repository.isUsernameInUse("other", 1)).isTrue();
        assertThat(repository.isEmailInUse("mail@example.test", 1)).isTrue();
        assertThat(repository.isUserActive(1)).isFalse();
    }

    private static UserAccountEntity account(int id, String username, String role, String status) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUserId(id);
        account.setUsername(username);
        account.setRole(role);
        account.setStatus(status);
        account.setAuthProvider("LOCAL");
        return account;
    }
}
