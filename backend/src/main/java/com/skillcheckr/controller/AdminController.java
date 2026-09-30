package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.service.AdminService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;

	@GetMapping("/teachers")
	public ResponseEntity<List<Teacher>> getAllTeachers(HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		List<Teacher> teachers = adminService.getAllTeacher();
		return ResponseEntity.ok(teachers != null ? teachers : List.of());
	}

	@GetMapping("/students")
	public ResponseEntity<List<Student>> getAllStudents(HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		List<Student> students = adminService.getAllStudent();
		return ResponseEntity.ok(students != null ? students : List.of());
	}

	@PostMapping("/students/from-request/{request_id}")
	public ResponseEntity<ApiResponse> addStudentFromRequest(@PathVariable("request_id") Integer requestId,
			HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		boolean success = adminService.addStudentFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(new ApiResponse(true, "Student added successfully"));
		}
		return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to add student from request"));
	}

	@PostMapping("/teachers/from-request/{request_id}")
	public ResponseEntity<ApiResponse> addTeacherFromRequest(@PathVariable("request_id") Integer requestId,
			HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		boolean success = adminService.addTeacherFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher added successfully"));
		}
		return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to add teacher from request"));
	}

	@DeleteMapping("/teachers/{teacher_id}")
	public ResponseEntity<ApiResponse> deleteTeacher(@PathVariable("teacher_id") Integer teacherId,
			HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		boolean deleted = adminService.deleteTeacherById(teacherId);
		if (deleted) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher deleted successfully"));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Teacher not found or could not be deleted"));
	}

	@DeleteMapping("/students/{student_id}")
	public ResponseEntity<ApiResponse> deleteStudent(@PathVariable("student_id") Integer studentId,
			HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		boolean deleted = adminService.deleteStudentById(studentId);
		if (deleted) {
			return ResponseEntity.ok(new ApiResponse(true, "Student account updated/removed successfully"));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Student not found or could not be processed"));
	}

	@PutMapping("/students/{student_id}/status")
	public ResponseEntity<ApiResponse> toggleStudentStatus(
			@PathVariable("student_id") Integer studentId,
			@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		String status = requiredStatus(body);
		boolean updated = adminService.toggleStudentStatus(studentId, status);
		if (updated) {
			return ResponseEntity.ok(new ApiResponse(true, "Student status updated to " + status));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Student not found or status update failed"));
	}

	@PutMapping("/teachers/{teacher_id}/status")
	public ResponseEntity<ApiResponse> toggleTeacherStatus(
			@PathVariable("teacher_id") Integer teacherId,
			@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		String status = requiredStatus(body);
		boolean updated = adminService.toggleTeacherStatus(teacherId, status);
		if (updated) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher status updated to " + status));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Teacher not found or status update failed"));
	}

	@GetMapping("/stats")
	public ResponseEntity<AdminStatsResponse> getAdminStats(HttpServletRequest request) {
		AuthGuard.requireAdmin(request);
		return ResponseEntity.ok(adminService.getAdminStats());
	}

	private String requiredStatus(Map<String, String> body) {
		String status = body == null || body.get("status") == null ? "" : body.get("status").trim();
		if (!"Active".equalsIgnoreCase(status) && !"Inactive".equalsIgnoreCase(status)) {
			throw new BadRequestException("Status must be either Active or Inactive");
		}
		return "Active".equalsIgnoreCase(status) ? "Active" : "Inactive";
	}
}
