package com.skillcheckr.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ApiResponse;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamAttemptResponse;
import com.skillcheckr.model.AttemptAnswerRequest;
import com.skillcheckr.model.AttemptAnswerResponse;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.RegistrationCountResponse;
import com.skillcheckr.model.RegistrationStatusResponse;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.AuthService;
import com.skillcheckr.service.AttemptAnswerService;
import com.skillcheckr.service.ExamSubmissionService;
import com.skillcheckr.service.QuestionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/Exams", "/api/exams"})
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AttemptAnswerService attemptAnswerService;

    @Autowired
    private ExamSubmissionService examSubmissionService;

    @PostMapping({"/addExams", ""})
    public ResponseEntity<Subject> addExams(@RequestBody Exam exam) {
        Subject subject = examService.saveExam(exam);
        if (subject == null) {
            throw new IllegalStateException("Unable to add exam.");
        }
        return ResponseEntity.ok(subject);
    }

    @GetMapping({"/viewAllExams", ""})
    public ResponseEntity<List<Exam>> getAllExams() {
        List<Exam> exams = examService.getAllExams();
        if (exams == null || exams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(exams);
    }

    @GetMapping("/{exam_id}")
    public ResponseEntity<Exam> getExamById(@PathVariable("exam_id") Integer examId) {
        Exam exam = examService.getExamById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
        return ResponseEntity.ok(exam);
    }

    @PostMapping("/{exam_id}/attempts")
    public ResponseEntity<ExamAttemptResponse> startAttempt(
            @PathVariable("exam_id") Integer examId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (examId == null || examId <= 0) {
            throw new BadRequestException("Invalid examId");
        }

        int studentId = authService.getStudentIdFromAuthorization(authorizationHeader);
        AttemptStartResult result = examService.startAttempt(examId, studentId);
        ExamAttempt attempt = result.getAttempt();
        ExamAttemptResponse response = ExamAttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .examId(examId)
                .startedAt(attempt.getStartedAt())
                .expiresAt(attempt.getExpiresAt())
                .status(attempt.getStatus())
                .build();
        return ResponseEntity.status(result.isExisting() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{exam_id}/attempts/{attempt_id}/answers/{question_id}")
    public ResponseEntity<AttemptAnswerResponse> saveAttemptAnswer(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            @PathVariable("question_id") Integer questionId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) AttemptAnswerRequest request) {
        if (examId == null || examId <= 0 || attemptId == null || attemptId <= 0
                || questionId == null || questionId <= 0) {
            throw new BadRequestException("Invalid examId, attemptId, or questionId");
        }
        int studentId = authService.getStudentIdFromAuthorization(authorizationHeader);
        AttemptAnswerResponse response = attemptAnswerService.saveAnswer(
                examId, attemptId, questionId, studentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{exam_id}/attempts/{attempt_id}/answers")
    public ResponseEntity<List<AttemptAnswerResponse>> getAttemptAnswers(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (examId == null || examId <= 0 || attemptId == null || attemptId <= 0) {
            throw new BadRequestException("Invalid examId or attemptId");
        }
        int studentId = authService.getStudentIdFromAuthorization(authorizationHeader);
        return ResponseEntity.ok(attemptAnswerService.getAnswers(examId, attemptId, studentId));
    }

    @PostMapping("/{exam_id}/attempts/{attempt_id}/submit")
    public ResponseEntity<ExamResultDTO> submitAttempt(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("attempt_id") Integer attemptId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (examId == null || examId <= 0 || attemptId == null || attemptId <= 0) {
            throw new BadRequestException("Invalid examId or attemptId");
        }
        int studentId = authService.getStudentIdFromAuthorization(authorizationHeader);
        return ResponseEntity.ok(examSubmissionService.submit(examId, attemptId, studentId));
    }

    @GetMapping("/teacher/{teacher_id}")
    public ResponseEntity<List<Exam>> getExamsByTeacherId(@PathVariable("teacher_id") Integer teacherId) {
        List<Exam> exams = examService.getExamsByTeacherId(teacherId);
        return ResponseEntity.ok(exams != null ? exams : List.of());
    }

    @GetMapping({"/{exam_id}/questions", "/questions/{exam_id}"})
    public ResponseEntity<List<QuestionDTO>> getQuestionsForExam(@PathVariable("exam_id") Integer examId) {
        List<QuestionDTO> questions = questionService.getQuestionsByExamId(examId);
        return ResponseEntity.ok(questions != null ? questions : List.of());
    }

    @DeleteMapping({"/deleteExamById/{exam_id}", "/{exam_id}"})
    public ResponseEntity<String> deleteExam(@PathVariable("exam_id") Integer examId) {
        boolean deleted = examService.deleteExamById(examId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam not found or could not be deleted.");
        }
        return ResponseEntity.ok("Exam deleted successfully.");
    }

    @PostMapping({"/upComingExamStatus/{exam_id}", "/accept/{exam_id}", "/approve/{exam_id}", "/{exam_id}/approve"})
    public ResponseEntity<String> acceptExam(@PathVariable("exam_id") Integer examId) {
        if (!examService.acceptExam(examId)) {
            throw new ResourceNotFoundException("Exam not found");
        }
        return ResponseEntity.ok("Accepted");
    }

    @PostMapping({"/reject/{exam_id}", "/{exam_id}/reject"})
    public ResponseEntity<ApiResponse> rejectExam(@PathVariable("exam_id") Integer examId) {
        if (!examService.updateExamStatus(examId, ExamConstants.EXAM_STATUS_REJECTED)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Exam not found"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam rejected successfully"));
    }

    @PostMapping({"/cancel/{exam_id}", "/{exam_id}/cancel"})
    public ResponseEntity<ApiResponse> cancelExam(@PathVariable("exam_id") Integer examId) {
        if (!examService.updateExamStatus(examId, ExamConstants.EXAM_STATUS_CANCELLED)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Exam not found"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam cancelled successfully"));
    }

    @PutMapping({"/status/{exam_id}", "/{exam_id}/status"})
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable("exam_id") Integer examId,
            @RequestBody(required = false) Map<String, String> body) {
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : ExamConstants.EXAM_STATUS_UPCOMING;
        if (!examService.updateExamStatus(examId, status)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Exam not found or status update failed"));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Exam status updated to " + status));
    }

    @GetMapping({"/viewAllUpComingExam", "/upcoming"})
    public ResponseEntity<List<Exam>> getAllUpcomingExams() {
        List<Exam> exams = examService.getAllUpcomingExams();
        return ResponseEntity.ok(exams != null ? exams : List.of());
    }

    @GetMapping({"/viewAllCompletedExam", "/completed"})
    public ResponseEntity<List<Exam>> getAllCompletedExams() {
        List<Exam> exams = examService.getAllCompletedExams();
        if (exams == null || exams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(exams);
    }


    @PostMapping({"/register", "/{exam_id}/register", "/{exam_id}/register/{student_id}"})
    public ResponseEntity<ApiResponse> registerForExam(
            @PathVariable(value = "exam_id", required = false) Integer pathExamId,
            @PathVariable(value = "student_id", required = false) Integer pathStudentId,
            @RequestBody(required = false) Map<String, Object> body) {

        Integer examId = pathExamId;
        Integer studentId = pathStudentId;

        if (body != null) {
            if (examId == null && body.containsKey("examId")) {
                examId = Integer.parseInt(body.get("examId").toString());
            } else if (examId == null && body.containsKey("exam_id")) {
                examId = Integer.parseInt(body.get("exam_id").toString());
            }
            if (studentId == null && body.containsKey("studentId")) {
                studentId = Integer.parseInt(body.get("studentId").toString());
            } else if (studentId == null && body.containsKey("student_id")) {
                studentId = Integer.parseInt(body.get("student_id").toString());
            }
        }

        if (examId == null || studentId == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Both examId and studentId are required."));
        }

        Exam exam = examService.getExamById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found."));

        try {
            if (exam.getDate() != null && !exam.getDate().isEmpty()) {
                String rawDate = exam.getDate().trim();
                String datePart = rawDate.contains(" ") ? rawDate.split(" ")[0] : (rawDate.contains("T") ? rawDate.split("T")[0] : rawDate);
                LocalDate date = LocalDate.parse(datePart);
                LocalTime startTime = exam.getStartTime() != null ? exam.getStartTime() : LocalTime.of(0, 0);
                LocalDateTime startDateTime = LocalDateTime.of(date, startTime);

                if (LocalDateTime.now().isAfter(startDateTime)) {
                    return ResponseEntity.badRequest().body(new ApiResponse(
                            false,
                            "Registration closed: the deadline for this exam has passed."
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Could not parse the exam date for the registration deadline check", e);
        }

        boolean registered = examService.registerStudentForExam(studentId, examId);
        if (registered) {
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Registered for " + exam.getExamName() + ". You may attend when the exam window starts."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(
                    false,
                    "Failed to register for exam."
            ));
        }
    }

    @GetMapping("/registrations/student/{student_id}")
    public ResponseEntity<List<Integer>> getRegistrationsForStudent(@PathVariable("student_id") Integer studentId) {
        List<Integer> registeredExamIds = examService.getRegisteredExamIdsForStudent(studentId);
        return ResponseEntity.ok(registeredExamIds != null ? registeredExamIds : List.of());
    }

    @GetMapping("/registrations/student/{student_id}/detailed")
    public ResponseEntity<List<ExamRegistration>> getDetailedRegistrationsForStudent(@PathVariable("student_id") Integer studentId) {
        List<ExamRegistration> registrations = examService.getRegistrationsByStudentId(studentId);
        return ResponseEntity.ok(registrations != null ? registrations : List.of());
    }

    @GetMapping({"/{exam_id}/isRegistered/{student_id}", "/isRegistered/{exam_id}/{student_id}"})
    public ResponseEntity<RegistrationStatusResponse> isRegistered(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId) {
        boolean registered = examService.isStudentRegisteredForExam(studentId, examId);
        return ResponseEntity.ok(RegistrationStatusResponse.builder()
                .isRegistered(registered)
                .examId(examId)
                .studentId(studentId)
                .build());
    }

    @GetMapping("/{exam_id}/registeredStudents")
    public ResponseEntity<List<Student>> getRegisteredStudents(@PathVariable("exam_id") Integer examId) {
        List<Student> students = examService.getRegisteredStudentsByExamId(examId);
        return ResponseEntity.ok(students != null ? students : List.of());
    }

    @GetMapping("/{exam_id}/registrationCount")
    public ResponseEntity<RegistrationCountResponse> getRegistrationCount(@PathVariable("exam_id") Integer examId) {
        int count = examService.getRegistrationCountByExamId(examId);
        return ResponseEntity.ok(RegistrationCountResponse.builder()
                .examId(examId)
                .registrationCount(count)
                .build());
    }

    @DeleteMapping({"/{exam_id}/unregister/{student_id}", "/unregister/{exam_id}/{student_id}"})
    public ResponseEntity<ApiResponse> unregisterStudent(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId) {
        boolean success = examService.unregisterStudentFromExam(studentId, examId);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Unregistered from exam successfully."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, "Registration not found."));
    }
}