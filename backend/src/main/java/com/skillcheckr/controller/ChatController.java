package com.skillcheckr.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ForbiddenException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.ChatContext;
import com.skillcheckr.model.ChatRequest;
import com.skillcheckr.model.ChatResponse;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.security.AuthGuard;
import com.skillcheckr.security.AuthPrincipal;
import com.skillcheckr.service.AiChatService;
import com.skillcheckr.service.ExamService;
import com.skillcheckr.service.QuestionService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        AuthPrincipal principal = AuthGuard.requirePrincipal(httpRequest);
        ChatContext context = ChatContext.empty();
        if (request != null && request.getExamId() != null) {
            Exam exam = requireExam(request.getExamId());
            context.setExamId(exam.getExamId());
            context.setExamName(exam.getExamName());
            context.setSubjectName(exam.getSubject() != null ? exam.getSubject().getSubjectName() : null);
            context.setExamStatus(exam.getStatus());
            context.setExamType(exam.getExamType());
            context.setDurationMinutes(exam.getDurationMinutes());
            context.setTotalMarks(exam.getTotalMarks());
            context.setPassingMarks(exam.getPassingMarks());

            if (principal.isStudent()) {
                boolean openForRegistration = ExamConstants.isOpenForRegistration(exam.getStatus());
                boolean examCompleted = ExamConstants.EXAM_STATUS_COMPLETED.equalsIgnoreCase(exam.getStatus());
                if (!openForRegistration && !examCompleted) {
                    throw new ForbiddenException("You are not allowed to ask questions about this exam yet.");
                }
                attachQuestionContext(context, questionService.getStudentQuestionsByExamId(exam.getExamId()),
                        request.getQuestionId(), false);
            } else {
                AuthGuard.requireExamAccess(httpRequest, exam);
                attachQuestionContext(context, questionService.getQuestionsByExamId(exam.getExamId()),
                        request.getQuestionId(), true);
            }
        }
        ChatResponse response = aiChatService.chat(request, context, principal.getRole());
        return ResponseEntity.ok(response);
    }

    private Exam requireExam(Integer examId) {
        if (examId == null || examId <= 0) {
            throw new BadRequestException("Invalid examId");
        }
        return examService.getExamById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
    }

    private void attachQuestionContext(ChatContext context, List<QuestionDTO> questions, Integer questionId,
            boolean staff) {
        if (context.getExamId() == null || questionId == null) {
            return;
        }
        QuestionDTO question = questions == null ? null : questions.stream()
                .filter(candidate -> candidate != null && questionId.equals(candidate.getQuestionId()))
                .findFirst()
                .orElse(null);
        if (question == null) {
            return;
        }
        context.setQuestionId(question.getQuestionId());
        context.setQuestion(question.getQuestion());
        context.setQuestionType(question.getQuestionType());
        List<String> options = new ArrayList<>();
        options.add(question.getOption1());
        options.add(question.getOption2());
        options.add(question.getOption3());
        options.add(question.getOption4());
        context.setOptions(options);
        context.setMarks(question.getMarks());
        context.setWordLimit(question.getWordLimit());
        context.setSampleAnswer(staff ? question.getSampleAnswer() : null);
    }
}
