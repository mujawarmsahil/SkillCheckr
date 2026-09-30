package com.skillcheckr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.skillcheckr.model.SubjectStatsResponse;
import com.skillcheckr.service.SubjectService;
import com.skillcheckr.support.TestAuth;

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
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    @Test
    void subjectEndpoints_rejectRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllSubjects_returnsListToAnySignedInUser() throws Exception {
        Subject s = new Subject(1, "Computer Science", "CS101");
        when(subjectService.getAllSubjects()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/subjects").with(TestAuth.asStudent(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject_id").value(1))
                .andExpect(jsonPath("$[0].subject_name").value("Computer Science"));
    }

    @Test
    void getAllSubjectsWithStats_isStaffOnly() throws Exception {
        SubjectStatsResponse stats = SubjectStatsResponse.builder()
                .subjectId(1)
                .subjectName("Computer Science")
                .subjectCode("CS101")
                .questionCount(7)
                .examCount(2)
                .build();
        when(subjectService.getAllSubjectsWithStats()).thenReturn(List.of(stats));

        mockMvc.perform(get("/api/subjects/with-stats").with(TestAuth.asTeacher(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectId").value(1))
                .andExpect(jsonPath("$[0].questionCount").value(7))
                .andExpect(jsonPath("$[0].examCount").value(2));

        mockMvc.perform(get("/api/subjects/with-stats").with(TestAuth.asStudent(7)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSubjectById_returns404_whenMissing() throws Exception {
        when(subjectService.getSubjectById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/subjects/9").with(TestAuth.asStudent(7)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSubject_returnsCreatedAndNormalisesTheCode() throws Exception {
        when(subjectService.addSubject(any())).thenReturn(new Subject(10, "Physics", "PHY101"));

        mockMvc.perform(post("/api/subjects")
                        .with(TestAuth.asAdmin(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\" Physics \",\"subject_code\":\" phy101 \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject_code").value("PHY101"));

        verify(subjectService).addSubject(argThat(subject -> "Physics".equals(subject.getSubjectName())
                && "PHY101".equals(subject.getSubjectCode())));
    }

    @Test
    void addSubject_isAdminOnly() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(TestAuth.asTeacher(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Physics\",\"subject_code\":\"PHY101\"}"))
                .andExpect(status().isForbidden());
        verify(subjectService, never()).addSubject(any());
    }

    @Test
    void addSubject_rejectsAMissingName() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(TestAuth.asAdmin(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_code\":\"PHY101\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subject name is required"));
    }

    @Test
    void addSubject_rejectsAnUnsafeCode() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(TestAuth.asAdmin(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Physics\",\"subject_code\":\"PHY 101!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subject code may only contain letters, digits and hyphens"));
    }

    @Test
    void updateSubject_returnsUpdatedSubject() throws Exception {
        Subject s = new Subject(10, "Advanced Physics", "PHY102");
        when(subjectService.updateSubject(eq(10), any())).thenReturn(true);
        when(subjectService.getSubjectById(10)).thenReturn(Optional.of(s));

        mockMvc.perform(put("/api/subjects/10")
                        .with(TestAuth.asAdmin(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Advanced Physics\",\"subject_code\":\"PHY102\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject_name").value("Advanced Physics"));
    }

    @Test
    void updateSubject_isAdminOnly() throws Exception {
        mockMvc.perform(put("/api/subjects/10")
                        .with(TestAuth.asStudent(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject_name\":\"Advanced Physics\",\"subject_code\":\"PHY102\"}"))
                .andExpect(status().isForbidden());
        verify(subjectService, never()).updateSubject(eq(10), any());
    }

    @Test
    void deleteSubject_returnsOk_whenFound() throws Exception {
        when(subjectService.deleteSubjectById(10)).thenReturn(true);

        mockMvc.perform(delete("/api/subjects/10").with(TestAuth.asAdmin(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subject deleted successfully"));
    }

    @Test
    void deleteSubject_isAdminOnly() throws Exception {
        mockMvc.perform(delete("/api/subjects/10").with(TestAuth.asTeacher(10)))
                .andExpect(status().isForbidden());
        verify(subjectService, never()).deleteSubjectById(10);
    }
}
