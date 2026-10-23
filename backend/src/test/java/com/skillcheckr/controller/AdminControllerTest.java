package com.skillcheckr.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.service.AdminService;
import com.skillcheckr.support.TestAuth;

@SpringBootTest(properties = "app.exam.attempt-finalizer-initial-delay-ms=3600000")
@AutoConfigureMockMvc
class AdminControllerTest {

    private static final org.springframework.test.web.servlet.request.RequestPostProcessor ADMIN = TestAuth.asAdminAccount(30, 1);

    @MockBean
    private AdminService adminService;

    @MockBean
    private com.skillcheckr.service.AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void stubActive() {
        Mockito.when(authService.isUserActive(Mockito.anyInt())).thenReturn(true);
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
    void deleteStudent_returnsOk_whenDeleted() throws Exception {
        when(adminService.deleteStudentById(6)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/students/6").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteStudent_returnsNotFound_whenMissing() throws Exception {
        when(adminService.deleteStudentById(6)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/students/6").with(ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStudent_isAdminOnly() throws Exception {
        mockMvc.perform(delete("/api/admin/students/6").with(TestAuth.asStudent(7)))
                .andExpect(status().isForbidden());
        verify(adminService, never()).deleteStudentById(6);
    }

    @Test
    void toggleStudentStatus_returnsOk() throws Exception {
        when(adminService.toggleStudentStatus(6, "Inactive")).thenReturn(true);

        mockMvc.perform(put("/api/admin/students/6/status")
                .with(ADMIN)
                .contentType("application/json")
                .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void toggleTeacherStatus_returnsOk() throws Exception {
        when(adminService.toggleTeacherStatus(5, "Inactive")).thenReturn(true);

        mockMvc.perform(put("/api/admin/teachers/5/status")
                .with(ADMIN)
                .contentType("application/json")
                .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAdminStats_returnsStats() throws Exception {
        AdminStatsResponse stats = new AdminStatsResponse();
        when(adminService.getAdminStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTeacher_returnsOk_whenDeleted() throws Exception {
        when(adminService.deleteTeacherById(5)).thenReturn(true);

        mockMvc.perform(delete("/api/admin/teachers/5").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteTeacher_returnsNotFound_whenMissing() throws Exception {
        when(adminService.deleteTeacherById(5)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/teachers/5").with(ADMIN))
                .andExpect(status().isNotFound());
    }
}
