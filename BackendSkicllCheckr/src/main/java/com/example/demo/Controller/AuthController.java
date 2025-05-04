package com.example.demo.Controller;

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

import com.example.demo.Model.LoginRequest;
import com.example.demo.Model.User;
import com.example.demo.Service.AuthService;

@RestController
@RequestMapping("/api/authentication")
//@CrossOrigin(origins = "http://localhost:5173")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

	@Autowired
	private AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		try {
			User user = authService.login(request.getUsername(), request.getPassword());
			if (user != null) {

//                return ResponseEntity.ok("Login Successfully. Welcome " + user.getUser_role() + ", ID: " + user.getUser_id());

				int roleId = 0;
				
				if("Student".equals(user.getUser_role()))
				{
					
					roleId = authService.getStudentIdByUserId(user.getUser_id());
				}
				else if("Teacher".equals(user.getUser_role())) {
					
					roleId = authService.getTeacherIdByUserId(user.getUser_id());
				}
				else if("Admin".equals(user.getUser_role()))
				{
					 roleId = authService.getAdminIdByUserId(user.getUser_id());
				}
				  Map<String, Object> response = new HashMap<>();
		            response.put("message", "Login Successful");
		            response.put("role", user.getUser_role());
		            response.put("userId", user.getUser_id());
		            response.put("roleId", roleId);
return ResponseEntity.ok(response);

			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
			}
		} catch (EmptyResultDataAccessException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		}
	}
}
