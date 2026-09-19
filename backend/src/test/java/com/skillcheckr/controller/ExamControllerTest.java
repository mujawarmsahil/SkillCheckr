package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.AttemptAnswerResponse;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.AttemptStartException;
import com.skillcheckr.service.AuthService;
import com.skillcheckr.service.AttemptAnswerService;
import com.skillcheckr.service.ExamSubmissionService;
import com.skillcheckr.service.QuestionService;

@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    @Mock
    private ExamService examService;

    @Mock
    private QuestionService questionService;

    @Mock
    private AuthService authService;

    @Mock
    private AttemptAnswerService attemptAnswerService;

    @Mock
    private ExamSubmissionService examSubmissionService;

    @InjectMocks
    private ExamController examController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(examController).build();
    }

    private Exam examWith(int id, String name, String status, String date) {
        Exam exam = new Exam();
        exam.setExamId(id);
        exam.setExamName(name);
        exam.setStatus(status);
        exam.setDate(date);
        return exam;
    }

    @Test
    void viewAllExams_returns404_whenNoExamsExist() throws Exception {
        when(examService.viewAllExams()).thenReturn(List.of());

        mockMvc.perform(get("/api/Exams/viewAllExams"))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewAllExams_returnsListOfExams_whenPresent() throws Exception {
        Exam maths = examWith(1, "Mathematics", "Pending", "2025-01-15T09:00:00");
        when(examService.viewAllExams()).thenReturn(List.of(maths));

        mockMvc.perform(get("/api/Exams/viewAllExams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_id").value(1))
                .andExpect(jsonPath("$[0].exam_name").value("Mathematics"))
                .andExpect(jsonPath("$[0].status").value("Pending"));
    }

    @Test
    void getExamById_returnsExam_whenFound() throws Exception {
        Exam exam = examWith(1, "Mathematics", "Upcoming", "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(exam);

        mockMvc.perform(get("/api/Exams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exam_id").value(1))
                .andExpect(jsonPath("$.exam_name").value("Mathematics"));
    }

    @Test
    void getExamById_returns404_whenNotFound() throws Exception {
        when(examService.getExamById(99)).thenReturn(null);

        mockMvc.perform(get("/api/Exams/99"))
                .andExpect(status().isNotFound());
    }

            @Test
            void startAttempt_returns201_forNewAttempt() throws Exception {
            ExamAttempt attempt = ExamAttempt.builder()
                .attemptId(1)
                .startedAt(LocalDateTime.of(2026, 9, 17, 18, 0))
                .expiresAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                .status("IN_PROGRESS")
                .build();
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(examService.startAttempt(1, 7)).thenReturn(new AttemptStartResult(attempt, false));

            mockMvc.perform(post("/api/exams/1/attempts")
                            .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").value(1))
                .andExpect(jsonPath("$.examId").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
            }

            @Test
            void startAttempt_returns200_forExistingActiveAttempt() throws Exception {
            ExamAttempt attempt = ExamAttempt.builder()
                .attemptId(2)
                .startedAt(LocalDateTime.of(2026, 9, 17, 18, 0))
                .expiresAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                .status("IN_PROGRESS")
                .build();
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(examService.startAttempt(1, 7)).thenReturn(new AttemptStartResult(attempt, true));

            mockMvc.perform(post("/api/exams/1/attempts")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(2));
            }

            @Test
            void startAttempt_returnsBusinessErrorResponse() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(examService.startAttempt(1, 7))
                .thenThrow(new AttemptStartException(403, "Student is not registered for this exam"));

            mockMvc.perform(post("/api/exams/1/attempts")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Student is not registered for this exam"));
            }

            @Test
            void saveAttemptAnswer_returns200WithAnswerResponse() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.saveAnswer(any(Integer.class), any(Integer.class), any(Integer.class),
                any(Integer.class), any()))
                .thenReturn(AttemptAnswerResponse.builder()
                    .attemptAnswerId(5).attemptId(1).questionId(10).selectedAnswerId(12).build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/exams/1/attempts/1/answers/10")
                    .header("Authorization", "Bearer jwt-mock-42-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"selectedAnswerId\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptAnswerId").value(5))
                .andExpect(jsonPath("$.selectedAnswerId").value(12));
            }

            @Test
            void saveAttemptAnswer_returnsStandardizedBusinessError() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.saveAnswer(any(Integer.class), any(Integer.class), any(Integer.class),
                any(Integer.class), any()))
                .thenThrow(new com.skillcheckr.service.AttemptAnswerException(409,
                    "Exam attempt has expired"));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/exams/1/attempts/1/answers/10")
                    .header("Authorization", "Bearer jwt-mock-42-1")
                            .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Exam attempt has expired"));
            }

            @Test
            void getAttemptAnswers_returnsPersistedAnswersForOwner() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.getAnswers(1, 2, 7)).thenReturn(List.of(
                AttemptAnswerResponse.builder().attemptAnswerId(5).attemptId(2).questionId(10)
                    .selectedAnswerId(12).build()));

            mockMvc.perform(get("/api/exams/1/attempts/2/answers")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptAnswerId").value(5))
                .andExpect(jsonPath("$[0].selectedAnswerId").value(12))
                .andExpect(jsonPath("$[0].is_correct").doesNotExist());
            }

            @Test
            void getAttemptAnswers_returnsNotFoundWhenAttemptIsMissing() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.getAnswers(1, 2, 7))
                .thenThrow(new com.skillcheckr.service.AttemptAnswerException(404, "Exam attempt not found"));

            mockMvc.perform(get("/api/exams/1/attempts/2/answers")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
            }

            @Test
            void getAttemptAnswers_rejectsUnauthorizedAttempt() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.getAnswers(1, 2, 7))
                .thenThrow(new com.skillcheckr.service.AttemptAnswerException(403,
                    "You are not authorized to view this attempt"));

            mockMvc.perform(get("/api/exams/1/attempts/2/answers")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
            }

            @Test
            void getAttemptAnswers_rejectsExamMismatch() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(attemptAnswerService.getAnswers(1, 2, 7))
                .thenThrow(new com.skillcheckr.service.AttemptAnswerException(409,
                    "Attempt does not belong to this exam"));

            mockMvc.perform(get("/api/exams/1/attempts/2/answers")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
            }

            @Test
            void submitAttempt_returnsResult() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(examSubmissionService.submit(1, 2, 7)).thenReturn(ExamResultDTO.builder()
                .resultId(4).attemptId(2).examId(1).studentId(7)
                .marksObtained(7).totalMarks(10).passingMarks(4)
                .percentage(70).status("Pass").build());

            mockMvc.perform(post("/api/exams/1/attempts/2/submit")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt_id").value(2))
                .andExpect(jsonPath("$.marks_obtained").value(7))
                .andExpect(jsonPath("$.status").value("Pass"));
            }

            @Test
            void submitAttempt_returnsStandardizedError() throws Exception {
            when(authService.getStudentIdFromAuthorization("Bearer jwt-mock-42-1")).thenReturn(7);
            when(examSubmissionService.submit(1, 2, 7))
                .thenThrow(new com.skillcheckr.service.ExamSubmissionException(403,
                    "You are not authorized to submit this attempt"));

            mockMvc.perform(post("/api/exams/1/attempts/2/submit")
                    .header("Authorization", "Bearer jwt-mock-42-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
            }

    @Test
    void getExamsByTeacherId_returnsList() throws Exception {
        Exam exam = examWith(1, "Physics", "Upcoming", "2030-01-15T09:00:00");
        when(examService.getExamsByTeacherId(10)).thenReturn(List.of(exam));

        mockMvc.perform(get("/api/Exams/teacher/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_id").value(1));
    }

    @Test
    void getQuestionsForExam_returnsQuestions() throws Exception {
        QuestionDTO q = new QuestionDTO();
        q.setQuestionId(1);
        q.setQuestion("Sample Q");
        when(questionService.getQuestionsByExamId(1)).thenReturn(List.of(q));

        mockMvc.perform(get("/api/Exams/1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(1));
    }

    @Test
    void deleteExam_returns200_whenDeleted() throws Exception {
        when(examService.deleteExamById(9)).thenReturn(true);

        mockMvc.perform(delete("/api/Exams/deleteExamById/9"))
                .andExpect(status().isOk())
                .andExpect(content().string("Exam deleted successfully."));
    }

    @Test
    void deleteExam_returns404_whenExamMissing() throws Exception {
        when(examService.deleteExamById(999)).thenReturn(false);

        mockMvc.perform(delete("/api/Exams/deleteExamById/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptExam_returns200_whenExamAccepted() throws Exception {
        when(examService.acceptExam(3)).thenReturn(true);

        mockMvc.perform(post("/api/Exams/upComingExamStatus/3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Accepted"));
    }

    @Test
    void acceptExam_returns404_whenExamNotFound() throws Exception {
        when(examService.acceptExam(3)).thenReturn(false);

        mockMvc.perform(post("/api/Exams/upComingExamStatus/3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewAllUpcomingExam_returnsList() throws Exception {
        when(examService.viewAllUpcomingExam())
                .thenReturn(List.of(examWith(4, "Physics", "Upcoming", "2030-02-01T10:00:00")));

        mockMvc.perform(get("/api/Exams/viewAllUpComingExam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_name").value("Physics"));
    }

    @Test
    void viewAllCompletedExam_returns404_whenEmpty() throws Exception {
        when(examService.viewAllCompletedExam()).thenReturn(List.of());

        mockMvc.perform(get("/api/Exams/viewAllCompletedExam"))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewAllCompletedExam_returnsList_whenPresent() throws Exception {
        when(examService.viewAllCompletedExam())
                .thenReturn(List.of(examWith(4, "Chemistry", "Completed", "2024-02-01T10:00:00")));

        mockMvc.perform(get("/api/Exams/viewAllCompletedExam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_name").value("Chemistry"));
    }

    @Test
    void addExams_returnsSubject_whenSaved() throws Exception {
        Subject subject = new Subject(1, "Mathematics", "MATH101");
        when(examService.saveExam(any(Exam.class))).thenReturn(subject);

        mockMvc.perform(post("/api/Exams/addExams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_name\":\"Maths\",\"date\":\"2030-01-15T09:00:00\","
                                + "\"start_time\":\"09:00\",\"end_time\":\"10:30\","
                                + "\"subject\":{\"subjectName\":\"Mathematics\",\"subjectCode\":\"MATH101\"},"
                                + "\"teacher_id\":1,\"duration_minutes\":90,\"total_marks\":100,\"passing_marks\":35}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(1))
                .andExpect(jsonPath("$.subjectName").value("Mathematics"));
    }

    @Test
    void addExams_returns500_whenServiceRejectsExam() throws Exception {
        when(examService.saveExam(any())).thenReturn(null);

        mockMvc.perform(post("/api/Exams/addExams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_name\":\"Maths\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void registerForExam_returnsBadRequest_whenIdsMissing() throws Exception {
        mockMvc.perform(post("/api/Exams/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registerForExam_returnsNotFound_whenExamDoesNotExist() throws Exception {
        when(examService.getExamById(99)).thenReturn(null);

        mockMvc.perform(post("/api/Exams/99/register/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Exam not found."));
    }

    @Test
    void registerForExam_returnsBadRequest_whenDeadlinePassed() throws Exception {
        Exam pastExam = examWith(10, "Past Exam", "Upcoming", "2020-01-01 09:00:00");
        when(examService.getExamById(10)).thenReturn(pastExam);

        mockMvc.perform(post("/api/Exams/10/register/5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Registration closed: The registration deadline for this examination has passed."));
    }

    @Test
    void registerForExam_returnsSuccess_whenRegistered() throws Exception {
        Exam futureExam = examWith(11, "Future Exam", "Upcoming", "2035-01-01 09:00:00");
        when(examService.getExamById(11)).thenReturn(futureExam);
        when(examService.registerStudentForExam(5, 11)).thenReturn(true);

        mockMvc.perform(post("/api/Exams/11/register/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void registerForExam_returns500_whenRegistrationFails() throws Exception {
        Exam futureExam = examWith(11, "Future Exam", "Upcoming", "2035-01-01 09:00:00");
        when(examService.getExamById(11)).thenReturn(futureExam);
        when(examService.registerStudentForExam(5, 11)).thenReturn(false);

        mockMvc.perform(post("/api/Exams/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":11,\"studentId\":5}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRegistrationsForStudent_returnsList() throws Exception {
        when(examService.getRegisteredExamIdsForStudent(5)).thenReturn(List.of(1, 2));

        mockMvc.perform(get("/api/Exams/registrations/student/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void getDetailedRegistrationsForStudent_returnsList() throws Exception {
        ExamRegistration reg = new ExamRegistration();
        reg.setRegistrationId(1);
        reg.setStudentId(5);
        when(examService.getRegistrationsByStudentId(5)).thenReturn(List.of(reg));

        mockMvc.perform(get("/api/Exams/registrations/student/5/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registration_id").value(1));
    }

    @Test
    void isRegistered_returnsStatus() throws Exception {
        when(examService.isStudentRegisteredForExam(5, 1)).thenReturn(true);

        mockMvc.perform(get("/api/Exams/1/isRegistered/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRegistered").value(true));
    }

    @Test
    void getRegisteredStudents_returnsList() throws Exception {
        Student s = new Student();
        s.setStudentId(5);
        when(examService.getRegisteredStudentsByExamId(1)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/Exams/1/registeredStudents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student_id").value(5));
    }

    @Test
    void getRegistrationCount_returnsCount() throws Exception {
        when(examService.getRegistrationCountByExamId(1)).thenReturn(42);

        mockMvc.perform(get("/api/Exams/1/registrationCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationCount").value(42));
    }

    @Test
    void unregisterStudent_returnsOk_whenSuccess() throws Exception {
        when(examService.unregisterStudentFromExam(5, 1)).thenReturn(true);

        mockMvc.perform(delete("/api/Exams/1/unregister/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unregisterStudent_returnsNotFound_whenFailed() throws Exception {
        when(examService.unregisterStudentFromExam(5, 1)).thenReturn(false);

        mockMvc.perform(delete("/api/Exams/1/unregister/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
