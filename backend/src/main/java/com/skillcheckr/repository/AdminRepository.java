package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

public interface AdminRepository {

    Optional<RegistrationRequest> findRequestById(int requestId);

    boolean existsUserByUsername(String username);

    boolean existsTeacherByEmail(String email);

    boolean existsStudentByEmail(String email);

    int createUser(String username, String encodedPassword, String role);

    void createTeacher(int userId, String name, String contact, String email);

    void createStudent(int userId, String name, String contact, String email);

    boolean updateRequestStatus(int requestId, String status);

    List<Teacher> getAllTeacher();

    List<Student> getAllStudent();

    boolean deleteTeacherById(int teacherId);

    boolean deleteStudentById(int studentId);

    boolean toggleTeacherStatus(int teacherId, String status);

    boolean toggleStudentStatus(int studentId, String status);

    AdminStatsResponse getAdminStats();
}
