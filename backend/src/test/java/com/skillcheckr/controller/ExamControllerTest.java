package com.skillcheckr.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.ExamService;

@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    @Mock
    private ExamService examService;

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
    void deleteExam_returns200_whenDeleted() throws Exception {
        when(examService.deleteExamById(9)).thenReturn(true);

        mockMvc.perform(delete("/api/Exams/deleteExamById/9"))
                .andExpect(status().isOk());
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
                .andExpect(status().isOk());
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
                .thenReturn(List.of(examWith(4, "Physics", "Upcoming", "2025-02-01T10:00:00")));

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
    void addExams_returnsSubject_whenSaved() throws Exception {
        Subject subject = new Subject(1, "Mathematics", "MATH101");
        when(examService.saveExam(org.mockito.ArgumentMatchers.any(Exam.class))).thenReturn(subject);

        mockMvc.perform(post("/api/Exams/addExams")
                        .contentType("application/json")
                        .content("{\"exam_name\":\"Maths\",\"date\":\"2025-01-15T09:00:00\","
                                + "\"start_time\":\"09:00\",\"end_time\":\"10:30\","
                                + "\"subject\":{\"subjectName\":\"Mathematics\",\"subjectCode\":\"MATH101\"},"
                                + "\"teacher_id\":1,\"duration_minuets\":90,\"total_marks\":100,\"passing_marks\":35}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(1))
                .andExpect(jsonPath("$.subjectName").value("Mathematics"));
    }

    @Test
    void addExams_returns500_whenServiceRejectsExam() throws Exception {
        when(examService.saveExam(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        mockMvc.perform(post("/api/Exams/addExams")
                        .contentType("application/json")
                        .content("{\"exam_name\":\"Maths\"}"))
                .andExpect(status().isInternalServerError());
    }
}