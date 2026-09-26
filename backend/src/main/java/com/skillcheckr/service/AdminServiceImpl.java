package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService {

	@Autowired
	private AdminRepository adminRepository;

	@Override
	public boolean addTeacherFromRequest(int requestId) {
		return adminRepository.addTeacherFromRequest(requestId);
	}

	@Override
	public boolean addStudentFromRequest(int requestId) {
		return adminRepository.addStudentFromRequest(requestId);
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
	public AdminStatsResponse getAdminStats() {
		return adminRepository.getAdminStats();
	}
}
