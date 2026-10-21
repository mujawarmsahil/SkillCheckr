package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.model.AdminStatsResponse;
import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Teacher;
import com.skillcheckr.persistence.entity.RegistrationRequestEntity;
import com.skillcheckr.persistence.entity.StudentProfileEntity;
import com.skillcheckr.persistence.entity.TeacherProfileEntity;
import com.skillcheckr.persistence.entity.UserAccountEntity;
import com.skillcheckr.persistence.repository.RegistrationRequestJpaRepository;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.TeacherProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@Repository
public class AdminRepositoryImpl implements AdminRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountJpaRepository accountRepository;
    private final StudentProfileJpaRepository studentRepository;
    private final TeacherProfileJpaRepository teacherRepository;
    private final RegistrationRequestJpaRepository requestRepository;

    public AdminRepositoryImpl(JdbcTemplate jdbcTemplate, UserAccountJpaRepository accountRepository,
            StudentProfileJpaRepository studentRepository, TeacherProfileJpaRepository teacherRepository,
            RegistrationRequestJpaRepository requestRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountRepository = accountRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.requestRepository = requestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistrationRequest> findRequestById(int requestId) {
        return requestRepository.findById(requestId).map(this::toRegistrationRequest);
    }

    /**
     * Unlike the list projection in {@link RegistrationRequestRepositoryImpl}, the
     * approval flow needs the stored password hash to create the account.
     */
    private RegistrationRequest toRegistrationRequest(RegistrationRequestEntity entity) {
        RegistrationRequest request = new RegistrationRequest();
        request.setRequestId(entity.getRequestId());
        request.setName(entity.getName());
        request.setEmail(entity.getEmail());
        request.setUsername(entity.getUsername());
        request.setPassword(entity.getPassword());
        request.setContact(entity.getContact());
        request.setRequestedRole(entity.getRequestedRole());
        request.setStatus(entity.getStatus());
        return request;
    }

    private Student toStudent(StudentProfileEntity entity) {
        Student student = new Student();
        student.setStudentId(entity.getStudentId());
        student.setUserId(entity.getUser().getUserId());
        student.setName(entity.getName());
        student.setContact(entity.getContact());
        student.setEmail(entity.getEmail());
        student.setProfileImage(entity.getProfileImage());
        student.setStatus(entity.getStatus() == null || entity.getStatus().isBlank()
                ? "Active" : entity.getStatus());
        return student;
    }

    private Teacher toTeacher(TeacherProfileEntity entity) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(entity.getTeacherId());
        teacher.setUserId(entity.getUser().getUserId());
        teacher.setName(entity.getName());
        teacher.setContact(entity.getContact());
        teacher.setEmail(entity.getEmail());
        teacher.setProfileImage(entity.getProfileImage());
        teacher.setStatus(entity.getStatus() == null || entity.getStatus().isBlank()
                ? "Active" : entity.getStatus());
        return teacher;
    }

    @Override
    public boolean existsUserByUsername(String username) {
        return username != null && accountRepository.existsByUsernameIgnoreCase(username.trim());
    }

    @Override
    public boolean existsTeacherByEmail(String email) {
        return email != null && teacherRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Override
    public boolean existsStudentByEmail(String email) {
        return email != null && studentRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Override
    @Transactional
    public int createUser(String username, String encodedPassword, String role) {
        UserAccountEntity account = new UserAccountEntity();
        account.setUsername(username);
        account.setPassword(encodedPassword);
        account.setRole(role);
        account.setAuthProvider("LOCAL");
        account.setStatus("Active");
        UserAccountEntity saved = accountRepository.save(account);
        if (saved.getUserId() == null) {
            throw new IllegalStateException("Unable to create the account for this request");
        }
        return saved.getUserId();
    }

    @Override
    @Transactional
    public void createTeacher(int userId, String name, String contact, String email) {
        TeacherProfileEntity teacher = new TeacherProfileEntity();
        teacher.setUser(accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found for teacher profile")));
        teacher.setName(name);
        teacher.setContact(contact);
        teacher.setEmail(email);
        teacher.setStatus("Active");
        teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public void createStudent(int userId, String name, String contact, String email) {
        StudentProfileEntity student = new StudentProfileEntity();
        student.setUser(accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found for student profile")));
        student.setName(name);
        student.setContact(contact);
        student.setEmail(email);
        student.setStatus("Active");
        studentRepository.save(student);
    }

    @Override
    @Transactional
    public boolean updateRequestStatus(int requestId, String status) {
        return requestRepository.updateStatus(requestId, status) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> getAllTeacher() {
        return teacherRepository.findAllByOrderByTeacherIdDesc().stream()
                .map(this::toTeacher)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudent() {
        return studentRepository.findAllByOrderByStudentIdDesc().stream()
                .map(this::toStudent)
                .toList();
    }

    @Override
    @Transactional
    public boolean deleteTeacherById(int teacherId) {
        Optional<TeacherProfileEntity> result = teacherRepository.findById(teacherId);
        if (result.isEmpty()) {
            return false;
        }
        TeacherProfileEntity teacher = result.get();
        UserAccountEntity account = teacher.getUser();

        if (count("SELECT COUNT(*) FROM exam WHERE teacher_id = ?", teacherId) > 0) {
            teacher.setStatus("Inactive");
            account.setStatus("Inactive");
            teacherRepository.save(teacher);
            accountRepository.save(account);
            return true;
        }

        teacherRepository.delete(teacher);
        teacherRepository.flush();
        accountRepository.delete(account);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteStudentById(int studentId) {
        Optional<StudentProfileEntity> result = studentRepository.findById(studentId);
        if (result.isEmpty()) {
            return false;
        }
        StudentProfileEntity student = result.get();
        UserAccountEntity account = student.getUser();

        if (hasAcademicHistory(studentId)) {
            student.setStatus("Inactive");
            account.setStatus("Inactive");
            studentRepository.save(student);
            accountRepository.save(account);
            return true;
        }

        studentRepository.delete(student);
        studentRepository.flush();
        accountRepository.delete(account);
        return true;
    }

    private boolean hasAcademicHistory(int studentId) {
        return count("SELECT COUNT(*) FROM result WHERE student_id = ?", studentId) > 0
                || count("SELECT COUNT(*) FROM exam_attempt WHERE student_id = ?", studentId) > 0;
    }

    private int count(String sql, int id) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return result == null ? 0 : result;
    }

    @Override
    @Transactional
    public boolean toggleTeacherStatus(int teacherId, String status) {
        return teacherRepository.findById(teacherId).map(teacher -> {
            teacher.setStatus(status);
            teacher.getUser().setStatus(status);
            teacherRepository.save(teacher);
            accountRepository.save(teacher.getUser());
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean toggleStudentStatus(int studentId, String status) {
        return studentRepository.findById(studentId).map(student -> {
            student.setStatus(status);
            student.getUser().setStatus(status);
            studentRepository.save(student);
            accountRepository.save(student.getUser());
            return true;
        }).orElse(false);
    }

    @Override
    public AdminStatsResponse getAdminStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        Integer totalStudents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
        Integer totalTeachers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher", Integer.class);
        Integer totalExams = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam", Integer.class);
        Integer pendingExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE status = 'Pending' OR status IS NULL", Integer.class);
        Integer upcomingExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE (status = 'Upcoming' OR status = 'Approved') "
                        + "AND (DATE(exam_date) > CURDATE() OR (DATE(exam_date) = CURDATE() "
                        + "AND (end_time IS NULL OR end_time >= CURTIME())))",
                Integer.class);
        Integer completedExams = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam WHERE status = 'Completed' OR (DATE(exam_date) < CURDATE() "
                        + "OR (DATE(exam_date) = CURDATE() AND end_time IS NOT NULL AND end_time < CURTIME()))",
                Integer.class);
        Integer pendingRequests = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM request WHERE status = 'Pending' OR status IS NULL", Integer.class);
        Integer totalSubjects = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subject", Integer.class);
        Integer totalQuestions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question", Integer.class);
        Integer totalResults = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM result", Integer.class);

        stats.setTotalStudents(totalStudents != null ? totalStudents : 0);
        stats.setTotalTeachers(totalTeachers != null ? totalTeachers : 0);
        stats.setTotalExams(totalExams != null ? totalExams : 0);
        stats.setPendingExams(pendingExams != null ? pendingExams : 0);
        stats.setUpcomingExams(upcomingExams != null ? upcomingExams : 0);
        stats.setCompletedExams(completedExams != null ? completedExams : 0);
        stats.setPendingRequests(pendingRequests != null ? pendingRequests : 0);
        stats.setTotalSubjects(totalSubjects != null ? totalSubjects : 0);
        stats.setTotalQuestions(totalQuestions != null ? totalQuestions : 0);
        stats.setTotalResults(totalResults != null ? totalResults : 0);
        return stats;
    }
}
