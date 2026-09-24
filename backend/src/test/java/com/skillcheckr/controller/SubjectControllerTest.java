package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.SubjectService;

@ExtendWith(MockitoExtension.class)
class SubjectControllerTest {

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private SubjectController subjectController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subjectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllSubjects_returnsList() throws Exception {
        Subject s = new Subject(1, "Computer Science", "CS101");
        when(subjectService.getAllSubjects()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject_id").value(1))
                .andExpect(jsonPath("$[0].subject_name").value("Computer Science"));
    }

    @Test
    void addSubject_returnsCreated() throws Exception {
        Subject s = new Subject(10, "Physics", "PHY101");
        when(subjectService.addSubject(any())).thenReturn(s);

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Physics\",\"subject_code\":\"PHY101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject_name").value("Physics"));
    }

    @Test
    void updateSubject_returnsUpdatedSubject() throws Exception {
        Subject s = new Subject(10, "Advanced Physics", "PHY102");
        when(subjectService.updateSubject(eq(10), any())).thenReturn(true);
        when(subjectService.getSubjectById(10)).thenReturn(Optional.of(s));

        mockMvc.perform(put("/api/subjects/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Advanced Physics\",\"subject_code\":\"PHY102\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject_name").value("Advanced Physics"));
    }

    @Test
    void deleteSubject_returnsOk_whenFound() throws Exception {
        when(subjectService.deleteSubjectById(10)).thenReturn(true);

        mockMvc.perform(delete("/api/subjects/10"))
                .andExpect(status().isOk());
    }
}
