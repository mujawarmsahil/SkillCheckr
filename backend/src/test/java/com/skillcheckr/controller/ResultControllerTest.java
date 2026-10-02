package com.skillcheckr.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.ResultService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class ResultControllerTest {

    private static final int STUDENT_ID = 2;
    private static final int OTHER_STUDENT_ID = 3;
    private static final int TEACHER_ID = 10;

    @Mock
    private ResultService resultService;

    @Mock
    private ExamService examService;

    @InjectMocks
    private ResultController resultController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resultController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    @Test
    void readEndpoints_rejectRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/results/all"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/results/student/" + STUDENT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacySubmitEndpointIsNoLongerExposed() throws Exception {
        mockMvc.perform(post("/api/results/submit")
                        .with(TestAuth.asStudent(STUDENT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":5,\"studentId\":" + STUDENT_ID + ",\"examType\":\"MCQ\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStudentExamStatus_returnsHasSubmittedTrue_whenResultExists() throws Exception {
        ExamResultDTO existing = ExamResultDTO.builder()
                .resultId(101)
                .examId(5)
                .studentId(STUDENT_ID)
                .build();
        when(resultService.getResultByExamAndStudent(5, STUDENT_ID)).thenReturn(Optional.of(existing));

        mockMvc.perform(get("/api/results/check/5/" + STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(true))
                .andExpect(jsonPath("$.result.result_id").value(101));
    }

    @Test
    void checkStudentExamStatus_returnsHasSubmittedFalse_whenNoResult() throws Exception {
        when(resultService.getResultByExamAndStudent(5, STUDENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/results/check/5/" + STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(false));
    }

    @Test
    void checkStudentExamStatus_rejectsAnotherStudent() throws Exception {
        mockMvc.perform(get("/api/results/check/5/" + OTHER_STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultByExamAndStudent(5, OTHER_STUDENT_ID);
    }

    @Test
    void getResultsByStudent_returnsList_whenPresent() throws Exception {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).studentId(STUDENT_ID).build();
        when(resultService.getResultsByStudentId(STUDENT_ID)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/results/student/" + STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result_id").value(1));
    }

    @Test
    void getResultsByStudent_returnsEmptyList_whenNull() throws Exception {
        when(resultService.getResultsByStudentId(STUDENT_ID)).thenReturn(null);

        mockMvc.perform(get("/api/results/student/" + STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getResultsByStudent_rejectsAnotherStudent() throws Exception {
        mockMvc.perform(get("/api/results/student/" + OTHER_STUDENT_ID).with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultsByStudentId(OTHER_STUDENT_ID);
    }

    @Test
    void checkStudentExamStatus_allowsTheTeacherWhoOwnsTheExam() throws Exception {
        Exam exam = new Exam();
        exam.setExamId(5);
        exam.setTeacherId(TEACHER_ID);
        when(examService.getExamById(5)).thenReturn(Optional.of(exam));
        when(resultService.getResultByExamAndStudent(5, OTHER_STUDENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/results/check/5/" + OTHER_STUDENT_ID).with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(false));
    }

    @Test
    void checkStudentExamStatus_refusesATeacherWhoDoesNotOwnTheExam() throws Exception {
        Exam exam = new Exam();
        exam.setExamId(5);
        exam.setTeacherId(99);
        when(examService.getExamById(5)).thenReturn(Optional.of(exam));

        mockMvc.perform(get("/api/results/check/5/" + OTHER_STUDENT_ID).with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultByExamAndStudent(5, OTHER_STUDENT_ID);
    }

    @Test
    void getResultsByStudent_refusesATeacher() throws Exception {
        mockMvc.perform(get("/api/results/student/" + OTHER_STUDENT_ID).with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultsByStudentId(OTHER_STUDENT_ID);
    }

    @Test
    void getResultsByStudent_refusesTeacherWhenRoleIdsCollide() throws Exception {
        mockMvc.perform(get("/api/results/student/" + STUDENT_ID)
                        .with(TestAuth.asTeacherAccount(99, STUDENT_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultsByStudentId(STUDENT_ID);
    }

    @Test
    void checkStudentExamStatus_requiresTeacherToOwnExamWhenRoleIdsCollide() throws Exception {
        Exam exam = new Exam();
        exam.setExamId(5);
        exam.setTeacherId(99);
        when(examService.getExamById(5)).thenReturn(Optional.of(exam));

        mockMvc.perform(get("/api/results/check/5/" + TEACHER_ID)
                        .with(TestAuth.asTeacherAccount(99, TEACHER_ID)))
                .andExpect(status().isForbidden());
        verify(resultService, never()).getResultByExamAndStudent(5, TEACHER_ID);
    }

    @Test
    void getResultsByStudent_allowsAnAdmin() throws Exception {
        when(resultService.getResultsByStudentId(OTHER_STUDENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/results/student/" + OTHER_STUDENT_ID).with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllResults_isAdminOnly() throws Exception {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultService.getAllResults()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/results/all").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result_id").value(1));

        mockMvc.perform(get("/api/results/all").with(TestAuth.asTeacher(TEACHER_ID)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/results/all").with(TestAuth.asStudent(STUDENT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllResults_returnsEmptyList_whenNull() throws Exception {
        when(resultService.getAllResults()).thenReturn(null);

        mockMvc.perform(get("/api/results/all").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
