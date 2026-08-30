package com.skillcheckr.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService {

	@Autowired
	private AdminRepository adminRepository;

	@Override
	public boolean addTeacherFromRequest(int requestId) {
		String username = adminRepository.getUsernameByRequestId(requestId);
		if (username != null && isUsernameExist(username)) {
			return false;
		}
		return adminRepository.addTeacherFromRequest(requestId);
	}

	@Override
	public boolean addStudentFromRequest(int requestId) {
		String username = adminRepository.getUsernameByRequestId(requestId);
		if (username != null && isUsernameExist(username)) {
			return false;
		}
		return adminRepository.addStudentFromRequest(requestId);
	}

	@Override
	public boolean isUsernameExist(String username) {
		return adminRepository.isUsernameExist(username);
	}

	@Override
	public List<Teacher> getAllTeacher() {
		return adminRepository.getAllTeacher();
	}

	@Override
	public List<Student> getAllStudent() {
		return adminRepository.getAllStudent();
	}

	@Override
	public boolean deleteTeacherById(int teacherId) {
		return adminRepository.deleteTeacherById(teacherId);
	}

	@Override
	public boolean deleteStudentById(int studentId) {
		return adminRepository.deleteStudentById(studentId);
	}

	@Override
	public boolean toggleTeacherStatus(int teacherId, String status) {
		return adminRepository.toggleTeacherStatus(teacherId, status);
	}

	@Override
	public boolean toggleStudentStatus(int studentId, String status) {
		return adminRepository.toggleStudentStatus(studentId, status);
	}

	@Override
	public String getUsernameByRequestId(int requestId) {
		return adminRepository.getUsernameByRequestId(requestId);
	}

	@Override
	public Map<String, Object> getAdminStats() {
		return adminRepository.getAdminStats();
	}
}
