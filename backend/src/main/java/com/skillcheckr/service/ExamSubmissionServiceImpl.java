package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.ResultRepository;
import com.skillcheckr.validation.ExamAttemptValidator;

@Service
public class ExamSubmissionServiceImpl implements ExamSubmissionService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ResultRepository resultRepository;

    public ExamSubmissionServiceImpl(ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamQuestionRepository examQuestionRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            ResultRepository resultRepository) {
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional
    public ExamResultDTO submit(int examId, int attemptId, int studentId) {
        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId);
        ExamAttemptValidator.validateSubmitAttempt(attempt, examId, studentId);

        if (ExamConstants.ATTEMPT_STATUS_SUBMITTED.equalsIgnoreCase(attempt.getStatus())) {
            ExamResultDTO existing = resultRepository.findByAttemptId(attemptId);
            if (existing != null) return existing;
            throw new ExamSubmissionException(409, "Exam attempt has already been submitted");
        }
        ExamAttemptValidator.validateAttemptCanSubmit(attempt);

        Exam exam = examRepository.getExamById(examId);
        if (exam == null) {
            throw new ExamSubmissionException(404, "Exam not found");
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        List<ExamQuestion> assignedQuestions = examQuestionRepository.findByExamId(examId);
        List<AttemptAnswer> persistedAnswers = attemptAnswerRepository.findByAttemptId(attemptId);
        Map<Integer, AttemptAnswer> answersByQuestion = new HashMap<>();
        for (AttemptAnswer answer : persistedAnswers) {
            if (answer.getQuestion() != null) {
                answersByQuestion.put(answer.getQuestion().getQuestionId(), answer);
            }
        }

        int marksObtained = 0;
        boolean hasPendingText = false;
        for (ExamQuestion assignment : assignedQuestions) {
            Question question = assignment.getQuestion();
            if (question == null) continue;
            AttemptAnswer answer = answersByQuestion.get(question.getQuestionId());
            if (ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(question.getQuestionType())) {
                int awarded = answer != null && answer.getSelectedAnswer() != null
                        && answer.getSelectedAnswer().isCorrect() ? Math.max(0, question.getMarks()) : 0;
                marksObtained += awarded;
                if (answer != null) {
                    attemptAnswerRepository.updateMarks(attemptId, question.getQuestionId(), awarded);
                }
            } else if (ExamConstants.QUESTION_TYPE_QUESTION_ANSWER.equalsIgnoreCase(question.getQuestionType())) {
                hasPendingText = true;
                if (answer != null) {
                    attemptAnswerRepository.updateMarks(attemptId, question.getQuestionId(), null);
                }
            }
        }

        int totalMarks = Math.max(0, exam.getTotalMarks());
        int passingMarks = Math.max(0, exam.getPassingMarks());
        double percentage = totalMarks == 0 ? 0.0 : ((double) marksObtained / totalMarks) * 100.0;
        String status = hasPendingText ? ExamConstants.RESULT_STATUS_SUBMITTED_FOR_EVALUATION
                : (marksObtained >= passingMarks ? ExamConstants.RESULT_STATUS_PASS : ExamConstants.RESULT_STATUS_FAIL);
        String submittedAtText = submittedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        ExamResultDTO result = ExamResultDTO.builder()
                .examId(examId)
                .attemptId(attemptId)
                .studentId(studentId)
                .examName(exam.getExamName())
                .examType(exam.getExamType())
                .subjectId(exam.getSubject() == null ? 0 : exam.getSubject().getSubjectId())
                .subjectName(exam.getSubject() == null ? null : exam.getSubject().getSubjectName())
                .marksObtained(marksObtained)
                .totalMarks(totalMarks)
                .passingMarks(passingMarks)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .status(status)
                .submittedAt(submittedAtText)
                .build();

        ExamResultDTO savedResult = resultRepository.insertSubmissionResult(result);
        if (savedResult == null || savedResult.getResultId() <= 0) {
            throw new ExamSubmissionException(500, "Unable to save exam result");
        }
        if (!examAttemptRepository.finalizeAttempt(attemptId, submittedAt)) {
            throw new ExamSubmissionException(500, "Unable to finalize exam attempt");
        }
        return savedResult;
    }
}
