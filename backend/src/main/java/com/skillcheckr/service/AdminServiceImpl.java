package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.RoleConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.repository.AdminRepository;
import com.skillcheckr.repository.ExamRepository;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public boolean addTeacherFromRequest(int requestId) {
        return addUserFromRequest(requestId, RoleConstants.ROLE_TEACHER);
    }

    @Override
    @Transactional
    public boolean addStudentFromRequest(int requestId) {
        return addUserFromRequest(requestId, RoleConstants.ROLE_STUDENT);
    }

    private boolean addUserFromRequest(int requestId, String role) {
        RegistrationRequest request = adminRepository.findRequestById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration request not found"));

        String status = request.getStatus() == null ? "Pending" : request.getStatus();
        if ("Approved".equalsIgnoreCase(status)) {
            throw new BadRequestException("This request has already been approved");
        }

        String requestedRole = request.getRequestedRole() == null
                ? "" : request.getRequestedRole().trim();
        if (!requestedRole.isEmpty() && !role.equalsIgnoreCase(requestedRole)) {
            throw new BadRequestException("This request asked for the " + requestedRole
                    + " role and cannot be approved as " + role);
        }

        String username = request.getUsername();
        String email = request.getEmail() == null ? "" : request.getEmail();
        String name = request.getName() == null ? username : request.getName();
        String contact = request.getContact();

        if (adminRepository.existsUserByUsername(username)) {
            throw new BadRequestException("An account with the username '" + username + "' already exists");
        }

        boolean emailExists = RoleConstants.ROLE_STUDENT.equals(role)
                ? adminRepository.existsStudentByEmail(email)
                : adminRepository.existsTeacherByEmail(email);
        if (emailExists) {
            throw new BadRequestException("An account with the email '" + email + "' already exists");
        }

        String rawPassword = request.getPassword();
        String encodedPassword = rawPassword != null && rawPassword.startsWith("$2")
                ? rawPassword
                : passwordEncoder.encode(rawPassword == null ? "" : rawPassword);

        int userId = adminRepository.createUser(username, encodedPassword, role);
        if (RoleConstants.ROLE_STUDENT.equals(role)) {
            adminRepository.createStudent(userId, name, contact, email);
        } else {
            adminRepository.createTeacher(userId, name, contact, email);
        }
        adminRepository.updateRequestStatus(requestId, "Approved");
        return true;
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
    @Transactional
    public boolean deleteTeacherById(int teacherId) {
        return adminRepository.deleteTeacherById(teacherId);
    }

    @Override
    @Transactional
    public boolean deleteStudentById(int studentId) {
        return adminRepository.deleteStudentById(studentId);
    }

    @Override
    @Transactional
    public boolean toggleTeacherStatus(int teacherId, String status) {
        return adminRepository.toggleTeacherStatus(teacherId, status);
    }

    @Override
    @Transactional
    public boolean toggleStudentStatus(int studentId, String status) {
        return adminRepository.toggleStudentStatus(studentId, status);
    }

    @Override
    public AdminStatsResponse getAdminStats() {
        examRepository.syncExamStatuses();
        return adminRepository.getAdminStats();
    }
}
