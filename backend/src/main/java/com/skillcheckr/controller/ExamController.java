package com.skillcheckr.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Subject;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;

@RestController
@RequestMapping({"/api/Exams", "/api/exams"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"})
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
        return ResponseEntity.ok(list != null ? list : List.of());
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

    @PostMapping({"/upComingExamStatus/{exam_id}", "/accept/{exam_id}"})
    @PutMapping("/status/{exam_id}")
    public ResponseEntity<String> acceptExam(@PathVariable("exam_id") Integer examId) {
        if (examService.acceptExam(examId)) {
            return ResponseEntity.ok("Accepted");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exam not found.");
    }

    @GetMapping({"/viewAllUpComingExam", "/upcoming"})
    public ResponseEntity<?> viewAllUpcomingExam() {
        List<Exam> list = examService.viewAllUpcomingExam();
        return ResponseEntity.ok(list != null ? list : List.of());
    }

    @GetMapping({"/viewAllCompletedExam", "/completed"})
    public ResponseEntity<?> viewAllCompletedExams() {
        List<Exam> list = examService.viewAllCompletedExam();
        return ResponseEntity.ok(list != null ? list : List.of());
    }
}