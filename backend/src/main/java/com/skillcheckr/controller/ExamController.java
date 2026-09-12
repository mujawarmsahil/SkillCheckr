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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;

@RestController
@RequestMapping({"/api/Exams", "/api/exams"})
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @PostMapping({"/addExams", ""})
    public ResponseEntity<?> addExams(@RequestBody Exam exam) {
        Subject subject = examService.saveExam(exam);
        if (subject != null) {
            return ResponseEntity.ok(subject);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to add exam.");
    }

    @GetMapping({"/viewAllExams", ""})
    public ResponseEntity<?> viewAllExams() {
        List<Exam> list = examService.viewAllExams();
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{exam_id}")
    public ResponseEntity<?> getExamById(@PathVariable("exam_id") Integer examId) {
        Exam exam = examService.getExamById(examId);
        if (exam != null) {
            return ResponseEntity.ok(exam);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam not found with id: " + examId);
    }

    @GetMapping("/teacher/{teacher_id}")
    public ResponseEntity<?> getExamsByTeacherId(@PathVariable("teacher_id") Integer teacherId) {
        List<Exam> list = examService.getExamsByTeacherId(teacherId);
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping({"/{exam_id}/questions", "/questions/{exam_id}"})
    public ResponseEntity<?> getQuestionsForExam(@PathVariable("exam_id") Integer examId) {
        List<QuestionDTO> questions = questionService.getQuestionsByExamId(examId);
        return ResponseEntity.ok(questions != null ? questions : List.of());
    }

    @DeleteMapping({"/deleteExamById/{exam_id}", "/{exam_id}"})
    public ResponseEntity<?> deleteExam(@PathVariable("exam_id") Integer examId) {
        boolean deleted = examService.deleteExamById(examId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam not found or could not be deleted.");
        }
        return ResponseEntity.ok("Exam deleted successfully.");
    }

    @PostMapping({"/upComingExamStatus/{exam_id}", "/accept/{exam_id}", "/approve/{exam_id}", "/{exam_id}/approve"})
    public ResponseEntity<?> acceptExam(@PathVariable("exam_id") Integer examId) {
        if (examService.acceptExam(examId)) {
            return ResponseEntity.ok("Accepted");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exam not found", "success", false));
    }

    @PostMapping({"/reject/{exam_id}", "/{exam_id}/reject"})
    public ResponseEntity<?> rejectExam(@PathVariable("exam_id") Integer examId) {
        if (examService.updateExamStatus(examId, "Rejected")) {
            return ResponseEntity.ok(Map.of("message", "Exam rejected successfully", "success", true));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exam not found", "success", false));
    }

    @PostMapping({"/cancel/{exam_id}", "/{exam_id}/cancel"})
    public ResponseEntity<?> cancelExam(@PathVariable("exam_id") Integer examId) {
        if (examService.updateExamStatus(examId, "Cancelled")) {
            return ResponseEntity.ok(Map.of("message", "Exam cancelled successfully", "success", true));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exam not found", "success", false));
    }

    @PutMapping({"/status/{exam_id}", "/{exam_id}/status"})
    public ResponseEntity<?> updateStatus(
            @PathVariable("exam_id") Integer examId,
            @RequestBody(required = false) Map<String, String> body) {
        String status = (body != null && body.get("status") != null && !body.get("status").trim().isEmpty())
                ? body.get("status").trim()
                : "Upcoming";
        if (examService.updateExamStatus(examId, status)) {
            return ResponseEntity.ok(Map.of("message", "Exam status updated to " + status, "success", true));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Exam not found or status update failed", "success", false));
    }

    @GetMapping({"/viewAllUpComingExam", "/upcoming"})
    public ResponseEntity<?> viewAllUpcomingExam() {
        List<Exam> list = examService.viewAllUpcomingExam();
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping({"/viewAllCompletedExam", "/completed"})
    public ResponseEntity<?> viewAllCompletedExams() {
        List<Exam> list = examService.viewAllCompletedExam();
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(list);
    }


    @PostMapping({"/register", "/{exam_id}/register", "/{exam_id}/register/{student_id}"})
    public ResponseEntity<?> registerForExam(
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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Both examId and studentId are required."));
        }

        Exam exam = examService.getExamById(examId);
        if (exam == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Exam not found."));
        }

        // Validate registration deadline (exam start date & time)
        try {
            if (exam.getDate() != null && !exam.getDate().isEmpty()) {
                String rawDate = exam.getDate().trim();
                String datePart = rawDate.contains(" ") ? rawDate.split(" ")[0] : (rawDate.contains("T") ? rawDate.split("T")[0] : rawDate);
                LocalDate date = LocalDate.parse(datePart);
                LocalTime startTime = exam.getStartTime() != null ? exam.getStartTime() : LocalTime.of(0, 0);
                LocalDateTime startDateTime = LocalDateTime.of(date, startTime);

                if (LocalDateTime.now().isAfter(startDateTime)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                            "success", false,
                            "message", "Registration closed: The registration deadline for this examination has passed."
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Registration deadline check parse error: " + e.getMessage());
        }

        boolean registered = examService.registerStudentForExam(studentId, examId);
        if (registered) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully registered for " + exam.getExamName() + "! You may attend when the exam window starts."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to register for exam. Please try again."
            ));
        }
    }

    @GetMapping("/registrations/student/{student_id}")
    public ResponseEntity<?> getRegistrationsForStudent(@PathVariable("student_id") Integer studentId) {
        List<Integer> registeredExamIds = examService.getRegisteredExamIdsForStudent(studentId);
        return ResponseEntity.ok(registeredExamIds != null ? registeredExamIds : List.of());
    }

    @GetMapping("/registrations/student/{student_id}/detailed")
    public ResponseEntity<?> getDetailedRegistrationsForStudent(@PathVariable("student_id") Integer studentId) {
        List<ExamRegistration> list = examService.getRegistrationsByStudentId(studentId);
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping({"/{exam_id}/isRegistered/{student_id}", "/isRegistered/{exam_id}/{student_id}"})
    public ResponseEntity<?> isRegistered(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId) {
        boolean registered = examService.isStudentRegisteredForExam(studentId, examId);
        return ResponseEntity.ok(Map.of("isRegistered", registered, "examId", examId, "studentId", studentId));
    }

    @GetMapping("/{exam_id}/registeredStudents")
    public ResponseEntity<?> getRegisteredStudents(@PathVariable("exam_id") Integer examId) {
        List<Student> students = examService.getRegisteredStudentsByExamId(examId);
        return ResponseEntity.ok(students != null ? students : List.of());
    }

    @GetMapping("/{exam_id}/registrationCount")
    public ResponseEntity<?> getRegistrationCount(@PathVariable("exam_id") Integer examId) {
        int count = examService.getRegistrationCountByExamId(examId);
        return ResponseEntity.ok(Map.of("examId", examId, "registrationCount", count));
    }

    @DeleteMapping({"/{exam_id}/unregister/{student_id}", "/unregister/{exam_id}/{student_id}"})
    public ResponseEntity<?> unregisterStudent(
            @PathVariable("exam_id") Integer examId,
            @PathVariable("student_id") Integer studentId) {
        boolean success = examService.unregisterStudentFromExam(studentId, examId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Unregistered from exam successfully."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Registration not found."));
    }
}