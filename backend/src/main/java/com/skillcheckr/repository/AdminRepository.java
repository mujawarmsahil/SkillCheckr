package com.skillcheckr.repository;

import java.util.List;
import java.util.Map;

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

	String getUsernameByRequestId(int requestId);

	Map<String, Object> getAdminStats();
}
