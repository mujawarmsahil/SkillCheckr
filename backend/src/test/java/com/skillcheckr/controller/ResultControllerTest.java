package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.service.ResultService;

@ExtendWith(MockitoExtension.class)
class ResultControllerTest {

    @Mock
    private ResultService resultService;

    @InjectMocks
    private ResultController resultController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resultController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitExam_returnsBadRequest_whenExamIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/results/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid submission data: exam ID is required"));
    }

    @Test
    void submitExam_returnsExistingResult_whenAlreadySubmitted() throws Exception {
        ExamResultDTO existing = ExamResultDTO.builder()
                .resultId(101)
                .examId(5)
                .studentId(2)
                .marksObtained(80)
                .build();
        when(resultService.getResultByExamAndStudent(5, 2)).thenReturn(existing);

        mockMvc.perform(post("/api/results/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":5,\"studentId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_id").value(101))
                .andExpect(jsonPath("$.marks_obtained").value(80));
    }

    @Test
    void submitExam_returnsNewResult_whenSubmissionSucceeds() throws Exception {
        when(resultService.getResultByExamAndStudent(5, 2)).thenReturn(null);
        ExamResultDTO submitted = ExamResultDTO.builder()
                .resultId(102)
                .examId(5)
                .studentId(2)
                .marksObtained(90)
                .status("Pass")
                .build();
        when(resultService.submitExam(any(ExamSubmissionDTO.class))).thenReturn(submitted);

        mockMvc.perform(post("/api/results/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":5,\"studentId\":2,\"examType\":\"MCQ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_id").value(102))
                .andExpect(jsonPath("$.status").value("Pass"));
    }

    @Test
    void submitExam_returnsInternalServerError_whenServiceThrows() throws Exception {
        when(resultService.getResultByExamAndStudent(5, 2)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/results/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examId\":5,\"studentId\":2}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("DB error"));
    }

    @Test
    void checkStudentExamStatus_returnsHasSubmittedTrue_whenResultExists() throws Exception {
        ExamResultDTO existing = ExamResultDTO.builder()
                .resultId(101)
                .examId(5)
                .studentId(2)
                .build();
        when(resultService.getResultByExamAndStudent(5, 2)).thenReturn(existing);

        mockMvc.perform(get("/api/results/check/5/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(true))
                .andExpect(jsonPath("$.result.result_id").value(101));
    }

    @Test
    void checkStudentExamStatus_returnsHasSubmittedFalse_whenNoResult() throws Exception {
        when(resultService.getResultByExamAndStudent(5, 2)).thenReturn(null);

        mockMvc.perform(get("/api/results/check/5/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(false));
    }

    @Test
    void checkStudentExamStatus_returnsHasSubmittedFalse_whenExceptionThrown() throws Exception {
        when(resultService.getResultByExamAndStudent(5, 2)).thenThrow(new RuntimeException("Lookup error"));

        mockMvc.perform(get("/api/results/check/5/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasSubmitted").value(false));
    }

    @Test
    void getResultsByStudent_returnsList_whenPresent() throws Exception {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).studentId(2).build();
        when(resultService.getResultsByStudentId(2)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/results/student/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result_id").value(1));
    }

    @Test
    void getResultsByStudent_returnsEmptyList_whenNull() throws Exception {
        when(resultService.getResultsByStudentId(2)).thenReturn(null);

        mockMvc.perform(get("/api/results/student/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllResults_returnsList_whenPresent() throws Exception {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultService.getAllResults()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/results/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result_id").value(1));
    }

    @Test
    void getAllResults_returnsEmptyList_whenNull() throws Exception {
        when(resultService.getAllResults()).thenReturn(null);

        mockMvc.perform(get("/api/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
