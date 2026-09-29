package com.skillcheckr.controller;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.skillcheckr.exception.GlobalExceptionHandler;
import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.service.AdminService;
import com.skillcheckr.support.TestAuth;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final RequestPostProcessor ADMIN = TestAuth.asAdminAccount(30, 1);

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(TestAuth.authInterceptor())
                .build();
    }

    @Test
    void adminEndpoints_rejectRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/admin/teachers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoints_areRefusedForTeachers() throws Exception {
        mockMvc.perform(get("/api/admin/teachers").with(TestAuth.asTeacher(10)))
                .andExpect(status().isForbidden());
        verify(adminService, never()).getAllTeacher();
    }

    @Test
    void getAllTeachers_returnsEmptyList_whenEmpty() throws Exception {
        when(adminService.getAllTeacher()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/teachers").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllTeachers_returnsTeachers() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(1);
        teacher.setName("Alice");
        teacher.setEmail("alice@example.com");
        when(adminService.getAllTeacher()).thenReturn(List.of(teacher));

        mockMvc.perform(get("/api/admin/teachers").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacher_id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void getAllStudents_returnsEmptyList_whenEmpty() throws Exception {
        when(adminService.getAllStudent()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/students").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllStudents_returnsStudents() throws Exception {
        Student student = new Student();
        student.setStudentId(2);
        student.setName("Bob");
        student.setEmail("bob@example.com");
        when(adminService.getAllStudent()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/admin/students").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student_id").value(2))
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[0].email").value("bob@example.com"));
    }

    @Test
    void addStudent_returnsOk_whenAccepted() throws Exception {
        when(adminService.addStudentFromRequest(4)).thenReturn(true);

        mockMvc.perform(post("/api/admin/students/from-request/4").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student added successfully"));
    }

    @Test
    void addStudent_returnsBadRequest_whenRejected() throws Exception {
        when(adminService.addStudentFromRequest(4)).thenReturn(false);

        mockMvc.perform(post("/api/admin/students/from-request/4").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addStudent_isAdminOnly() throws Exception {
        mockMvc.perform(post("/api/admin/students/from-request/4").with(TestAuth.asStudent(7)))
                .andExpect(status().isForbidden());
        verify(adminService, never()).addStudentFromRequest(4);
    }

    @Test
    void addTeacher_returnsOk_whenAccepted() throws Exception {
        when(adminService.addTeacherFromRequest(5)).thenReturn(true);

        mockMvc.perform(post("/api/admin/teachers/from-request/5").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Teacher added successfully"));
    }

    @Test
    void addTeacher_returnsBadRequest_whenRejected() throws Exception {
        when(adminService.addTeacherFromRequest(5)).thenReturn(false);

        mockMvc.perform(post("/api/admin/teachers/from-request/5").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteTeacher_returns200_whenDeleted() throws Exception {
        when(adminService.deleteTeacherById(11)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/teachers/11").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTeacher_returns404_whenNotDeleted() throws Exception {
        when(adminService.deleteTeacherById(11)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/teachers/11").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStudent_returns200_whenDeleted() throws Exception {
        when(adminService.deleteStudentById(22)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/students/22").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteStudent_returns404_whenNotDeleted() throws Exception {
        when(adminService.deleteStudentById(22)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/students/22").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStudent_isAdminOnly() throws Exception {
        mockMvc.perform(delete("/api/admin/students/22").with(TestAuth.asTeacher(10)))
                .andExpect(status().isForbidden());
        verify(adminService, never()).deleteStudentById(22);
    }

    @Test
    void toggleStudentStatus_acceptsActiveAndInactive() throws Exception {
        when(adminService.toggleStudentStatus(22, "Inactive")).thenReturn(true);
        when(adminService.toggleTeacherStatus(11, "Active")).thenReturn(true);

        mockMvc.perform(put("/api/admin/students/22/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student status updated to Inactive"));

        mockMvc.perform(put("/api/admin/teachers/11/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\" ACTIVE \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Teacher status updated to Active"));
    }

    @Test
    void toggleStatus_rejectsAnUnsupportedStatus() throws Exception {
        mockMvc.perform(put("/api/admin/students/22/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Deleted\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status must be either Active or Inactive"));
        verify(adminService, never()).toggleStudentStatus(22, "Deleted");
    }

    @Test
    void toggleStudentStatus_returnsNotFound_whenTheStudentIsGone() throws Exception {
        when(adminService.toggleStudentStatus(99, "Inactive")).thenReturn(false);

        mockMvc.perform(put("/api/admin/students/99/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleTeacherStatus_returnsNotFound_whenTheTeacherIsGone() throws Exception {
        when(adminService.toggleTeacherStatus(98, "Active")).thenReturn(false);

        mockMvc.perform(put("/api/admin/teachers/98/status")
                        .with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Active\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleStudentStatus_requiresAnAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/students/22/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteStudent_reportsSuccess_whenTheAccountOnlyHadHistoryAndWasDeactivated() throws Exception {
        when(adminService.deleteStudentById(22)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/students/22").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTeacher_reportsNotFound_whenThereIsNothingToDeactivate() throws Exception {
        when(adminService.deleteTeacherById(11)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/teachers/11").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAdminStats_returnsOk_withStats() throws Exception {
        when(adminService.getAdminStats()).thenReturn(AdminStatsResponse.builder().totalStudents(5).build());

        mockMvc.perform(get("/api/admin/stats").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(5));
    }
}
