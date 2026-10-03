package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptAnswerException;
import com.skillcheckr.exception.AttemptStartException;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.AttemptAnswerResponse;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.EvaluationItemDTO;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.model.QuestionBankDocument;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Student;
import com.skillcheckr.service.AttemptAnswerService;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.ExamSubmissionService;
import com.skillcheckr.service.QuestionBankService;
import com.skillcheckr.service.QuestionService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    private static final int STUDENT_ID = 7;
    private static final int OTHER_STUDENT_ID = 8;
    private static final int TEACHER_ID = 10;
    private static final int OTHER_TEACHER_ID = 11;

    @Mock
    private ExamService examService;

    @Mock
    private QuestionService questionService;

    @Mock
    private AttemptAnswerService attemptAnswerService;

    @Mock
    private ExamSubmissionService examSubmissionService;

    @Mock
    private QuestionBankService questionBankService;

    @InjectMocks
    private ExamController examController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup supplies its own message converter, so it would otherwise bind JSON
        // with Jackson's defaults rather than with the settings application.properties applies.
        // Mirroring the one setting that matters here keeps these tests honest about how a
        // fractional mark is treated. JacksonNumberBindingTest checks the real context.
        // The builder is what Spring Boot itself uses, so the Java time module that
        // LocalDateTime/OffsetDateTime fields need is registered exactly as in the running app.
        ObjectMapper strictMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(examController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                // The string converter stays ahead of Jackson so endpoints that return a bare
                // String body keep returning plain text rather than a JSON string.
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(strictMapper))
                .build();
    }

    private Exam examWith(int id, String name, String status, String date) {
        Exam exam = new Exam();
        exam.setExamId(id);
        exam.setExamName(name);
        exam.setStatus(status);
        exam.setDate(date);
        return exam;
    }

    private Exam ownedExam(int id) {
        Exam exam = examWith(id, "Physics", ExamConstants.EXAM_STATUS_UPCOMING, "2030-01-15T09:00:00");
        exam.setTeacherId(TEACHER_ID);
        return exam;
    }

    @Test
    void protectedEndpoints_rejectRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/exams/1/attempts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/exams/1/question-bank"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoints_rejectATamperedToken() throws Exception {
        String valid = TestAuth.tokenFor(STUDENT_ID, STUDENT_ID, "Student");
        String tampered = valid.substring(0, valid.length() - 2) + "xy";

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.withToken("Bearer " + tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoints_rejectTheLegacyMockToken() throws Exception {
        mockMvc.perform(post("/api/exams/1/attempts")
                        .with(TestAuth.withToken("Bearer jwt-mock-42-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllExams_returns404_whenNoExamsExist() throws Exception {
        when(examService.getAllExams()).thenReturn(List.of());

        mockMvc.perform(get("/api/exams").with(TestAuth.asAdmin(1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllExams_isNotAvailableToStudents() throws Exception {
        mockMvc.perform(get("/api/exams").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllExams_returnsListOfExams_whenPresent() throws Exception {
        Exam maths = examWith(1, "Mathematics", "Pending", "2025-01-15T09:00:00");
        when(examService.getAllExams()).thenReturn(List.of(maths));

        mockMvc.perform(get("/api/exams").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_id").value(1))
                .andExpect(jsonPath("$[0].exam_name").value("Mathematics"))
                .andExpect(jsonPath("$[0].status").value("Pending"));
    }

    @Test
    void getExamById_returnsExam_whenFound() throws Exception {
        Exam exam = examWith(1, "Mathematics", "Upcoming", "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(Optional.of(exam));

        mockMvc.perform(get("/api/exams/1").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exam_id").value(1))
                .andExpect(jsonPath("$.exam_name").value("Mathematics"));
    }

    @Test
    void getExamById_returns404_whenNotFound() throws Exception {
        when(examService.getExamById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/exams/99").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    private ExamAttempt attemptExpiringAt(LocalDateTime expiresAt) {
        return ExamAttempt.builder()
                .attemptId(1)
                .startedAt(LocalDateTime.of(2026, 9, 17, 18, 0))
                .expiresAt(expiresAt)
                .status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS)
                .build();
    }

    @Test
    void startAttempt_returns201_forNewAttempt() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenReturn(new AttemptStartResult(attemptExpiringAt(LocalDateTime.of(2026, 9, 17, 19, 0)), false));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").value(1))
                .andExpect(jsonPath("$.examId").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startAttempt_serializesExpiresAtWithAnExplicitOffset() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenReturn(new AttemptStartResult(attemptExpiringAt(LocalDateTime.of(2026, 9, 17, 19, 0)), false));

        // Jackson always writes the seconds field; OffsetDateTime.toString() would drop it when it
        // is zero, so the expectation is formatted the same way rather than via toString().
        String expected = LocalDateTime.of(2026, 9, 17, 19, 0)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value(expected));
    }

    @Test
    void startAttempt_expiresAtIsUnambiguousRegardlessOfTheReaderTimezone() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 9, 17, 19, 0);
        when(examService.startAttempt(1, STUDENT_ID))
                .thenReturn(new AttemptStartResult(attemptExpiringAt(expiresAt), false));

        MvcResult result = mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        String serialized = node.get("expiresAt").asText();

        // The value must be parseable as offset date-time, not as a bare local date-time. A bare
        // value would be read as reader-local time and would name a different instant.
        OffsetDateTime parsed = OffsetDateTime.parse(serialized);
        assertEquals(
                expiresAt.atZone(ZoneId.systemDefault()).toOffsetDateTime().toInstant(),
                parsed.toInstant(),
                "serialized expiresAt must denote the instant the server enforces");
    }

    @Test
    void startAttempt_returns400_not500_forANonNumericQuestionId() throws Exception {
        // The path variable is typed, so Spring rejects it before the handler runs. The client must
        // see a validation failure, never a server error.
        mockMvc.perform(put("/api/exams/1/attempts/2/answers/not-a-number")
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid parameter value"));
    }

    @Test
    void startAttempt_returns400_whenTheServiceRejectsAnInvalidAttemptId() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenThrow(new AttemptStartException(400, "Invalid examId"));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid examId"));
    }

    @Test
    void startAttempt_hidesTheCauseOfAnUnexpectedFailure() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenThrow(new DataAccessResourceFailureException(
                        "Connection refused to db-primary:5432 (user=exam_app, database=exam_system)"));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("db-primary"))));
    }

    @Test
    void attachQuestions_returns400_forANonNumericQuestionId() throws Exception {
        givenAccessibleExam();

        mockMvc.perform(post("/api/exams/1/questions")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"abc\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("questionIds must be a valid number"));
    }

    @Test
    void awardMarks_returns400_forNonNumericMarks() throws Exception {
        givenAccessibleExam();

        mockMvc.perform(put("/api/exams/1/attempts/2/answers/3/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("marks must be a valid number"));
    }

    /** Stubs the exam lookup that AuthGuard.requireExamAccess performs for staff-only endpoints. */
    private void givenAccessibleExam() {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
    }

    @Test
    void addExams_rejectsFractionalTotalMarksWithoutTruncating() throws Exception {
        // `total_marks` is an INT column and `Exam.totalMarks` is an int field, so a fractional
        // value has to be refused at binding time rather than quietly rounded on the way in.
        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson().replace("\"total_marks\":100", "\"total_marks\":100.5")))
                .andExpect(status().isBadRequest());
        verify(examService, never()).saveExam(any(), anyInt());
    }

    @Test
    void addExams_rejectsFractionalPassingMarksWithoutTruncating() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson().replace("\"passing_marks\":35", "\"passing_marks\":34.5")))
                .andExpect(status().isBadRequest());
        verify(examService, never()).saveExam(any(), anyInt());
    }

    @Test
    void addExams_stillAcceptsWholeMarks() throws Exception {
        when(examService.saveExam(any(Exam.class), anyInt()))
                .thenReturn(examWith(12, "Maths", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00"));

        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson()))
                .andExpect(status().isCreated());
    }

    private String examJson() {
        return "{\"exam_name\":\"Maths\",\"date\":\"2030-01-15T09:00:00\","
                + "\"start_time\":\"09:00\",\"end_time\":\"10:30\","
                + "\"subject\":{\"subjectName\":\"Mathematics\",\"subjectCode\":\"MATH101\"},"
                + "\"duration_minutes\":90,\"total_marks\":100,\"passing_marks\":35}";
    }

    @Test
    void startAttempt_serializesNullExpiresAtAsNull() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenReturn(new AttemptStartResult(attemptExpiringAt(null), false));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    void startAttempt_returns200_forExistingActiveAttempt() throws Exception {
        ExamAttempt attempt = ExamAttempt.builder()
                .attemptId(2)
                .startedAt(LocalDateTime.of(2026, 9, 17, 18, 0))
                .expiresAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                .status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS)
                .build();
        when(examService.startAttempt(1, STUDENT_ID)).thenReturn(new AttemptStartResult(attempt, true));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(2));
    }

    @Test
    void startAttempt_usesTheStudentFromTheToken_andNeverTheRequestBody() throws Exception {
        ExamAttempt attempt = ExamAttempt.builder()
                .attemptId(3)
                .expiresAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                .build();
        when(examService.startAttempt(1, STUDENT_ID)).thenReturn(new AttemptStartResult(attempt, false));

        mockMvc.perform(post("/api/exams/1/attempts")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + OTHER_STUDENT_ID + "}"))
                .andExpect(status().isCreated());
        verify(examService).startAttempt(1, STUDENT_ID);
    }

    @Test
    void startAttempt_isNotAvailableToTeachers() throws Exception {
        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startAttempt_returnsBusinessErrorResponse() throws Exception {
        when(examService.startAttempt(1, STUDENT_ID))
                .thenThrow(new AttemptStartException(403, "Student is not registered for this exam"));

        mockMvc.perform(post("/api/exams/1/attempts").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Student is not registered for this exam"));
    }

    @Test
    void saveAttemptAnswer_returns200WithAnswerResponse() throws Exception {
        when(attemptAnswerService.saveAnswer(anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(AttemptAnswerResponse.builder()
                        .attemptAnswerId(5).attemptId(1).questionId(10).selectedAnswerId(12).build());

        mockMvc.perform(put("/api/exams/1/attempts/1/answers/10")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedAnswerId\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptAnswerId").value(5))
                .andExpect(jsonPath("$.selectedAnswerId").value(12));
    }

    @Test
    void saveAttemptAnswer_returnsStandardizedBusinessError() throws Exception {
        when(attemptAnswerService.saveAnswer(anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenThrow(new AttemptAnswerException(409, "Exam attempt has expired"));

        mockMvc.perform(put("/api/exams/1/attempts/1/answers/10")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Exam attempt has expired"));
    }

    @Test
    void getAttemptAnswers_returnsPersistedAnswersForOwner() throws Exception {
        when(attemptAnswerService.getAnswers(1, 2, STUDENT_ID)).thenReturn(List.of(
                AttemptAnswerResponse.builder().attemptAnswerId(5).attemptId(2).questionId(10)
                        .selectedAnswerId(12).build()));

        mockMvc.perform(get("/api/exams/1/attempts/2/answers").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptAnswerId").value(5))
                .andExpect(jsonPath("$[0].selectedAnswerId").value(12))
                .andExpect(jsonPath("$[0].is_correct").doesNotExist());
    }

    @Test
    void getAttemptAnswers_returnsNotFoundWhenAttemptIsMissing() throws Exception {
        when(attemptAnswerService.getAnswers(1, 2, STUDENT_ID))
                .thenThrow(new AttemptAnswerException(404, "Exam attempt not found"));

        mockMvc.perform(get("/api/exams/1/attempts/2/answers").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAttemptAnswers_rejectsUnauthorizedAttempt() throws Exception {
        when(attemptAnswerService.getAnswers(1, 2, STUDENT_ID))
                .thenThrow(new AttemptAnswerException(403, "You are not authorized to view this attempt"));

        mockMvc.perform(get("/api/exams/1/attempts/2/answers").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAttemptAnswers_rejectsExamMismatch() throws Exception {
        when(attemptAnswerService.getAnswers(1, 2, STUDENT_ID))
                .thenThrow(new AttemptAnswerException(409, "Attempt does not belong to this exam"));

        mockMvc.perform(get("/api/exams/1/attempts/2/answers").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void submitAttempt_returnsResult() throws Exception {
        when(examSubmissionService.submit(1, 2, STUDENT_ID)).thenReturn(ExamResultDTO.builder()
                .resultId(4).attemptId(2).examId(1).studentId(STUDENT_ID)
                .marksObtained(7).totalMarks(10).passingMarks(4)
                .percentage(70).status(ExamConstants.RESULT_STATUS_PASS).build());

        mockMvc.perform(post("/api/exams/1/attempts/2/submit").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attempt_id").value(2))
                .andExpect(jsonPath("$.marks_obtained").value(7))
                .andExpect(jsonPath("$.status").value("Pass"));
    }

    @Test
    void submitAttempt_returnsStandardizedError() throws Exception {
        when(examSubmissionService.submit(1, 2, STUDENT_ID))
                .thenThrow(new ExamSubmissionException(403, "You are not authorized to submit this attempt"));

        mockMvc.perform(post("/api/exams/1/attempts/2/submit").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getExamsByTeacherId_returnsOwnList() throws Exception {
        when(examService.getExamsByTeacherId(TEACHER_ID)).thenReturn(List.of(ownedExam(1)));

        mockMvc.perform(get("/api/exams/teacher/" + TEACHER_ID).with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_id").value(1));
    }

    @Test
    void getExamsByTeacherId_rejectsAnotherTeacher() throws Exception {
        mockMvc.perform(get("/api/exams/teacher/" + OTHER_TEACHER_ID).with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQuestionsForExam_hidesTheAnswerKeyFromStudents() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(questionService.getStudentQuestionsByExamId(1)).thenReturn(List.of(QuestionDTO.builder()
                .questionId(1).question("Sample Q").option1("A").option2("B").marks(2).build()));

        mockMvc.perform(get("/api/exams/1/questions").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(1))
                .andExpect(jsonPath("$[0].correct_option").doesNotExist())
                .andExpect(jsonPath("$[0].sample_answer").doesNotExist());
        verify(questionService, never()).getQuestionsByExamId(1);
    }

    @Test
    void getQuestionsForExam_hidesPendingExamQuestionsFromStudents() throws Exception {
        Exam pendingExam = examWith(1, "Pending exam", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(Optional.of(pendingExam));

        mockMvc.perform(get("/api/exams/1/questions").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
        verify(questionService, never()).getStudentQuestionsByExamId(1);
    }

    @Test
    void getExamById_hidesPendingExamFromStudents() throws Exception {
        Exam pendingExam = examWith(1, "Pending exam", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(Optional.of(pendingExam));

        mockMvc.perform(get("/api/exams/1").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadQuestionBank_returnsQuestionPdfForAnApprovedExam() throws Exception {
        Exam approvedExam = examWith(1, "Approved exam", ExamConstants.EXAM_STATUS_UPCOMING, "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(Optional.of(approvedExam));
        when(questionBankService.getStudentDocument(approvedExam))
                .thenReturn(new QuestionBankDocument("questions.pdf", new byte[] { 1, 2, 3 }));

        mockMvc.perform(get("/api/exams/1/question-bank").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    void downloadQuestionBank_hidesPendingExamFromStudents() throws Exception {
        Exam pendingExam = examWith(1, "Pending exam", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00");
        when(examService.getExamById(1)).thenReturn(Optional.of(pendingExam));

        mockMvc.perform(get("/api/exams/1/question-bank").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
        verify(questionBankService, never()).getStudentDocument(any());
    }

    @Test
    void uploadQuestionBank_acceptsTheOwningTeachersPdfForPendingExam() throws Exception {
        Exam pendingExam = examWith(1, "Pending exam", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00");
        pendingExam.setTeacherId(TEACHER_ID);
        when(examService.getExamById(1)).thenReturn(Optional.of(pendingExam));
        MockMultipartFile file = new MockMultipartFile("file", "questions.pdf", "application/pdf",
                "%PDF-1.4\n".getBytes());
        MockMultipartHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/api/exams/1/question-bank");

        mockMvc.perform(request.file(file)
                        .param("questionOnlyConfirmed", "true")
                        .with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(questionBankService).saveUploadedPdf(1, "questions.pdf", file.getBytes(), true);
    }

    @Test
    void uploadQuestionBank_rejectsAnotherTeachersExam() throws Exception {
        Exam pendingExam = examWith(1, "Pending exam", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00");
        pendingExam.setTeacherId(TEACHER_ID);
        when(examService.getExamById(1)).thenReturn(Optional.of(pendingExam));
        MockMultipartFile file = new MockMultipartFile("file", "questions.pdf", "application/pdf",
                "%PDF-1.4\n".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/exams/1/question-bank")
                        .file(file)
                        .param("questionOnlyConfirmed", "true")
                        .with(TestAuth.asTeacher(OTHER_TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(questionBankService, never()).saveUploadedPdf(anyInt(), any(), any(), anyBoolean());
    }

    @Test
    void getQuestionsForExam_includesTheAnswerKeyForTheOwningTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(questionService.getQuestionsByExamId(1)).thenReturn(List.of(QuestionDTO.builder()
                .questionId(1).question("Sample Q").correctOption("B").build()));

        mockMvc.perform(get("/api/exams/1/questions").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correct_option").value("B"));
    }

    @Test
    void attachExistingQuestions_delegatesToTheService() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(questionService.attachQuestionsToExam(1, List.of(4, 5))).thenReturn(2);

        mockMvc.perform(post("/api/exams/1/questions")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[4,5]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void attachExistingQuestions_rejectsAnotherTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(post("/api/exams/1/questions")
                        .with(TestAuth.asTeacher(OTHER_TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[4]}"))
                .andExpect(status().isForbidden());
        verify(questionService, never()).attachQuestionsToExam(anyInt(), any());
    }

    @Test
    void deleteExam_returns200_whenDeleted() throws Exception {
        when(examService.getExamById(9)).thenReturn(Optional.of(ownedExam(9)));
        when(examService.deleteExamById(9)).thenReturn(true);

        mockMvc.perform(delete("/api/exams/9").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string("Exam deleted successfully."));
    }

    @Test
    void deleteExam_rejectsAnotherTeacher() throws Exception {
        when(examService.getExamById(9)).thenReturn(Optional.of(ownedExam(9)));

        mockMvc.perform(delete("/api/exams/9").with(TestAuth.asTeacher(OTHER_TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(examService, never()).deleteExamById(anyInt());
    }

    @Test
    void deleteExam_returns404_whenExamMissing() throws Exception {
        when(examService.getExamById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/exams/999").with(TestAuth.asAdmin(1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptExam_returns200_whenExamAccepted() throws Exception {
        when(examService.acceptExam(3)).thenReturn(true);

        mockMvc.perform(post("/api/exams/3/approve").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(content().string("Accepted"));
    }

    @Test
    void acceptExam_isAdminOnly() throws Exception {
        mockMvc.perform(post("/api/exams/3/approve").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(examService, never()).acceptExam(anyInt());
    }

    @Test
    void acceptExam_returns404_whenExamNotFound() throws Exception {
        when(examService.acceptExam(3)).thenReturn(false);

        mockMvc.perform(post("/api/exams/3/approve").with(TestAuth.asAdmin(1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectAndCancelExam_areAdminOnly() throws Exception {
        mockMvc.perform(post("/api/exams/3/reject").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/exams/3/cancel").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUpcomingExams_returnsList() throws Exception {
        when(examService.getAllUpcomingExams())
                .thenReturn(List.of(examWith(4, "Physics", "Upcoming", "2030-02-01T10:00:00")));

        mockMvc.perform(get("/api/exams/upcoming").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_name").value("Physics"));
    }

    @Test
    void getAllCompletedExams_returns404_whenEmpty() throws Exception {
        when(examService.getAllCompletedExams()).thenReturn(List.of());

        mockMvc.perform(get("/api/exams/completed").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCompletedExams_returnsList_whenPresent() throws Exception {
        when(examService.getAllCompletedExams())
                .thenReturn(List.of(examWith(4, "Chemistry", "Completed", "2024-02-01T10:00:00")));

        mockMvc.perform(get("/api/exams/completed").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exam_name").value("Chemistry"));
    }

    @Test
    void addExams_createsTheExamForTheAuthenticatedTeacher() throws Exception {
        when(examService.saveExam(any(Exam.class), anyInt()))
                .thenReturn(examWith(12, "Maths", ExamConstants.EXAM_STATUS_PENDING, "2030-01-15T09:00:00"));

        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_name\":\"Maths\",\"date\":\"2030-01-15T09:00:00\","
                                + "\"start_time\":\"09:00\",\"end_time\":\"10:30\","
                                + "\"subject\":{\"subjectName\":\"Mathematics\",\"subjectCode\":\"MATH101\"},"
                                + "\"teacher_id\":" + OTHER_TEACHER_ID + ",\"duration_minutes\":90,"
                                + "\"total_marks\":100,\"passing_marks\":35}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exam_id").value(12))
                .andExpect(jsonPath("$.status").value(ExamConstants.EXAM_STATUS_PENDING));
    }

    @Test
    void addExams_isNotAvailableToStudents() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_name\":\"Maths\"}"))
                .andExpect(status().isForbidden());
        verify(examService, never()).saveExam(any(), anyInt());
    }

    @Test
    void addExams_returnsServiceError() throws Exception {
        when(examService.saveExam(any(Exam.class), anyInt()))
                .thenThrow(new BadRequestException("Exam name may only contain letters, single spaces and hyphens"));

        mockMvc.perform(post("/api/exams")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exam_name\":\"Maths\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerForExam_usesTheStudentFromTheToken() throws Exception {
        Exam futureExam = examWith(11, "Future Exam", "Upcoming", "2035-01-01 09:00:00");
        when(examService.getExamById(11)).thenReturn(Optional.of(futureExam));

        mockMvc.perform(post("/api/exams/11/register")
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(examService).registerStudentForExam(STUDENT_ID, 11);
    }

    @Test
    void registerForExam_returnsNotFound_whenExamDoesNotExist() throws Exception {
        when(examService.getExamById(99)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/exams/99/register").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerForExam_returnsBadRequest_whenTheExamAlreadyStarted() throws Exception {
        Exam pastExam = examWith(10, "Past Exam", "Upcoming", "2020-01-01 09:00:00");
        when(examService.getExamById(10)).thenReturn(Optional.of(pastExam));
        org.mockito.Mockito.doThrow(new BadRequestException("Registration closed: the exam has already started"))
                .when(examService).registerStudentForExam(STUDENT_ID, 10);

        mockMvc.perform(post("/api/exams/10/register").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Registration closed: the exam has already started"));
    }

    @Test
    void getRegistrationsForStudent_returnsOwnList() throws Exception {
        when(examService.getRegisteredExamIdsForStudent(STUDENT_ID)).thenReturn(List.of(1, 2));

        mockMvc.perform(get("/api/exams/registrations/student/" + STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void getRegistrationsForStudent_rejectsAnotherStudent() throws Exception {
        mockMvc.perform(get("/api/exams/registrations/student/" + OTHER_STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(examService, never()).getRegisteredExamIdsForStudent(anyInt());
    }

    @Test
    void getDetailedRegistrationsForStudent_returnsList() throws Exception {
        ExamRegistration reg = new ExamRegistration();
        reg.setRegistrationId(1);
        reg.setStudentId(STUDENT_ID);
        when(examService.getRegistrationsByStudentId(STUDENT_ID)).thenReturn(List.of(reg));

        mockMvc.perform(get("/api/exams/registrations/student/" + STUDENT_ID + "/detailed")
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registration_id").value(1));
    }

    @Test
    void isRegistered_returnsStatus() throws Exception {
        when(examService.isStudentRegisteredForExam(STUDENT_ID, 1)).thenReturn(true);

        mockMvc.perform(get("/api/exams/1/isRegistered/" + STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRegistered").value(true));
    }

    @Test
    void isRegistered_rejectsAnotherStudent() throws Exception {
        mockMvc.perform(get("/api/exams/1/isRegistered/" + OTHER_STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRegisteredStudents_isLimitedToTheOwningTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        Student s = new Student();
        s.setStudentId(STUDENT_ID);
        when(examService.getRegisteredStudentsByExamId(1)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/exams/1/registeredStudents").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student_id").value(STUDENT_ID));
    }

    @Test
    void getRegisteredStudents_rejectsAnotherTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(get("/api/exams/1/registeredStudents").with(TestAuth.asTeacher(OTHER_TEACHER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRegistrationCount_returnsCount() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(examService.getRegistrationCountByExamId(1)).thenReturn(42);

        mockMvc.perform(get("/api/exams/1/registrationCount").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationCount").value(42));
    }

    @Test
    void unregisterStudent_usesTheStudentFromTheToken() throws Exception {
        mockMvc.perform(delete("/api/exams/1/unregister/" + STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(examService).unregisterStudentFromExam(STUDENT_ID, 1);
    }

    @Test
    void unregisterStudent_rejectsAnotherStudent() throws Exception {
        mockMvc.perform(delete("/api/exams/1/unregister/" + OTHER_STUDENT_ID)
                        .with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(examService, never()).unregisterStudentFromExam(anyInt(), anyInt());
    }

    @Test
    void listSubmissions_isLimitedToTheOwningTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(examSubmissionService.listSubmissions(1)).thenReturn(List.of(ExamSubmissionSummaryDTO.builder()
                .attemptId(3).studentId(STUDENT_ID).resultStatus("Submitted for Evaluation")
                .pendingEvaluation(true).build()));

        mockMvc.perform(get("/api/exams/1/submissions").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attempt_id").value(3))
                .andExpect(jsonPath("$[0].pending_evaluation").value(true));
    }

    @Test
    void listSubmissions_isNotAvailableToStudents() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(get("/api/exams/1/submissions").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(examSubmissionService, never()).listSubmissions(anyInt());
    }

    @Test
    void getEvaluationItems_returnsTheSavedAnswers() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(examSubmissionService.getEvaluationItems(1, 3)).thenReturn(List.of(EvaluationItemDTO.builder()
                .questionId(9).marks(5).textAnswer("A long answer").requiresEvaluation(true).build()));

        mockMvc.perform(get("/api/exams/1/attempts/3/evaluation").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question_id").value(9))
                .andExpect(jsonPath("$[0].text_answer").value("A long answer"));
    }

    @Test
    void awardAnswerMarks_delegatesToTheService() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(examSubmissionService.awardAnswerMarks(1, 3, 9, 4)).thenReturn(ExamResultDTO.builder()
                .attemptId(3).marksObtained(9).status(ExamConstants.RESULT_STATUS_PASS).build());

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marks_obtained").value(9));
    }

    @Test
    void awardAnswerMarks_rejectsAnotherTeacher() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(OTHER_TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":4}"))
                .andExpect(status().isForbidden());
        verify(examSubmissionService, never()).awardAnswerMarks(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void awardAnswerMarks_rejectsAMissingMark() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void awardAnswerMarks_rejectsAHalfMarkInsteadOfRoundingItToOne() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":0.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("marks must be a whole number"));

        verify(examSubmissionService, never()).awardAnswerMarks(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void awardAnswerMarks_rejectsAQuarterMarkInsteadOfRoundingItDownToZero() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":0.25}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("marks must be a whole number"));

        verify(examSubmissionService, never()).awardAnswerMarks(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void awardAnswerMarks_rejectsAWholeNumberSentWithADecimalPoint() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));
        when(examSubmissionService.awardAnswerMarks(1, 3, 9, 4)).thenReturn(ExamResultDTO.builder()
                .attemptId(3).marksObtained(9).status(ExamConstants.RESULT_STATUS_PASS).build());

        // 4.0 is a whole number, so it is accepted rather than refused for its decimal point.
        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":4.0}"))
                .andExpect(status().isOk());
        verify(examSubmissionService).awardAnswerMarks(1, 3, 9, 4);
    }

    @Test
    void awardAnswerMarks_rejectsAStringHalfMark() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":\"0.5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("marks must be a whole number"));

        verify(examSubmissionService, never()).awardAnswerMarks(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void awardAnswerMarks_stillRejectsNonNumericText() throws Exception {
        when(examService.getExamById(1)).thenReturn(Optional.of(ownedExam(1)));

        mockMvc.perform(put("/api/exams/1/attempts/3/answers/9/marks")
                        .with(TestAuth.asTeacher(TEACHER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("marks must be a valid number"));
    }
}
