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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.service.AdminService;

@RestController
@RequestMapping({"/api/Admin", "/api/admin"})
public class AdminController {

	@Autowired
	private AdminService adminService;

	@GetMapping({"/viewAllTeacher", "/teachers"})
	public ResponseEntity<List<Teacher>> getAllTeachers() {
		List<Teacher> teachers = adminService.getAllTeacher();
		return ResponseEntity.ok(teachers != null ? teachers : List.of());
	}

	@GetMapping({"/viewAllStudent", "/students"})
	public ResponseEntity<List<Student>> getAllStudents() {
		List<Student> students = adminService.getAllStudent();
		return ResponseEntity.ok(students != null ? students : List.of());
	}

	@PostMapping({"/addStudent/{request_id}", "/students/from-request/{request_id}"})
	public ResponseEntity<ApiResponse> addStudentFromRequest(@PathVariable("request_id") Integer requestId) {
		boolean success = adminService.addStudentFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(new ApiResponse(true, "Student added successfully"));
		}
		return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to add student from request"));
	}

	@PostMapping({"/addTeacher/{request_id}", "/teachers/from-request/{request_id}"})
	public ResponseEntity<ApiResponse> addTeacherFromRequest(@PathVariable("request_id") Integer requestId) {
		boolean success = adminService.addTeacherFromRequest(requestId);
		if (success) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher added successfully"));
		}
		return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to add teacher from request"));
	}

	@DeleteMapping({"/teacherDeleteById/{teacher_id}", "/teachers/{teacher_id}"})
	public ResponseEntity<ApiResponse> deleteTeacher(@PathVariable("teacher_id") Integer teacherId) {
		boolean deleted = adminService.deleteTeacherById(teacherId);
		if (deleted) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher deleted successfully"));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Teacher not found or could not be deleted"));
	}

	@DeleteMapping({"/studentDelteteById/{student_id}", "/studentDeleteById/{student_id}", "/students/{student_id}"})
	public ResponseEntity<ApiResponse> deleteStudent(@PathVariable("student_id") Integer studentId) {
		boolean deleted = adminService.deleteStudentById(studentId);
		if (deleted) {
			return ResponseEntity.ok(new ApiResponse(true, "Student account updated/removed successfully"));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Student not found or could not be processed"));
	}

	@org.springframework.web.bind.annotation.PutMapping({"/student/{student_id}/status", "/students/{student_id}/status"})
	public ResponseEntity<ApiResponse> toggleStudentStatus(
			@PathVariable("student_id") Integer studentId,
			@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
		String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
				? body.get("status").trim()
				: "Active";
		boolean updated = adminService.toggleStudentStatus(studentId, status);
		if (updated) {
			return ResponseEntity.ok(new ApiResponse(true, "Student status updated to " + status));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Student not found or status update failed"));
	}

	@org.springframework.web.bind.annotation.PutMapping({"/teacher/{teacher_id}/status", "/teachers/{teacher_id}/status"})
	public ResponseEntity<ApiResponse> toggleTeacherStatus(
			@PathVariable("teacher_id") Integer teacherId,
			@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
		String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
				? body.get("status").trim()
				: "Active";
		boolean updated = adminService.toggleTeacherStatus(teacherId, status);
		if (updated) {
			return ResponseEntity.ok(new ApiResponse(true, "Teacher status updated to " + status));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse(false, "Teacher not found or status update failed"));
	}

	@GetMapping("/stats")
	public ResponseEntity<AdminStatsResponse> getAdminStats() {
		return ResponseEntity.ok(adminService.getAdminStats());
	}
}