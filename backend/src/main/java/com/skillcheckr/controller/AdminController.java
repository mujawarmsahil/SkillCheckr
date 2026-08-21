package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.service.AdminService;

@RestController
@RequestMapping({"/api/Admin", "/api/admin"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class AdminController {

	@Autowired
	private AdminService adminService;

	@GetMapping({"/viewAllTeacher", "/teachers"})
	public ResponseEntity<?> viewAllTeacher() {
		List<Teacher> list = adminService.getAllTeacher();
		return ResponseEntity.ok(list != null ? list : List.of());
	}

	@GetMapping({"/viewAllStudent", "/students"})
	public ResponseEntity<?> viewAllStudent() {
		List<Student> list = adminService.getAllStudent();
		return ResponseEntity.ok(list != null ? list : List.of());
	}

	@PostMapping({"/addStudent/{request_id}", "/students/from-request/{request_id}"})
	public ResponseEntity<Object> addStudentFromRequest(@PathVariable("request_id") Integer requestId) {
		boolean success = adminService.addStudentFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(Map.of("message", "Student added successfully", "success", true));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "Failed to add student. User may already exist or request invalid.", "success", false));
		}
	}

	@PostMapping({"/addTeacher/{request_id}", "/teachers/from-request/{request_id}"})
	public ResponseEntity<Object> addTeacherFromRequest(@PathVariable("request_id") Integer requestId) {
		boolean success = adminService.addTeacherFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(Map.of("message", "Teacher added successfully", "success", true));
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "Failed to add teacher. User may already exist or request invalid.", "success", false));
		}
	}

	@DeleteMapping({"/teacherDeleteById/{teacher_id}", "/teachers/{teacher_id}"})
	public ResponseEntity<?> deleteTeacher(@PathVariable("teacher_id") Integer teacherId) {
		boolean deleted = adminService.deleteTeacherById(teacherId);
		if (deleted) {
			return ResponseEntity.ok(Map.of("message", "Teacher deleted successfully", "success", true));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "Teacher not found or could not be deleted", "success", false));
		}
	}

	@DeleteMapping({"/studentDelteteById/{student_id}", "/studentDeleteById/{student_id}", "/students/{student_id}"})
	public ResponseEntity<?> deleteStudent(@PathVariable("student_id") Integer studentId) {
		boolean deleted = adminService.deleteStudentById(studentId);
		if (deleted) {
			return ResponseEntity.ok(Map.of("message", "Student deleted successfully", "success", true));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "Student not found or could not be deleted", "success", false));
		}
	}

	@GetMapping("/stats")
	public ResponseEntity<?> getAdminStats() {
		Map<String, Object> stats = adminService.getAdminStats();
		return ResponseEntity.ok(stats);
	}
}