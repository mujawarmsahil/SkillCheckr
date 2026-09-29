package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.ExamSubmissionException;
import com.skillcheckr.model.AttemptAnswer;
import com.skillcheckr.model.EvaluationItemDTO;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamQuestion;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionSummaryDTO;
import com.skillcheckr.model.Question;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.ResultRepository;
import com.skillcheckr.validation.ExamAttemptValidator;

@Service
public class ExamSubmissionServiceImpl implements ExamSubmissionService {

    private static final DateTimeFormatter SUBMITTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ResultRepository resultRepository;
    private final QuestionBreakdownService questionBreakdownService;

    public ExamSubmissionServiceImpl(ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamQuestionRepository examQuestionRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            ResultRepository resultRepository,
            QuestionBreakdownService questionBreakdownService) {
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.resultRepository = resultRepository;
        this.questionBreakdownService = questionBreakdownService;
    }

    @Override
    @Transactional
    public ExamResultDTO submit(int examId, int attemptId, int studentId) {
        ExamAttempt attempt = examAttemptRepository.findByIdForUpdate(attemptId);
        ExamAttemptValidator.validateSubmitAttempt(attempt, examId, studentId);

        if (ExamConstants.ATTEMPT_STATUS_SUBMITTED.equalsIgnoreCase(attempt.getStatus())) {
            // Submission is idempotent: a repeated submit returns the stored result.
            return resultRepository.findByAttemptId(attemptId)
                    .map(result -> withBreakdown(result, examId, attemptId))
                    .orElseThrow(() -> new ExamSubmissionException(409, "Exam attempt has already been submitted"));
        }
        ExamAttemptValidator.validateAttemptCanSubmit(attempt);

        Exam exam = requireExam(examId);
        LocalDateTime submittedAt = LocalDateTime.now();

        // Multiple choice answers are graded here, descriptive answers stay pending until
        // a teacher awards marks for them.
        int marksObtained = gradeObjectiveAnswers(examId, attemptId, true);

        String status = hasPendingEvaluation(examId, attemptId)
                ? ExamConstants.RESULT_STATUS_SUBMITTED_FOR_EVALUATION
                : statusForMarks(marksObtained, exam.getPassingMarks());

        ExamResultDTO result = ExamResultDTO.builder()
                .examId(examId)
                .attemptId(attemptId)
                .studentId(studentId)
                .examName(exam.getExamName())
                .examType(exam.getExamType())
                .subjectId(exam.getSubject() == null ? 0 : exam.getSubject().getSubjectId())
                .subjectName(exam.getSubject() == null ? null : exam.getSubject().getSubjectName())
                .marksObtained(marksObtained)
                .totalMarks(Math.max(0, exam.getTotalMarks()))
                .passingMarks(Math.max(0, exam.getPassingMarks()))
                .percentage(percentage(marksObtained, exam.getTotalMarks()))
                .status(status)
                .submittedAt(submittedAt.format(SUBMITTED_AT_FORMAT))
                .build();

        ExamResultDTO savedResult = resultRepository.insertSubmissionResult(result);
        if (savedResult == null || savedResult.getResultId() <= 0) {
            throw new ExamSubmissionException(500, "Unable to save exam result");
        }
        if (!examAttemptRepository.finalizeAttempt(attemptId, submittedAt)) {
            throw new ExamSubmissionException(500, "Unable to finalize exam attempt");
        }
        return withBreakdown(savedResult, examId, attemptId);
    }

    /**
     * The stored row only carries the score, so the per question review the client renders is
     * always re-derived from the saved attempt answers.
     */
    private ExamResultDTO withBreakdown(ExamResultDTO result, int examId, int attemptId) {
        if (result != null) {
            result.setQuestionBreakdown(questionBreakdownService.build(examId, attemptId));
        }
        return result;
    }

    @Override
    public List<ExamSubmissionSummaryDTO> listSubmissions(int examId) {
        requireExam(examId);
        List<ExamSubmissionSummaryDTO> submissions = new ArrayList<>();
        for (ExamSubmissionSummaryDTO summary : resultRepository.findSubmissionSummariesByExamId(examId)) {
            summary.setPendingEvaluation(hasPendingEvaluation(examId, summary.getAttemptId()));
            submissions.add(summary);
        }
        return submissions;
    }

    @Override
    public List<EvaluationItemDTO> getEvaluationItems(int examId, int attemptId) {
        requireExam(examId);
        ExamAttempt attempt = examAttemptRepository.findById(attemptId);
        if (attempt == null || attempt.getExam() == null || attempt.getExam().getExamId() != examId) {
            throw new ExamSubmissionException(404, "Exam attempt not found for this exam");
        }
        if (!ExamConstants.ATTEMPT_STATUS_SUBMITTED.equalsIgnoreCase(attempt.getStatus())) {
            throw new ExamSubmissionException(409, "This attempt has not been submitted yet");
        }

        Map<Integer, AttemptAnswer> answers = answersByQuestion(attemptId);
        List<EvaluationItemDTO> items = new ArrayList<>();
        for (ExamQuestion assignment : examQuestionRepository.findByExamId(examId)) {
            Question question = assignment.getQuestion();
            if (question == null) {
                continue;
            }
            AttemptAnswer answer = answers.get(question.getQuestionId());
            boolean isMcq = ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(question.getQuestionType());
            items.add(EvaluationItemDTO.builder()
                    .questionId(question.getQuestionId())
                    .questionOrder(assignment.getQuestionOrder())
                    .question(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .marks(question.getMarks())
                    .wordLimit(question.getWordLimit())
                    .textAnswer(answer == null ? null : answer.getTextAnswer())
                    .selectedAnswer(answer == null || answer.getSelectedAnswer() == null
                            ? null
                            : answer.getSelectedAnswer().getOptionText())
                    .awardedMarks(answer == null ? null : answer.getMarksObtained())
                    .evaluated(answer != null && answer.getMarksObtained() != null)
                    .requiresEvaluation(!isMcq)
                    .build());
        }
        return items;
    }

    @Override
    @Transactional
    public ExamResultDTO awardAnswerMarks(int examId, int attemptId, int questionId, int marks) {
        requireExam(examId);
        ExamAttempt attempt = examAttemptRepository.findById(attemptId);
        if (attempt == null || attempt.getExam() == null || attempt.getExam().getExamId() != examId) {
            throw new ExamSubmissionException(404, "Exam attempt not found for this exam");
        }
        if (!ExamConstants.ATTEMPT_STATUS_SUBMITTED.equalsIgnoreCase(attempt.getStatus())) {
            throw new ExamSubmissionException(409, "Only a submitted attempt can be evaluated");
        }

        Question question = findAssignedQuestion(examId, questionId);
        if (ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(question.getQuestionType())) {
            throw new ExamSubmissionException(400, "Multiple choice answers are graded automatically");
        }
        if (marks < 0 || marks > question.getMarks()) {
            throw new ExamSubmissionException(400,
                    "Marks must be between 0 and " + question.getMarks());
        }
        if (!answersByQuestion(attemptId).containsKey(questionId)) {
            // A question the student never answered has no answer row yet. Record the awarded
            // marks anyway, otherwise the attempt could never leave "Submitted for Evaluation".
            attemptAnswerRepository.saveAndReturn(AttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .marksObtained(marks)
                    .build());
        } else {
            attemptAnswerRepository.updateMarks(attemptId, questionId, marks);
        }

        Exam exam = requireExam(examId);
        int marksObtained = gradeObjectiveAnswers(examId, attemptId, false);
        marksObtained += awardedDescriptiveMarks(attemptId, examId);
        String status = hasPendingEvaluation(examId, attemptId)
                ? ExamConstants.RESULT_STATUS_SUBMITTED_FOR_EVALUATION
                : statusForMarks(marksObtained, exam.getPassingMarks());

        resultRepository.updateResultAfterEvaluation(attemptId, marksObtained, Math.max(0, exam.getTotalMarks()),
                Math.max(0, exam.getPassingMarks()), percentage(marksObtained, exam.getTotalMarks()), status);

        return resultRepository.findByAttemptId(attemptId)
                .map(result -> {
                    result.setQuestionBreakdown(questionBreakdownService.build(examId, attemptId));
                    return result;
                })
                .orElseThrow(() -> new ExamSubmissionException(404, "Result not found for this attempt"));
    }

    private Exam requireExam(int examId) {
        return examRepository.getExamById(examId)
                .orElseThrow(() -> new ExamSubmissionException(404, "Exam not found"));
    }

    private Question findAssignedQuestion(int examId, int questionId) {
        for (ExamQuestion assignment : examQuestionRepository.findByExamId(examId)) {
            Question question = assignment.getQuestion();
            if (question != null && question.getQuestionId() == questionId) {
                return question;
            }
        }
        throw new ExamSubmissionException(404, "Question " + questionId + " is not assigned to this exam");
    }

    private Map<Integer, AttemptAnswer> answersByQuestion(int attemptId) {
        Map<Integer, AttemptAnswer> answers = new HashMap<>();
        for (AttemptAnswer answer : attemptAnswerRepository.findByAttemptId(attemptId)) {
            if (answer.getQuestion() != null) {
                answers.put(answer.getQuestion().getQuestionId(), answer);
            }
        }
        return answers;
    }

    /**
     * Persists the automatic score of every multiple choice question.
     *
     * @param resetPending when true the descriptive answers are returned to "not yet
     *                     evaluated" so a new submission is never scored as if it had
     *                     been evaluated already
     */
    private int gradeObjectiveAnswers(int examId, int attemptId, boolean resetPending) {
        Map<Integer, AttemptAnswer> answers = answersByQuestion(attemptId);
        int marksObtained = 0;
        for (ExamQuestion assignment : examQuestionRepository.findByExamId(examId)) {
            Question question = assignment.getQuestion();
            if (question == null) {
                continue;
            }
            if (ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(question.getQuestionType())) {
                AttemptAnswer answer = answers.get(question.getQuestionId());
                int awarded = answer != null && answer.getSelectedAnswer() != null
                        && answer.getSelectedAnswer().isCorrect()
                        ? Math.max(0, question.getMarks())
                        : 0;
                attemptAnswerRepository.updateMarks(attemptId, question.getQuestionId(), awarded);
                marksObtained += awarded;
            } else if (resetPending && answers.containsKey(question.getQuestionId())) {
                attemptAnswerRepository.updateMarks(attemptId, question.getQuestionId(), null);
            }
        }
        return marksObtained;
    }

    private int awardedDescriptiveMarks(int attemptId, int examId) {
        List<Integer> descriptiveQuestionIds = new ArrayList<>();
        for (ExamQuestion assignment : examQuestionRepository.findByExamId(examId)) {
            if (assignment.getQuestion() != null
                    && !ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(assignment.getQuestion().getQuestionType())) {
                descriptiveQuestionIds.add(assignment.getQuestion().getQuestionId());
            }
        }

        int awarded = 0;
        for (AttemptAnswer answer : answersByQuestion(attemptId).values()) {
            if (descriptiveQuestionIds.contains(answer.getQuestion().getQuestionId())
                    && answer.getMarksObtained() != null) {
                awarded += answer.getMarksObtained();
            }
        }
        return awarded;
    }

    /**
     * A submission is pending while any assigned descriptive question still has no
     * awarded marks.
     */
    private boolean hasPendingEvaluation(int examId, int attemptId) {
        List<Integer> descriptiveQuestionIds = new ArrayList<>();
        for (ExamQuestion assignment : examQuestionRepository.findByExamId(examId)) {
            if (assignment.getQuestion() != null
                    && !ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(assignment.getQuestion().getQuestionType())) {
                descriptiveQuestionIds.add(assignment.getQuestion().getQuestionId());
            }
        }
        if (descriptiveQuestionIds.isEmpty()) {
            return false;
        }

        Map<Integer, AttemptAnswer> answers = answersByQuestion(attemptId);
        for (Integer questionId : descriptiveQuestionIds) {
            AttemptAnswer answer = answers.get(questionId);
            if (answer == null || answer.getMarksObtained() == null) {
                return true;
            }
        }
        return false;
    }

    private String statusForMarks(int marksObtained, int passingMarks) {
        return marksObtained >= Math.max(0, passingMarks)
                ? ExamConstants.RESULT_STATUS_PASS
                : ExamConstants.RESULT_STATUS_FAIL;
    }

    private double percentage(int marksObtained, int totalMarks) {
        if (totalMarks <= 0) {
            return 0.0;
        }
        return Math.round(((double) marksObtained / totalMarks) * 10000.0) / 100.0;
    }
}
