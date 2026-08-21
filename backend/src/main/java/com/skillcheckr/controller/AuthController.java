package com.skillcheckr.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.LoginRequest;
import com.skillcheckr.model.User;
import com.skillcheckr.service.AuthService;

@RestController
@RequestMapping({"/api/authentication", "/api/auth"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		if (request == null || request.getUsername() == null || request.getPassword() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "Username and password are required"));
		}

		try {
			User user = authService.login(request.getUsername().trim(), request.getPassword());
			if (user != null) {
				int roleId = 0;
				if ("Student".equalsIgnoreCase(user.getRole())) {
					try { roleId = authService.getStudentIdByUserId(user.getUserId()); } catch (Exception ignored) {}
				} else if ("Teacher".equalsIgnoreCase(user.getRole())) {
					try { roleId = authService.getTeacherIdByUserId(user.getUserId()); } catch (Exception ignored) {}
				} else if ("Admin".equalsIgnoreCase(user.getRole())) {
					try { roleId = authService.getAdminIdByUserId(user.getUserId()); } catch (Exception ignored) {}
				}

				Map<String, Object> response = new HashMap<>();
				response.put("message", "Login Successful");
				response.put("username", user.getUsername());
				response.put("role", user.getRole());
				response.put("userId", user.getUserId());
				response.put("roleId", roleId);
				response.put("user_id", user.getUserId());
				response.put("role_id", roleId);
				response.put("token", "jwt-mock-" + user.getUserId() + "-" + System.currentTimeMillis());

				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("message", "Invalid username or password"));
			}
		} catch (EmptyResultDataAccessException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "Invalid username or password"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Login error: " + e.getMessage()));
		}
	}
}
