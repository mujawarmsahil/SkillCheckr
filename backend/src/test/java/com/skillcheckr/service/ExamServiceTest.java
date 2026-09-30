package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptStartException;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamRepository;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    private static final int TEACHER_ID = 10;
    private static final int STUDENT_ID = 7;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamSubmissionService examSubmissionService;

    @InjectMocks
    private ExamServiceImpl examService;

    private Exam validExam(String name) {
        Exam exam = new Exam();
        exam.setExamName(name);
        exam.setDate(LocalDate.now().plusDays(30) + "T10:00");
        exam.setStartTime(LocalTime.of(10, 0));
        exam.setEndTime(LocalTime.of(12, 0));
        exam.setDurationMinutes(60);
        exam.setTotalMarks(100);
        exam.setPassingMarks(40);
        exam.setSubject(new Subject(1, "Mathematics", "MATH101"));
        return exam;
    }

    private Exam examAt(int examId, String status, LocalDateTime windowStart, LocalTime endTime) {
        Exam exam = new Exam();
        exam.setExamId(examId);
        exam.setStatus(status);
        exam.setDate(windowStart.toLocalDate() + "T" + windowStart.toLocalTime());
        exam.setStartTime(windowStart.toLocalTime());
        exam.setEndTime(endTime);
        exam.setDurationMinutes(60);
        exam.setTotalMarks(100);
        exam.setPassingMarks(40);
        return exam;
    }

    @Test
    void saveExam_forcesTheAuthenticatedTeacherAndPendingStatus() {
        Exam exam = validExam("Maths");
        exam.setTeacherId(999);
        exam.setStatus(ExamConstants.EXAM_STATUS_UPCOMING);
        when(examRepository.saveExam(any(Exam.class))).thenReturn(exam);

        Exam saved = examService.saveExam(exam, TEACHER_ID);

        ArgumentCaptor<Exam> captor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).saveExam(captor.capture());
        assertThat(captor.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(ExamConstants.EXAM_STATUS_PENDING);
        assertThat(saved).isSameAs(exam);
    }

    @Test
    void saveExam_rejectsInvalidExamNameWithoutCallingRepository() {
        Exam exam = validExam("Java 101");

        assertThatThrownBy(() -> examService.saveExam(exam, TEACHER_ID))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(examRepository);
    }

    @Test
    void saveExam_rejectsExamDateInsideMinimumLeadTimeWithoutCallingRepository() {
        Exam exam = validExam("Java Programming");
        exam.setDate(LocalDate.now().plusDays(9) + "T10:00");

        assertThatThrownBy(() -> examService.saveExam(exam, TEACHER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Exam date must be at least 10 days from today");
        verifyNoInteractions(examRepository);
    }

    @Test
    void saveExam_rejectsPastExamDateWithoutCallingRepository() {
        Exam exam = validExam("Java Programming");
        exam.setDate(LocalDate.now().minusDays(1) + "T10:00");

        assertThatThrownBy(() -> examService.saveExam(exam, TEACHER_ID))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(examRepository);
    }

    @Test
    void saveExam_rejectsADurationLongerThanTheScheduledWindow() {
        Exam exam = validExam("Java Programming");
        exam.setDurationMinutes(600);

        assertThatThrownBy(() -> examService.saveExam(exam, TEACHER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scheduled exam window");
        verifyNoInteractions(examRepository);
    }

    @Test
    void saveExam_rejectsPassingMarksAboveTotalMarks() {
        Exam exam = validExam("Java Programming");
        exam.setPassingMarks(500);

        assertThatThrownBy(() -> examService.saveExam(exam, TEACHER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Passing marks cannot exceed the total marks");
        verifyNoInteractions(examRepository);
    }

    @Test
    void getAllExams_delegatesToRepository() {
        when(examRepository.getAllExams()).thenReturn(List.of(new Exam()));

        assertThat(examService.getAllExams()).hasSize(1);
        verify(examRepository).getAllExams();
    }

    @Test
    void deleteExamById_delegatesToRepository() {
        when(examRepository.deleteExamById(9)).thenReturn(true);

        assertThat(examService.deleteExamById(9)).isTrue();
        verify(examRepository).deleteExamById(9);
    }

    @Test
    void acceptExam_opensAPendingExam() {
        Exam pending = new Exam();
        pending.setExamId(3);
        pending.setStatus(ExamConstants.EXAM_STATUS_PENDING);
        when(examRepository.getExamById(3)).thenReturn(Optional.of(pending));
        when(examRepository.acceptExam(3)).thenReturn(true);

        assertThat(examService.acceptExam(3)).isTrue();
        verify(examRepository).acceptExam(3);
    }

    @Test
    void acceptExam_rejectsACompletedExam() {
        Exam completed = new Exam();
        completed.setExamId(3);
        completed.setStatus(ExamConstants.EXAM_STATUS_COMPLETED);
        when(examRepository.getExamById(3)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> examService.acceptExam(3))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A completed exam can no longer change status");
        verify(examRepository, never()).acceptExam(anyInt());
    }

    @Test
    void acceptExam_rejectsAnUnknownStatus() {
        Exam pending = new Exam();
        pending.setExamId(3);
        pending.setStatus(ExamConstants.EXAM_STATUS_PENDING);
        when(examRepository.getExamById(3)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> examService.updateExamStatus(3, "Published"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not a valid exam status");
    }

    @Test
    void getAllUpcomingExams_delegatesToRepository() {
        when(examRepository.getAllUpcomingExams()).thenReturn(List.of(new Exam()));

        assertThat(examService.getAllUpcomingExams()).hasSize(1);
        verify(examRepository).getAllUpcomingExams();
    }

    @Test
    void getAllCompletedExams_delegatesToRepository() {
        when(examRepository.getAllCompletedExams()).thenReturn(List.of(new Exam()));

        assertThat(examService.getAllCompletedExams()).hasSize(1);
        verify(examRepository).getAllCompletedExams();
    }

    @Test
    void getExamById_delegatesToRepository() {
        Exam exam = new Exam();
        exam.setExamId(1);
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));

        assertThat(examService.getExamById(1)).containsSame(exam);
        verify(examRepository).getExamById(1);
    }

    @Test
    void getExamsByTeacherId_delegatesToRepository() {
        when(examRepository.getExamsByTeacherId(TEACHER_ID)).thenReturn(List.of(new Exam()));

        assertThat(examService.getExamsByTeacherId(TEACHER_ID)).hasSize(1);
        verify(examRepository).getExamsByTeacherId(TEACHER_ID);
    }

    @Test
    void startAttempt_createsAnAttemptInsideTheWindow() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_UPCOMING, now.minusMinutes(5), now.plusHours(1).toLocalTime());
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);
        when(examAttemptRepository.findByExamIdAndStudentId(1, STUDENT_ID)).thenReturn(List.of());
        when(examAttemptRepository.createAttempt(any(ExamAttempt.class))).thenReturn(42);

        AttemptStartResult result = examService.startAttempt(1, STUDENT_ID);

        assertThat(result.isExisting()).isFalse();
        assertThat(result.getAttempt().getAttemptId()).isEqualTo(42);
        assertThat(result.getAttempt().getStatus()).isEqualTo(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS);
    }

    @Test
    void startAttempt_resumesALiveAttempt() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_UPCOMING, now.minusMinutes(5), now.plusHours(1).toLocalTime());
        ExamAttempt live = ExamAttempt.builder()
                .attemptId(7).expiresAt(now.plusMinutes(20)).status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS).build();
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);
        when(examAttemptRepository.findByExamIdAndStudentId(1, STUDENT_ID)).thenReturn(List.of(live));

        AttemptStartResult result = examService.startAttempt(1, STUDENT_ID);

        assertThat(result.isExisting()).isTrue();
        assertThat(result.getAttempt()).isSameAs(live);
        verify(examAttemptRepository, never()).createAttempt(any());
    }

    @Test
    void startAttempt_autoSubmitsAnExpiredAttemptAndRefusesASecondOne() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_UPCOMING, now.minusHours(2), now.minusHours(1).toLocalTime());
        ExamAttempt expired = ExamAttempt.builder()
                .attemptId(7).expiresAt(now.minusMinutes(30)).status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS).build();
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);
        when(examAttemptRepository.findByExamIdAndStudentId(1, STUDENT_ID)).thenReturn(List.of(expired));

        assertThatThrownBy(() -> examService.startAttempt(1, STUDENT_ID))
                .isInstanceOf(AttemptStartException.class)
                .hasMessageContaining("submitted automatically");
        verify(examSubmissionService).submit(1, 7, STUDENT_ID);
        verify(examAttemptRepository, never()).createAttempt(any());
    }

    @Test
    void startAttempt_refusesASecondAttemptAfterSubmission() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_UPCOMING, now.minusMinutes(5), now.plusHours(1).toLocalTime());
        ExamAttempt submitted = ExamAttempt.builder()
                .attemptId(7).expiresAt(now.plusMinutes(20)).status(ExamConstants.ATTEMPT_STATUS_SUBMITTED).build();
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);
        when(examAttemptRepository.findByExamIdAndStudentId(1, STUDENT_ID)).thenReturn(List.of(submitted));

        assertThatThrownBy(() -> examService.startAttempt(1, STUDENT_ID))
                .isInstanceOf(AttemptStartException.class)
                .hasMessage("This exam has already been submitted");
    }

    @Test
    void startAttempt_requiresARegistration() {
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now(), LocalTime.of(23, 59));
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(false);

        assertThatThrownBy(() -> examService.startAttempt(1, STUDENT_ID))
                .isInstanceOf(AttemptStartException.class)
                .hasMessage("Student is not registered for this exam");
    }

    @Test
    void startAttempt_refusesAnUnapprovedExam() {
        LocalDateTime now = LocalDateTime.now();
        Exam exam = examAt(1, ExamConstants.EXAM_STATUS_PENDING, now.minusMinutes(5), now.plusHours(1).toLocalTime());
        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);
        when(examAttemptRepository.findByExamIdAndStudentId(1, STUDENT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> examService.startAttempt(1, STUDENT_ID))
                .isInstanceOf(AttemptStartException.class)
                .hasMessage("Exam cannot be started at this time");
        verify(examAttemptRepository, never()).createAttempt(any());
    }

    @Test
    void startAttempt_throwsNotFoundForAMissingExam() {
        when(examRepository.getExamById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.startAttempt(99, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerStudentForExam_delegatesToRepository() {
        Exam future = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().plusDays(3), LocalTime.of(12, 0));
        when(examRepository.getExamById(2)).thenReturn(Optional.of(future));
        when(examRepository.registerStudentForExam(STUDENT_ID, 2)).thenReturn(true);

        examService.registerStudentForExam(STUDENT_ID, 2);

        verify(examRepository).registerStudentForExam(STUDENT_ID, 2);
    }

    @Test
    void registerStudentForExam_rejectsAnUnapprovedExam() {
        Exam pending = examAt(2, ExamConstants.EXAM_STATUS_PENDING, LocalDateTime.now().plusDays(3), LocalTime.of(12, 0));
        when(examRepository.getExamById(2)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> examService.registerStudentForExam(STUDENT_ID, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Registration is closed: this exam is not open for registration");
        verify(examRepository, never()).registerStudentForExam(anyInt(), anyInt());
    }

    @Test
    void registerStudentForExam_rejectsAnExamThatAlreadyStarted() {
        Exam live = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1).toLocalTime());
        when(examRepository.getExamById(2)).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> examService.registerStudentForExam(STUDENT_ID, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Registration closed: the exam has already started");
    }

    @Test
    void isStudentRegisteredForExam_delegatesToRepository() {
        when(examRepository.isStudentRegisteredForExam(STUDENT_ID, 2)).thenReturn(true);

        assertThat(examService.isStudentRegisteredForExam(STUDENT_ID, 2)).isTrue();
        verify(examRepository).isStudentRegisteredForExam(STUDENT_ID, 2);
    }

    @Test
    void getRegisteredExamIdsForStudent_delegatesToRepository() {
        when(examRepository.getRegisteredExamIdsForStudent(STUDENT_ID)).thenReturn(List.of(2, 3));

        assertThat(examService.getRegisteredExamIdsForStudent(STUDENT_ID)).containsExactly(2, 3);
    }

    @Test
    void getRegistrationsByStudentId_delegatesToRepository() {
        ExamRegistration reg = new ExamRegistration();
        when(examRepository.getRegistrationsByStudentId(STUDENT_ID)).thenReturn(List.of(reg));

        assertThat(examService.getRegistrationsByStudentId(STUDENT_ID)).containsExactly(reg);
    }

    @Test
    void getRegisteredStudentsByExamId_delegatesToRepository() {
        Student student = new Student();
        when(examRepository.getRegisteredStudentsByExamId(2)).thenReturn(List.of(student));

        assertThat(examService.getRegisteredStudentsByExamId(2)).containsExactly(student);
    }

    @Test
    void getRegistrationCountByExamId_delegatesToRepository() {
        when(examRepository.getRegistrationCountByExamId(2)).thenReturn(7);

        assertThat(examService.getRegistrationCountByExamId(2)).isEqualTo(7);
    }

    @Test
    void unregisterStudentFromExam_delegatesToRepository() {
        Exam future = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().plusDays(3), LocalTime.of(12, 0));
        when(examRepository.getExamById(2)).thenReturn(Optional.of(future));
        when(examRepository.unregisterStudentFromExam(STUDENT_ID, 2)).thenReturn(true);

        examService.unregisterStudentFromExam(STUDENT_ID, 2);

        verify(examRepository).unregisterStudentFromExam(STUDENT_ID, 2);
    }

    @Test
    void unregisterStudentFromExam_rejectsAStartedExam() {
        Exam live = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1).toLocalTime());
        when(examRepository.getExamById(2)).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> examService.unregisterStudentFromExam(STUDENT_ID, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("registrations can no longer be removed");
        verify(examRepository, never()).unregisterStudentFromExam(anyInt(), anyInt());
    }

    @Test
    void unregisterStudentFromExam_reportsAMissingRegistration() {
        Exam future = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().plusDays(3), LocalTime.of(12, 0));
        when(examRepository.getExamById(2)).thenReturn(Optional.of(future));
        when(examRepository.unregisterStudentFromExam(STUDENT_ID, 2)).thenReturn(false);

        assertThatThrownBy(() -> examService.unregisterStudentFromExam(STUDENT_ID, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Registration not found.");
    }

    @Test
    void unregisterStudentFromExam_failsClosed_whenTheExamScheduleIsUnreadable() {
        Exam broken = examAt(2, ExamConstants.EXAM_STATUS_UPCOMING, LocalDateTime.now().plusDays(3), LocalTime.of(12, 0));
        broken.setDate("not-a-date");
        when(examRepository.getExamById(2)).thenReturn(Optional.of(broken));

        assertThatThrownBy(() -> examService.unregisterStudentFromExam(STUDENT_ID, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("registrations can no longer be removed");
        verify(examRepository, never()).unregisterStudentFromExam(anyInt(), anyInt());
    }
}
