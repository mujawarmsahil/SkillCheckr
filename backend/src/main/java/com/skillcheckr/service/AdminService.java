package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;

public interface AdminService {

	boolean addTeacherFromRequest(int requestId);

	boolean addStudentFromRequest(int requestId);

	List<Teacher> getAllTeacher();

	List<Student> getAllStudent();

	boolean deleteTeacherById(int teacherId);

	boolean deleteStudentById(int studentId);

	boolean toggleTeacherStatus(int teacherId, String status);

	boolean toggleStudentStatus(int studentId, String status);

	AdminStatsResponse getAdminStats();
}
