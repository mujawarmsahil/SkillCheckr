package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.skillcheckr.persistence.repository.ExamJpaRepository;
import com.skillcheckr.persistence.repository.ExamQuestionJpaRepository;
import com.skillcheckr.persistence.repository.ExamRegistrationJpaRepository;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;

@ExtendWith(MockitoExtension.class)
class ExamRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ExamJpaRepository examRepository;
    @Mock
    private ExamRegistrationJpaRepository registrationRepository;
    @Mock
    private ExamQuestionJpaRepository examQuestionRepository;

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
        when(examRepository.countExam(1)).thenReturn(1);
        when(examRepository.countAttempts(1)).thenReturn(3);

        assertThatThrownBy(() -> repository.deleteExamById(1))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("already has student attempts");
    }

    @Test
    void deleteExamById_removesOnlyTheExamAndItsQuestionAssignments() {
        when(examRepository.countExam(1)).thenReturn(1);
        when(examRepository.countAttempts(1)).thenReturn(0);
        when(examRepository.countRegistrations(1)).thenReturn(0);

        assertThat(repository.deleteExamById(1)).isTrue();
        verify(examQuestionRepository).deleteByExamId(1);
        verify(examRepository).deleteById(1);
    }

    @Test
    void deleteExamById_returnsFalse_forAMissingExam() {
        when(examRepository.countExam(9)).thenReturn(0);

        assertThat(repository.deleteExamById(9)).isFalse();
    }

    @Test
    void deleteExamById_refusesAnExamWithRegistrations() {
        when(examRepository.countExam(1)).thenReturn(1);
        when(examRepository.countAttempts(1)).thenReturn(0);
        when(examRepository.countRegistrations(1)).thenReturn(2);

        assertThatThrownBy(() -> repository.deleteExamById(1))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("student registrations");
        verify(jdbcTemplate, never()).update(eq("DELETE FROM exam WHERE exam_id = ?"), eq(1));
    }

    @Test
    void acceptExam_returnsTrue_whenStatusUpdated() {
        when(examRepository.acceptExam(3)).thenReturn(1);

        assertThat(repository.acceptExam(3)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllUpcomingExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("exam_id", 1, "question_count", 0)));

        List<Exam> exams = repository.getAllUpcomingExams();

        assertThat(exams).containsExactly(exam);
        assertListingCountQueryUses(exams, 1);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllCompletedExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(2);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("exam_id", 2, "question_count", 0)));

        List<Exam> exams = repository.getAllCompletedExams();

        assertThat(exams).containsExactly(exam);
        assertListingCountQueryUses(exams, 2);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllExams_returnsList() {
        Exam exam = new Exam();
        exam.setExamId(3);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(exam));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("exam_id", 3, "question_count", 2)));

        List<Exam> exams = repository.getAllExams();

        assertThat(exams).containsExactly(exam);
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(query.capture(), any(RowMapper.class));
        assertThat(query.getValue()).contains("ORDER BY e.exam_id DESC")
                .doesNotContain("SELECT COUNT(*) FROM exam_question");
        ArgumentCaptor<String> countsQuery = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> examIds = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForList(countsQuery.capture(), examIds.capture());
        assertThat(countsQuery.getValue()).contains("WHERE exam_id IN (?)", "GROUP BY exam_id");
        assertThat(examIds.getValue()).containsExactly(3);
        assertThat(exams.get(0).getQuestionCount()).isEqualTo(2);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllExamsReturnsEmptyWhenTheRepositoryHasNoRows() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.getAllExams()).isEmpty();
        verify(jdbcTemplate, never()).queryForList(anyString(), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllExamsMapsQuestionCountsForEachExamInListingOrder() throws SQLException {
        ResultSet firstRow = examRow(11);
        ResultSet secondRow = examRow(8);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<Exam> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(firstRow, 0), mapper.mapRow(secondRow, 1));
        });
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("exam_id", 11, "question_count", 4),
                Map.of("exam_id", 8, "question_count", 0)));

        List<Exam> exams = repository.getAllExams();

        assertThat(exams).extracting(Exam::getExamId).containsExactly(11, 8);
        assertThat(exams).extracting(Exam::getQuestionCount).containsExactly(4, 0);
    }

    private ResultSet examRow(int examId) throws SQLException {
        ResultSet row = mock(ResultSet.class);
        lenient().when(row.getInt("exam_id")).thenReturn(examId);
        when(row.getObject("exam_date", java.time.LocalDateTime.class)).thenReturn(null);
        return row;
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
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("exam_id", 1, "question_count", 0)));

        List<Exam> exams = repository.getExamsByTeacherId(10);

        assertThat(exams).containsExactly(exam);
        assertListingCountQueryUses(exams, 1);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        ArgumentCaptor<String> listingQuery = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(listingQuery.capture(), any(RowMapper.class), eq(10));
        assertThat(listingQuery.getValue()).contains("WHERE e.teacher_id = ?");
    }

    private void assertListingCountQueryUses(List<Exam> exams, int examId) {
        assertThat(exams).singleElement().extracting(Exam::getQuestionCount).isEqualTo(0);
        ArgumentCaptor<String> countQuery = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> examIds = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForList(countQuery.capture(), examIds.capture());
        assertThat(countQuery.getValue()).contains("WHERE exam_id IN (?)", "GROUP BY exam_id");
        assertThat(examIds.getValue()).containsExactly(examId);
    }

    @Test
    void registerStudentForExam_returnsTrue_whenInsertSucceeds() {
        when(registrationRepository.existsByStudentIdAndExamId(1, 2)).thenReturn(false);
        when(registrationRepository.upsertRegistration(1, 2)).thenReturn(1);

        boolean registered = repository.registerStudentForExam(1, 2);

        assertThat(registered).isTrue();
    }

    @Test
    void registerStudentForExam_isIdempotent_forAnExistingRegistration() {
        when(registrationRepository.existsByStudentIdAndExamId(1, 2)).thenReturn(true);

        assertThat(repository.registerStudentForExam(1, 2)).isTrue();
        verify(registrationRepository, never()).upsertRegistration(1, 2);
    }

    @Test
    void isStudentRegisteredForExam_returnsTrue_whenCountGreaterThanZero() {
        when(registrationRepository.existsByStudentIdAndExamId(1, 2)).thenReturn(true);

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
        when(registrationRepository.countByExamId(2)).thenReturn(15);

        assertThat(repository.getRegistrationCountByExamId(2)).isEqualTo(15);
    }

    @Test
    void unregisterStudentFromExam_returnsTrue_whenDeleted() {
        when(registrationRepository.deleteByStudentIdAndExamId(1, 2)).thenReturn(1);

        assertThat(repository.unregisterStudentFromExam(1, 2)).isTrue();
    }

    @Test
    void unregisterStudentFromExam_returnsFalse_whenNothingWasRemoved() {
        when(registrationRepository.deleteByStudentIdAndExamId(1, 2)).thenReturn(0);

        assertThat(repository.unregisterStudentFromExam(1, 2)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExamByIdForUpdate_readsTheLockedRowInsteadOfAPlainSnapshotRead() {
        Exam locked = new Exam();
        locked.setExamId(7);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of(locked));

        assertThat(repository.getExamByIdForUpdate(7)).containsSame(locked);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(query.capture(), any(RowMapper.class), eq(7));
        assertThat(query.getValue())
                .endsWith("FOR UPDATE")
                .contains("subject_name");
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(Integer.class), eq(7));
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class));
    }
}
