package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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

    @Test
    void saveExam_returnsSubject_whenValid() {
        Exam exam = new Exam();
        exam.setExamName("Maths Final");
        exam.setDate("2030-01-15T09:00:00");
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(10, 30));
        Subject sub = new Subject();
        sub.setSubjectCode("MATH202");
        sub.setSubjectName("Advanced Maths");
        exam.setSubject(sub);

        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM subject WHERE subject_code = ?"), eq(Integer.class), eq("MATH202")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT subject_id FROM subject WHERE subject_code = ? LIMIT 1"), eq(Integer.class), eq("MATH202")))
                .thenReturn(5);

        Subject result = repository.saveExam(exam);

        assertThat(result).isNotNull();
        assertThat(result.getSubjectId()).isEqualTo(5);
        assertThat(result.getSubjectCode()).isEqualTo("MATH202");
    }

    @Test
    void saveExam_returnsNull_whenDateOrTimeMissing() {
        Exam exam = new Exam();
        assertThat(repository.saveExam(exam)).isNull();

        exam.setDate("2030-01-15T09:00:00");
        assertThat(repository.saveExam(exam)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteExamById_deletesQuestionsAnswersAndExam() {
        when(jdbcTemplate.query(eq("SELECT subject_id FROM exam WHERE exam_id = ?"), any(RowMapper.class), eq(1)))
                .thenReturn(List.of(10));
        when(jdbcTemplate.query(eq("SELECT question_id FROM question WHERE subject_id = ?"), any(RowMapper.class), eq(10)))
                .thenReturn(List.of(101, 102));

        boolean deleted = repository.deleteExamById(1);

        assertThat(deleted).isTrue();
        verify(jdbcTemplate).update("DELETE FROM answer WHERE question_id = ?", 101);
        verify(jdbcTemplate).update("DELETE FROM answer WHERE question_id = ?", 102);
        verify(jdbcTemplate).update("DELETE FROM question WHERE subject_id = ?", 10);
        verify(jdbcTemplate).update("DELETE FROM exam WHERE exam_id = ?", 1);
    }

    @Test
    void acceptExam_returnsTrue_whenStatusUpdated() {
        when(jdbcTemplate.update(eq("UPDATE exam SET status = 'Upcoming' WHERE exam_id = ?"), eq(3)))
                .thenReturn(1);

        assertThat(repository.acceptExam(3)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void viewAllUpcomingExam_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.viewAllUpcomingExam();

        assertThat(exams).containsExactly(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void viewAllCompletedExam_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(2);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.viewAllCompletedExam();

        assertThat(exams).containsExactly(exam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void viewAllExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(3);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));

        List<Exam> exams = repository.viewAllExams();

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
}
