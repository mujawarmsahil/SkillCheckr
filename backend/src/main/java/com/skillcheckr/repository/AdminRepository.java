package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

public interface AdminRepository {

	boolean addTeacherFromRequest(int requestId);

	boolean addStudentFromRequest(int requestId);

	boolean isUsernameExist(String username);

	List<Teacher> getAllTeacher();

	List<Student> getAllStudent();

	boolean deleteTeacherById(int teacherId);

	boolean deleteStudentById(int studentId);

	boolean toggleTeacherStatus(int teacherId, String status);

	boolean toggleStudentStatus(int studentId, String status);

	Optional<String> getUsernameByRequestId(int requestId);

	AdminStatsResponse getAdminStats();
}
