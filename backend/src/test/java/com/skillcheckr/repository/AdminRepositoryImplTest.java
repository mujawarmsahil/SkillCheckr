package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.persistence.entity.RegistrationRequestEntity;
import com.skillcheckr.persistence.entity.StudentProfileEntity;
import com.skillcheckr.persistence.entity.TeacherProfileEntity;
import com.skillcheckr.persistence.entity.UserAccountEntity;
import com.skillcheckr.persistence.repository.RegistrationRequestJpaRepository;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.TeacherProfileJpaRepository;
import com.skillcheckr.persistence.repository.ExamAttemptJpaRepository;
import com.skillcheckr.persistence.repository.ExamJpaRepository;
import com.skillcheckr.persistence.repository.QuestionJpaRepository;
import com.skillcheckr.persistence.repository.ResultJpaRepository;
import com.skillcheckr.persistence.repository.SubjectJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@ExtendWith(MockitoExtension.class)
class AdminRepositoryImplTest {

    @Mock
    private UserAccountJpaRepository accountRepository;

    @Mock
    private StudentProfileJpaRepository studentRepository;

    @Mock
    private TeacherProfileJpaRepository teacherRepository;

    @Mock
    private RegistrationRequestJpaRepository requestRepository;
    @Mock
    private ExamJpaRepository examRepository;
    @Mock
    private SubjectJpaRepository subjectRepository;
    @Mock
    private QuestionJpaRepository questionRepository;
    @Mock
    private ResultJpaRepository resultRepository;
    @Mock
    private ExamAttemptJpaRepository attemptRepository;

    @InjectMocks
    private AdminRepositoryImpl repository;

    @Test
    void usernameAndEmailChecksUseJpaRepositories() {
        when(accountRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);
        when(teacherRepository.existsByEmailIgnoreCase("teacher@example.test")).thenReturn(true);
        when(studentRepository.existsByEmailIgnoreCase("student@example.test")).thenReturn(true);

        assertThat(repository.existsUserByUsername(" alice ")).isTrue();
        assertThat(repository.existsTeacherByEmail(" teacher@example.test ")).isTrue();
        assertThat(repository.existsStudentByEmail(" student@example.test ")).isTrue();
    }

    @Test
    void usernameAndEmailChecksHandleNullInputsWithoutQuerying() {
        assertThat(repository.existsUserByUsername(null)).isFalse();
        assertThat(repository.existsTeacherByEmail(null)).isFalse();
        assertThat(repository.existsStudentByEmail(null)).isFalse();

        verify(accountRepository, never()).existsByUsernameIgnoreCase(anyString());
        verify(teacherRepository, never()).existsByEmailIgnoreCase(anyString());
        verify(studentRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void createUserPersistsLocalActiveAccountAndReturnsGeneratedId() {
        doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setUserId(42);
            return entity;
        }).when(accountRepository).save(any(UserAccountEntity.class));

        int id = repository.createUser("alice", "$2a$10$hash", "Student");

        assertThat(id).isEqualTo(42);
        ArgumentCaptor<UserAccountEntity> account = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(accountRepository).save(account.capture());
        assertThat(account.getValue().getUsername()).isEqualTo("alice");
        assertThat(account.getValue().getPassword()).isEqualTo("$2a$10$hash");
        assertThat(account.getValue().getRole()).isEqualTo("Student");
        assertThat(account.getValue().getAuthProvider()).isEqualTo("LOCAL");
        assertThat(account.getValue().getStatus()).isEqualTo("Active");
    }

    @Test
    void createStudentAndTeacherPersistRoleProfileWithOwningAccount() {
        UserAccountEntity studentAccount = account(10, "student", "Student", "Active");
        UserAccountEntity teacherAccount = account(11, "teacher", "Teacher", "Active");
        when(accountRepository.findById(10)).thenReturn(Optional.of(studentAccount));
        when(accountRepository.findById(11)).thenReturn(Optional.of(teacherAccount));

        repository.createStudent(10, "Student Name", "111", "student@example.test");
        repository.createTeacher(11, "Teacher Name", "222", "teacher@example.test");

        ArgumentCaptor<StudentProfileEntity> student = ArgumentCaptor.forClass(StudentProfileEntity.class);
        verify(studentRepository).save(student.capture());
        assertThat(student.getValue().getUser()).isSameAs(studentAccount);
        assertThat(student.getValue().getStatus()).isEqualTo("Active");

        ArgumentCaptor<TeacherProfileEntity> teacher = ArgumentCaptor.forClass(TeacherProfileEntity.class);
        verify(teacherRepository).save(teacher.capture());
        assertThat(teacher.getValue().getUser()).isSameAs(teacherAccount);
        assertThat(teacher.getValue().getStatus()).isEqualTo("Active");
    }

    @Test
    void createProfileFailsWhenTheOwningAccountDoesNotExist() {
        when(accountRepository.findById(55)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.createStudent(55, "Missing", null, "missing@example.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Account not found for student profile");
        verify(studentRepository, never()).save(any(StudentProfileEntity.class));
    }

    @Test
    void findRequestByIdMapsTheRegistrationRequestIncludingPassword() {
        RegistrationRequestEntity entity = new RegistrationRequestEntity();
        entity.setRequestId(1);
        entity.setName("Alice");
        entity.setEmail("alice@example.test");
        entity.setUsername("alice");
        entity.setPassword("$2a$10$hash");
        entity.setContact("555-0100");
        entity.setRequestedRole("Student");
        entity.setStatus("Pending");
        when(requestRepository.findById(1)).thenReturn(Optional.of(entity));

        Optional<RegistrationRequest> result = repository.findRequestById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        assertThat(result.get().getRequestedRole()).isEqualTo("Student");
        assertThat(result.get().getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    void findRequestByIdReturnsEmptyWhenTheRequestIsMissing() {
        when(requestRepository.findById(1)).thenReturn(Optional.empty());

        assertThat(repository.findRequestById(1)).isEmpty();
    }

    @Test
    void updateRequestStatusReturnsWhetherTheRowWasUpdated() {
        when(requestRepository.updateStatus(1, "Approved")).thenReturn(1);

        assertThat(repository.updateRequestStatus(1, "Approved")).isTrue();
    }

    @Test
    void listsMapJpaProfilesToApiModelsInDescendingIdOrder() {
        UserAccountEntity studentAccount = account(20, "student", "Student", "Active");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(3);
        student.setUser(studentAccount);
        student.setName("Student Name");
        student.setEmail("student@example.test");
        student.setStatus("Inactive");
        when(studentRepository.findAllByOrderByStudentIdDesc()).thenReturn(List.of(student));

        UserAccountEntity teacherAccount = account(21, "teacher", "Teacher", "Active");
        TeacherProfileEntity teacher = new TeacherProfileEntity();
        teacher.setTeacherId(4);
        teacher.setUser(teacherAccount);
        teacher.setName("Teacher Name");
        teacher.setEmail("teacher@example.test");
        teacher.setStatus("Active");
        when(teacherRepository.findAllByOrderByTeacherIdDesc()).thenReturn(List.of(teacher));

        assertThat(repository.getAllStudent()).singleElement().satisfies(model -> {
            assertThat(model.getStudentId()).isEqualTo(3);
            assertThat(model.getUserId()).isEqualTo(20);
            assertThat(model.getStatus()).isEqualTo("Inactive");
        });
        assertThat(repository.getAllTeacher()).singleElement().satisfies(model -> {
            assertThat(model.getTeacherId()).isEqualTo(4);
            assertThat(model.getUserId()).isEqualTo(21);
            assertThat(model.getStatus()).isEqualTo("Active");
        });
    }

    @Test
    void deletingStudentWithAcademicHistoryDeactivatesProfileAndAccount() {
        UserAccountEntity account = account(30, "student", "Student", "Active");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(6);
        student.setUser(account);
        when(studentRepository.findById(6)).thenReturn(Optional.of(student));
        when(resultRepository.countByStudentId(6)).thenReturn(1L);

        assertThat(repository.deleteStudentById(6)).isTrue();

        assertThat(student.getStatus()).isEqualTo("Inactive");
        assertThat(account.getStatus()).isEqualTo("Inactive");
        verify(studentRepository).save(student);
        verify(accountRepository).save(account);
        verify(studentRepository, never()).delete(any(StudentProfileEntity.class));
    }

    @Test
    void deletingStudentWithoutHistoryDeletesProfileThenAccount() {
        UserAccountEntity account = account(30, "student", "Student", "Active");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(6);
        student.setUser(account);
        when(studentRepository.findById(6)).thenReturn(Optional.of(student));
        when(resultRepository.countByStudentId(6)).thenReturn(0L);
        when(attemptRepository.countByStudentId(6)).thenReturn(0);

        assertThat(repository.deleteStudentById(6)).isTrue();

        verify(studentRepository).delete(student);
        verify(studentRepository).flush();
        verify(accountRepository).delete(account);
    }

    @Test
    void deletingTeacherWithExamHistoryDeactivatesInsteadOfDeleting() {
        UserAccountEntity account = account(40, "teacher", "Teacher", "Active");
        TeacherProfileEntity teacher = new TeacherProfileEntity();
        teacher.setTeacherId(5);
        teacher.setUser(account);
        when(teacherRepository.findById(5)).thenReturn(Optional.of(teacher));
        when(examRepository.findByTeacherIdOrderByExamIdDesc(5)).thenReturn(java.util.List.of(new com.skillcheckr.persistence.entity.ExamEntity()));

        assertThat(repository.deleteTeacherById(5)).isTrue();

        assertThat(teacher.getStatus()).isEqualTo("Inactive");
        assertThat(account.getStatus()).isEqualTo("Inactive");
        verify(teacherRepository, never()).delete(any(TeacherProfileEntity.class));
    }

    @Test
    void deletingMissingProfileReturnsFalse() {
        when(studentRepository.findById(6)).thenReturn(Optional.empty());

        assertThat(repository.deleteStudentById(6)).isFalse();

        verify(resultRepository, never()).countByStudentId(6);
    }

    @Test
    void statusChangesUpdateBothProfileAndAccount() {
        UserAccountEntity account = account(30, "student", "Student", "Active");
        StudentProfileEntity student = new StudentProfileEntity();
        student.setStudentId(6);
        student.setUser(account);
        when(studentRepository.findById(6)).thenReturn(Optional.of(student));

        assertThat(repository.toggleStudentStatus(6, "Inactive")).isTrue();

        assertThat(student.getStatus()).isEqualTo("Inactive");
        assertThat(account.getStatus()).isEqualTo("Inactive");
        verify(studentRepository).save(student);
        verify(accountRepository).save(account);
    }

    @Test
    void adminStatsPreservesJdbcBackedAggregatesAndPropagatesFailures() {
        when(studentRepository.count()).thenReturn(10L);
        when(teacherRepository.count()).thenReturn(10L);
        when(examRepository.count()).thenReturn(10L);

        AdminStatsResponse stats = repository.getAdminStats();

        assertThat(stats.getTotalStudents()).isEqualTo(10);
        assertThat(stats.getTotalTeachers()).isEqualTo(10);
        assertThat(stats.getTotalExams()).isEqualTo(10);

        when(studentRepository.count()).thenThrow(new DataAccessResourceFailureException("connection reset"));
        assertThatThrownBy(repository::getAdminStats).isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static UserAccountEntity account(int id, String username, String role, String status) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(id);
        entity.setUsername(username);
        entity.setRole(role);
        entity.setStatus(status);
        entity.setAuthProvider("LOCAL");
        return entity;
    }
}
