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

import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void viewAllTeacher_returnsEmptyList_whenEmpty() throws Exception {
        when(adminService.getAllTeacher()).thenReturn(List.of());

        mockMvc.perform(get("/api/Admin/viewAllTeacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void viewAllTeacher_returnsTeachers() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(1);
        teacher.setTeacherName("Alice");
        teacher.setTeacherEmail("alice@example.com");
        when(adminService.getAllTeacher()).thenReturn(List.of(teacher));

        mockMvc.perform(get("/api/Admin/viewAllTeacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacher_id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void viewAllStudent_returnsEmptyList_whenEmpty() throws Exception {
        when(adminService.getAllStudent()).thenReturn(List.of());

        mockMvc.perform(get("/api/Admin/viewAllStudent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void viewAllStudent_returnsStudents() throws Exception {
        Student student = new Student();
        student.setStudentId(2);
        student.setStudentName("Bob");
        student.setStudentEmail("bob@example.com");
        when(adminService.getAllStudent()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/Admin/viewAllStudent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student_id").value(2))
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[0].email").value("bob@example.com"));
    }

    @Test
    void addStudent_returnsOk_whenAccepted() throws Exception {
        when(adminService.addStudentFromRequest(4)).thenReturn(true);

        mockMvc.perform(post("/api/Admin/addStudent/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student added successfully"));
    }

    @Test
    void addStudent_returnsBadRequest_whenRejected() throws Exception {
        when(adminService.addStudentFromRequest(4)).thenReturn(false);

        mockMvc.perform(post("/api/Admin/addStudent/4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addTeacher_returnsOk_whenAccepted() throws Exception {
        when(adminService.addTeacherFromRequest(5)).thenReturn(true);

        mockMvc.perform(post("/api/Admin/addTeacher/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Teacher added successfully"));
    }

    @Test
    void addTeacher_returnsBadRequest_whenRejected() throws Exception {
        when(adminService.addTeacherFromRequest(5)).thenReturn(false);

        mockMvc.perform(post("/api/Admin/addTeacher/5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteTeacher_returns200_whenDeleted() throws Exception {
        when(adminService.deleteTeacherById(11)).thenReturn(true);

        mockMvc.perform(delete("/api/Admin/teacherDeleteById/11")).andExpect(status().isOk());
    }

    @Test
    void deleteTeacher_returns404_whenNotDeleted() throws Exception {
        when(adminService.deleteTeacherById(11)).thenReturn(false);

        mockMvc.perform(delete("/api/Admin/teacherDeleteById/11")).andExpect(status().isNotFound());
    }

    @Test
    void deleteStudent_returns200_whenDeleted() throws Exception {
        when(adminService.deleteStudentById(22)).thenReturn(true);

        mockMvc.perform(delete("/api/Admin/studentDelteteById/22")).andExpect(status().isOk());
    }

    @Test
    void deleteStudent_returns404_whenNotDeleted() throws Exception {
        when(adminService.deleteStudentById(22)).thenReturn(false);

        mockMvc.perform(delete("/api/Admin/studentDelteteById/22")).andExpect(status().isNotFound());
    }

    @Test
    void getAdminStats_returnsOk_withStats() throws Exception {
        when(adminService.getAdminStats()).thenReturn(java.util.Map.of("totalStudents", 5));

        mockMvc.perform(get("/api/Admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(5));
    }
}