package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

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
import com.skillcheckr.persistence.repository.ExamAttemptJpaRepository;
import com.skillcheckr.persistence.repository.ExamJpaRepository;
import com.skillcheckr.persistence.repository.QuestionJpaRepository;
import com.skillcheckr.persistence.repository.RegistrationRequestJpaRepository;
import com.skillcheckr.persistence.repository.ResultJpaRepository;
import com.skillcheckr.persistence.repository.SubjectJpaRepository;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.TeacherProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@Repository
public class AdminRepositoryImpl implements AdminRepository {

    private final UserAccountJpaRepository accountRepository;
    private final StudentProfileJpaRepository studentRepository;
    private final TeacherProfileJpaRepository teacherRepository;
    private final RegistrationRequestJpaRepository requestRepository;
    private final ExamJpaRepository examRepository;
    private final SubjectJpaRepository subjectRepository;
    private final QuestionJpaRepository questionRepository;
    private final ResultJpaRepository resultRepository;
    private final ExamAttemptJpaRepository attemptRepository;

    public AdminRepositoryImpl(UserAccountJpaRepository accountRepository,
            StudentProfileJpaRepository studentRepository, TeacherProfileJpaRepository teacherRepository,
            RegistrationRequestJpaRepository requestRepository, ExamJpaRepository examRepository,
            SubjectJpaRepository subjectRepository, QuestionJpaRepository questionRepository,
            ResultJpaRepository resultRepository, ExamAttemptJpaRepository attemptRepository) {
        this.accountRepository = accountRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.requestRepository = requestRepository;
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
        this.attemptRepository = attemptRepository;
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

        if (!examRepository.findByTeacherIdOrderByExamIdDesc(teacherId).isEmpty()) {
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
        return resultRepository.countByStudentId(studentId) > 0
                || attemptRepository.countByStudentId(studentId) > 0;
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

        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalExams = examRepository.count();
        int pendingExams = examRepository.countPendingExams();
        int upcomingExams = examRepository.countUpcomingExams();
        int completedExams = examRepository.countCompletedExams();
        int pendingRequests = requestRepository.countPendingRequests();
        long totalSubjects = subjectRepository.count();
        long totalQuestions = questionRepository.count();
        long totalResults = resultRepository.count();

        stats.setTotalStudents((int) totalStudents);
        stats.setTotalTeachers((int) totalTeachers);
        stats.setTotalExams((int) totalExams);
        stats.setPendingExams(pendingExams);
        stats.setUpcomingExams(upcomingExams);
        stats.setCompletedExams(completedExams);
        stats.setPendingRequests(pendingRequests);
        stats.setTotalSubjects((int) totalSubjects);
        stats.setTotalQuestions((int) totalQuestions);
        stats.setTotalResults((int) totalResults);
        return stats;
    }
}
