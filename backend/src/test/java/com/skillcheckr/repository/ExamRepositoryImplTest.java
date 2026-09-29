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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;

@ExtendWith(MockitoExtension.class)
class ExamRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExamRepositoryImpl repository;

    private Exam examFor(String subjectCode) {
        Exam exam = new Exam();
        exam.setExamName("Maths Final");
        exam.setDate("2030-01-15T09:00:00");
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(10, 30));
        exam.setDurationMinutes(60);
        exam.setTotalMarks(100);
        exam.setPassingMarks(40);
        Subject subject = new Subject();
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName("Advanced Maths");
        exam.setSubject(subject);
        return exam;
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveExam_returnsTheCreatedExamWithItsGeneratedId() {
        Subject stored = new Subject(5, "Advanced Maths", "MATH202");
        when(jdbcTemplate.query(
                eq("SELECT subject_id, subject_name, subject_code FROM subject WHERE subject_code = ? LIMIT 1"),
                any(RowMapper.class), eq("MATH202"))).thenReturn(List.of(stored));
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("exam_id", 12L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Exam saved = repository.saveExam(examFor("MATH202"));

        assertThat(saved.getExamId()).isEqualTo(12);
        assertThat(saved.getSubject().getSubjectId()).isEqualTo(5);
        assertThat(saved.getStatus()).isEqualTo(ExamConstants.EXAM_STATUS_PENDING);
        assertThat(saved.getExamType()).isEqualTo(ExamConstants.QUESTION_TYPE_MCQ);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveExam_rejectsAnUnknownSubjectWithoutInserting() {
        when(jdbcTemplate.query(
                eq("SELECT subject_id, subject_name, subject_code FROM subject WHERE subject_code = ? LIMIT 1"),
                any(RowMapper.class), eq("MATH202"))).thenReturn(List.of());

        assertThatThrownBy(() -> repository.saveExam(examFor("MATH202")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not exist");
        verify(jdbcTemplate, never()).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }

    @Test
    void saveExam_rejectsAnUnknownTeacherWithoutInserting() {
        Subject stored = new Subject(5, "Advanced Maths", "MATH202");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("MATH202"))).thenReturn(List.of(stored));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM teacher WHERE teacher_id = ?"),
                eq(Integer.class), eq(10))).thenReturn(0);

        assertThatThrownBy(() -> {
            Exam exam = examFor("MATH202");
            exam.setTeacherId(10);
            repository.saveExam(exam);
        }).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Teacher 10 does not exist");
    }

    @Test
    void saveExam_requiresADate() {
        Exam exam = examFor("MATH202");
        exam.setDate(null);

        assertThatThrownBy(() -> repository.saveExam(exam))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam date is required");
    }

    @Test
    void saveExam_requiresStartAndEndTimes() {
        Exam exam = examFor("MATH202");
        exam.setStartTime(null);

        assertThatThrownBy(() -> repository.saveExam(exam))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam start and end times are required");
    }

    @Test
    void saveExam_requiresAKnownSubjectCode() {
        assertThatThrownBy(() -> repository.saveExam(examFor("  ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A valid subject is required");
    }

    @Test
    void saveExam_rejectsAnUnreadableDate() {
        Subject stored = new Subject(5, "Advanced Maths", "MATH202");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("MATH202"))).thenReturn(List.of(stored));
        Exam exam = examFor("MATH202");
        exam.setDate("15-01-2030");

        assertThatThrownBy(() -> repository.saveExam(exam))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam date must be a valid date and time value");
    }

    @Test
    void deleteExamById_refusesAnExamThatAlreadyHasAttempts() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE exam_id = ?"),
                eq(Integer.class), eq(1))).thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE exam_id = ?"),
                eq(Integer.class), eq(1))).thenReturn(3);

        assertThatThrownBy(() -> repository.deleteExamById(1))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("already has student attempts");
        verify(jdbcTemplate, never()).update(eq("DELETE FROM exam WHERE exam_id = ?"), eq(1));
    }

    @Test
    void deleteExamById_removesOnlyTheExamAndItsQuestionAssignments() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE exam_id = ?"),
                eq(Integer.class), eq(1))).thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_attempt WHERE exam_id = ?"),
                eq(Integer.class), eq(1))).thenReturn(0);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        assertThat(repository.deleteExamById(1)).isTrue();
        verify(jdbcTemplate).update("DELETE FROM exam_question WHERE exam_id = ?", 1);
        verify(jdbcTemplate).update("DELETE FROM exam WHERE exam_id = ?", 1);
        // The shared subject question bank must survive an exam deletion.
        verify(jdbcTemplate, never()).update(anyString(), eq("DELETE FROM question WHERE subject_id = ?"));
        verify(jdbcTemplate, never()).update(anyString(), eq("DELETE FROM answer WHERE question_id = ?"));
    }

    @Test
    void deleteExamById_returnsFalse_forAMissingExam() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam WHERE exam_id = ?"),
                eq(Integer.class), eq(9))).thenReturn(0);

        assertThat(repository.deleteExamById(9)).isFalse();
    }

    @Test
    void acceptExam_returnsTrue_whenStatusUpdated() {
        when(jdbcTemplate.update(eq("UPDATE exam SET status = 'Upcoming' WHERE exam_id = ?"), eq(3)))
                .thenReturn(1);

        assertThat(repository.acceptExam(3)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllUpcomingExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.getAllUpcomingExams();

        assertThat(exams).containsExactly(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllCompletedExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(2);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.getAllCompletedExams();

        assertThat(exams).containsExactly(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(3);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.getAllExams();

        assertThat(exams).containsExactly(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExamById_returnsExam_whenFound() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1))).thenReturn(List.of(exam));

        Optional<Exam> found = repository.getExamById(1);

        assertThat(found).containsSame(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExamsByTeacherId_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10))).thenReturn(List.of(exam));

        List<Exam> exams = repository.getExamsByTeacherId(10);

        assertThat(exams).containsExactly(exam);
    }

    @Test
    void registerStudentForExam_returnsTrue_whenInsertSucceeds() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM exam_registration WHERE student_id = ? AND exam_id = ?"), eq(Integer.class), eq(1), eq(2)))
                .thenReturn(0);
        when(jdbcTemplate.update(anyString(), eq(1), eq(2))).thenReturn(1);

        boolean registered = repository.registerStudentForExam(1, 2);

        assertThat(registered).isTrue();
    }

    @Test
    void registerStudentForExam_isIdempotent_forAnExistingRegistration() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1), eq(2))).thenReturn(1);

        assertThat(repository.registerStudentForExam(1, 2)).isTrue();
        verify(jdbcTemplate, never()).update(anyString(), eq(1), eq(2));
    }

    @Test
    void isStudentRegisteredForExam_returnsTrue_whenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1), eq(2))).thenReturn(1);

        assertThat(repository.isStudentRegisteredForExam(1, 2)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRegisteredExamIdsForStudent_returnsIds() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(5))).thenReturn(List.of(10, 20));

        List<Integer> ids = repository.getRegisteredExamIdsForStudent(5);

        assertThat(ids).containsExactly(10, 20);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRegistrationsByStudentId_returnsList() {
        ExamRegistration reg = new ExamRegistration();
        reg.setRegistrationId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(5))).thenReturn(List.of(reg));

        List<ExamRegistration> regs = repository.getRegistrationsByStudentId(5);

        assertThat(regs).containsExactly(reg);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRegisteredStudentsByExamId_returnsList() {
        Student s = new Student();
        s.setStudentId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2))).thenReturn(List.of(s));

        List<Student> students = repository.getRegisteredStudentsByExamId(2);

        assertThat(students).containsExactly(s);
    }

    @Test
    void getRegistrationCountByExamId_returnsCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2))).thenReturn(15);

        assertThat(repository.getRegistrationCountByExamId(2)).isEqualTo(15);
    }

    @Test
    void unregisterStudentFromExam_returnsTrue_whenDeleted() {
        when(jdbcTemplate.update(anyString(), eq(1), eq(2))).thenReturn(1);

        assertThat(repository.unregisterStudentFromExam(1, 2)).isTrue();
    }

    @Test
    void unregisterStudentFromExam_returnsFalse_whenNothingWasRemoved() {
        when(jdbcTemplate.update(anyString(), eq(1), eq(2))).thenReturn(0);

        assertThat(repository.unregisterStudentFromExam(1, 2)).isFalse();
    }
}
