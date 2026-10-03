package com.skillcheckr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.constant.RoleConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ForbiddenException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.AttemptAnswerResponse;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.EvaluationItemDTO;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamAttemptResponse;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.model.QuestionBankDocument;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.RegistrationCountResponse;
import com.skillcheckr.model.RegistrationStatusResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.security.AuthPrincipal;
import com.skillcheckr.service.AttemptAnswerService;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.ExamSubmissionService;
import com.skillcheckr.service.QuestionBankService;
import com.skillcheckr.service.QuestionService;
import com.skillcheckr.validation.RequestValueParser;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AttemptAnswerService attemptAnswerService;

    @Autowired
    private ExamSubmissionService examSubmissionService;

    @Autowired
    private QuestionBankService questionBankService;

    @PostMapping("")
    public ResponseEntity<Exam> addExams(@RequestBody Exam exam, HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requireStaff(request);
        int teacherId = principal.isTeacher() ? principal.getRoleId() : 0;
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.saveExam(exam, teacherId));
    }

    @GetMapping("")
    public ResponseEntity<List<Exam>> getAllExams(HttpServletRequest request) {
        AuthGuard.requireStaff(request);
        List<Exam> exams = examService.getAllExams();
        if (exams == null || exams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(exams);
    }

    @GetMapping("/{exam_id}")
    public ResponseEntity<Exam> getExamById(@PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requirePrincipal(request);
        Exam exam = requireExam(examId);
        if (principal.isStudent()) {
            requireStudentVisibleExam(exam);
        }
        return ResponseEntity.ok(exam);
    }

    @PostMapping(path = "/{exam_id}/question-bank", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadQuestionBank(
            @PathVariable("exam_id") Integer examId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionOnlyConfirmed") boolean questionOnlyConfirmed,
            HttpServletRequest request) throws java.io.IOException {
        Exam exam = requireExam(examId);
        AuthGuard.requireExamAccess(request, exam);
        if (!ExamConstants.EXAM_STATUS_PENDING.equalsIgnoreCase(exam.getStatus())) {
            throw new BadRequestException("A question-bank PDF can only be changed while the exam is pending approval");
        }
        questionBankService.saveUploadedPdf(examId, file.getOriginalFilename(), file.getBytes(),
                questionOnlyConfirmed);
        return ResponseEntity.ok(new ApiResponse(true, "Student question-bank PDF uploaded successfully"));
    }

    @GetMapping("/{exam_id}/question-bank")
    public ResponseEntity<byte[]> downloadQuestionBank(
            @PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requirePrincipal(request);
        Exam exam = requireExam(examId);
        if (principal.isStudent()) {
            requireStudentVisibleExam(exam);
        } else {
            AuthGuard.requireExamAccess(request, exam);
        }

        QuestionBankDocument document = questionBankService.getStudentDocument(exam);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(document.fileName()).build());
        headers.setCacheControl("private, no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(document.content());
    }

    @PostMapping("/{exam_id}/attempts")
    public ResponseEntity<ExamAttemptResponse> startAttempt(
            @PathVariable("exam_id") Integer examId,
            HttpServletRequest request) {
        int studentId = AuthGuard.requireStudentId(request);
        AttemptStartResult result = examService.startAttempt(examId, studentId);
        ExamAttempt attempt = result.getAttempt();
        ExamAttemptResponse response = ExamAttemptResponse.from(attempt, examId);
        return ResponseEntity.status(result.isExisting() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{exam_id}/attempts/{attempt_id}/answers/{question_id}")
    public ResponseEntity<AttemptAnswerResponse> saveAttemptAnswer(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            @PathVariable("question_id") Integer questionId,
            @RequestBody(required = false) AttemptAnswerRequest request,
            HttpServletRequest servletRequest) {
        int studentId = AuthGuard.requireStudentId(servletRequest);
        AttemptAnswerResponse response = attemptAnswerService.saveAnswer(
                examId, attemptId, questionId, studentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{exam_id}/attempts/{attempt_id}/answers")
    public ResponseEntity<List<AttemptAnswerResponse>> getAttemptAnswers(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            HttpServletRequest request) {
        int studentId = AuthGuard.requireStudentId(request);
        return ResponseEntity.ok(attemptAnswerService.getAnswers(examId, attemptId, studentId));
    }

    @PostMapping("/{exam_id}/attempts/{attempt_id}/submit")
    public ResponseEntity<ExamResultDTO> submitAttempt(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            HttpServletRequest request) {
        int studentId = AuthGuard.requireStudentId(request);
        return ResponseEntity.ok(examSubmissionService.submit(examId, attemptId, studentId));
    }

    @GetMapping("/teacher/{teacher_id}")
    public ResponseEntity<List<Exam>> getExamsByTeacherId(@PathVariable("teacher_id") Integer teacherId,
            HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requireStaff(request);
        if (principal.isTeacher() && principal.getRoleId() != teacherId) {
            throw new ForbiddenException("You are not authorized to view another teacher's exams.");
        }
        List<Exam> exams = examService.getExamsByTeacherId(teacherId);
        return ResponseEntity.ok(exams != null ? exams : List.of());
    }

    @GetMapping("/{exam_id}/questions")
    public ResponseEntity<List<QuestionDTO>> getQuestionsForExam(@PathVariable("exam_id") Integer examId,
            HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requirePrincipal(request);
        Exam exam = requireExam(examId);
        if (principal.isStudent()) {
            requireStudentVisibleExam(exam);
        }
        // The answer key stays with staff; a student only ever receives the question text.
        List<QuestionDTO> questions = principal.isStudent()
                ? questionService.getStudentQuestionsByExamId(examId)
                : questionService.getQuestionsByExamId(examId);
        return ResponseEntity.ok(questions != null ? questions : List.of());
    }

    @PostMapping("/{exam_id}/questions")
    public ResponseEntity<ApiResponse> attachExistingQuestions(@PathVariable("exam_id") Integer examId,
            @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        AuthPrincipal principal = AuthGuard.requireStaff(request);
        AuthGuard.requireExamAccess(request, requireExam(examId));

        if (body == null || body.get("questionIds") == null) {
            throw new BadRequestException("questionIds is required");
        }

        List<?> rawIds = body.get("questionIds") instanceof List
                ? (List<?>) body.get("questionIds")
                : List.of();
        List<Integer> questionIds = rawIds.stream()
                .map(value -> RequestValueParser.parseInt(value, "questionIds"))
                .toList();
        int attached = questionService.attachQuestionsToExam(examId, questionIds);
        return ResponseEntity.ok(new ApiResponse(true,
                attached + " question(s) added to the exam. Attached by " + principal.getUsername() + "."));
    }

    @DeleteMapping("/{exam_id}")
    public ResponseEntity<String> deleteExam(@PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthGuard.requireExamAccess(request, requireExam(examId));
        if (!examService.deleteExamById(examId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam not found or could not be deleted.");
        }
        return ResponseEntity.ok("Exam deleted successfully.");
    }

    @PostMapping("/{exam_id}/approve")
    public ResponseEntity<String> acceptExam(@PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        if (!examService.acceptExam(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }
        return ResponseEntity.ok("Accepted");
    }

    @PostMapping("/{exam_id}/reject")
    public ResponseEntity<ApiResponse> rejectExam(@PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        if (!examService.updateExamStatus(examId, ExamConstants.EXAM_STATUS_REJECTED)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Exam not found"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam rejected successfully"));
    }

    @PostMapping("/{exam_id}/cancel")
    public ResponseEntity<ApiResponse> cancelExam(@PathVariable("exam_id") Integer examId, HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        if (!examService.updateExamStatus(examId, ExamConstants.EXAM_STATUS_CANCELLED)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Exam not found"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam cancelled successfully"));
    }

    @PutMapping("/{exam_id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable("exam_id") Integer examId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        AuthGuard.requireAdmin(request);
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : ExamConstants.EXAM_STATUS_UPCOMING;
        if (!examService.updateExamStatus(examId, status)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Exam not found or status update failed"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam status updated to " + status));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Exam>> getAllUpcomingExams(HttpServletRequest request) {
        AuthGuard.requirePrincipal(request);
        List<Exam> exams = examService.getAllUpcomingExams();
        return ResponseEntity.ok(exams != null ? exams : List.of());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Exam>> getAllCompletedExams(HttpServletRequest request) {
        AuthGuard.requirePrincipal(request);
        List<Exam> exams = examService.getAllCompletedExams();
        if (exams == null || exams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(exams);
    }

    @PostMapping("/{exam_id}/register")
    public ResponseEntity<ApiResponse> registerForExam(
            @PathVariable(value = "exam_id", required = false) Integer pathExamId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        // A student always registers for themselves. The optional path and body values
        // stay for backwards compatibility but can never select another student.
        int studentId = AuthGuard.requireStudentId(request);
        Integer examId = pathExamId;

        if (examId == null && body != null) {
            Object rawExamId = body.containsKey("examId") ? body.get("examId") : body.get("exam_id");
            if (rawExamId != null) {
                examId = RequestValueParser.parseOptionalInt(rawExamId, "examId");
            }
        }
        if (examId == null || examId <= 0) {
            throw new BadRequestException("A valid examId is required");
        }

        Exam exam = requireExam(examId);
        examService.registerStudentForExam(studentId, examId);
        return ResponseEntity.ok(new ApiResponse(true,
                "Registered for " + exam.getExamName() + ". You may attend when the exam window starts."));
    }

    @GetMapping("/registrations/student/{student_id}")
    public ResponseEntity<List<Integer>> getRegistrationsForStudent(@PathVariable("student_id") Integer studentId,
            HttpServletRequest request) {
        AuthGuard.requireSelfOrStaff(request, studentId);
        List<Integer> registeredExamIds = examService.getRegisteredExamIdsForStudent(studentId);
        return ResponseEntity.ok(registeredExamIds != null ? registeredExamIds : List.of());
    }

    @GetMapping("/registrations/student/{student_id}/detailed")
    public ResponseEntity<List<ExamRegistration>> getDetailedRegistrationsForStudent(
            @PathVariable("student_id") Integer studentId, HttpServletRequest request) {
        AuthGuard.requireSelfOrStaff(request, studentId);
        List<ExamRegistration> registrations = examService.getRegistrationsByStudentId(studentId);
        return ResponseEntity.ok(registrations != null ? registrations : List.of());
    }

    @GetMapping("/{exam_id}/isRegistered/{student_id}")
    public ResponseEntity<RegistrationStatusResponse> isRegistered(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId,
            HttpServletRequest request) {
        AuthGuard.requireSelfOrStaff(request, studentId);
        boolean registered = examService.isStudentRegisteredForExam(studentId, examId);
        return ResponseEntity.ok(RegistrationStatusResponse.builder()
                .isRegistered(registered)
                .examId(examId)
                .studentId(studentId)
                .build());
    }

    @GetMapping("/{exam_id}/registeredStudents")
    public ResponseEntity<List<Student>> getRegisteredStudents(@PathVariable("exam_id") Integer examId,
            HttpServletRequest request) {
        AuthGuard.requireExamAccess(request, requireExam(examId));
        List<Student> students = examService.getRegisteredStudentsByExamId(examId);
        return ResponseEntity.ok(students != null ? students : List.of());
    }

    @GetMapping("/{exam_id}/registrationCount")
    public ResponseEntity<RegistrationCountResponse> getRegistrationCount(@PathVariable("exam_id") Integer examId,
            HttpServletRequest request) {
        AuthGuard.requireExamAccess(request, requireExam(examId));
        int count = examService.getRegistrationCountByExamId(examId);
        return ResponseEntity.ok(RegistrationCountResponse.builder()
                .examId(examId)
                .registrationCount(count)
                .build());
    }

    @DeleteMapping("/{exam_id}/unregister/{student_id}")
    public ResponseEntity<ApiResponse> unregisterStudent(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId,
            HttpServletRequest request) {
        AuthGuard.requireSelfOrStaff(request, studentId);
        examService.unregisterStudentFromExam(studentId, examId);
        return ResponseEntity.ok(new ApiResponse(true, "Unregistered from exam successfully."));
    }

    @GetMapping("/{exam_id}/submissions")
    public ResponseEntity<List<ExamSubmissionSummaryDTO>> listSubmissions(@PathVariable("exam_id") Integer examId,
            HttpServletRequest request) {
        AuthGuard.requireExamAccess(request, requireExam(examId));
        List<ExamSubmissionSummaryDTO> submissions = examSubmissionService.listSubmissions(examId);
        return ResponseEntity.ok(submissions != null ? submissions : List.of());
    }

    @GetMapping("/{exam_id}/attempts/{attempt_id}/evaluation")
    public ResponseEntity<List<EvaluationItemDTO>> getEvaluationItems(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            HttpServletRequest request) {
        AuthGuard.requireExamAccess(request, requireExam(examId));
        return ResponseEntity.ok(examSubmissionService.getEvaluationItems(examId, attemptId));
    }

    @PutMapping("/{exam_id}/attempts/{attempt_id}/answers/{question_id}/marks")
    public ResponseEntity<ExamResultDTO> awardAnswerMarks(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            @PathVariable("question_id") Integer questionId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        AuthGuard.requireRole(request, RoleConstants.ROLE_TEACHER, RoleConstants.ROLE_ADMIN);
        AuthGuard.requireExamAccess(request, requireExam(examId));

        // Marks are whole numbers everywhere: `question.marks`, `attempt_answer.marks_obtained`
        // and `result.marks_obtained` are INT columns, and the result is scored with integer
        // arithmetic. Parsing to a double and rounding would turn a fractional mark into a
        // different mark without telling anyone, so a fractional value is rejected instead.
        int marks = RequestValueParser.parseInt(body == null ? null : body.get("marks"), "marks");

        return ResponseEntity.ok(examSubmissionService.awardAnswerMarks(examId, attemptId, questionId, marks));
    }

    private Exam requireExam(Integer examId) {
        if (examId == null || examId <= 0) {
            throw new BadRequestException("Invalid examId");
        }
        return examService.getExamById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
    }

    private void requireStudentVisibleExam(Exam exam) {
        boolean visible = ExamConstants.isOpenForRegistration(exam.getStatus())
                || ExamConstants.EXAM_STATUS_COMPLETED.equalsIgnoreCase(exam.getStatus());
        if (!visible) {
            throw new ResourceNotFoundException("Exam not found");
        }
    }
}
