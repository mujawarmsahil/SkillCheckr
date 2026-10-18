package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.QuestionRepository;
import com.skillcheckr.validation.ExamCreationValidator;
import com.skillcheckr.validation.QuestionValidator;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionPaperMutationPolicy questionPaperMutationPolicy;

    @Autowired(required = false)
    private Clock clock;

    @Autowired
    public QuestionServiceImpl(QuestionRepository questionRepository,
            ExamQuestionRepository examQuestionRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            QuestionPaperMutationPolicy questionPaperMutationPolicy) {
        this.questionRepository = questionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.questionPaperMutationPolicy = questionPaperMutationPolicy;
    }

    @Override
    @Transactional
    public void saveQuestionsWithAnswers(List<QuestionDTO> questions) {
        QuestionValidator.validate(questions);
        Set<Integer> examIds = new TreeSet<>();
        for (QuestionDTO question : questions) {
            if (question != null && question.getExamId() != null && question.getExamId() > 0) {
                examIds.add(question.getExamId());
            }
        }
        for (int examId : examIds) {
            Exam exam = questionPaperMutationPolicy.lockAndValidate(examId);
            requirePendingPaper(exam);
        }
        questionRepository.saveQuestionWithAnswers(questions);
    }

    @Override
    public List<QuestionDTO> getAllQuestions() {
        return questionRepository.getAllQuestions();
    }

    @Override
    public Optional<QuestionDTO> getQuestionById(int questionId) {
        return questionRepository.getQuestionById(questionId);
    }

    @Override
    public List<QuestionDTO> getQuestionsBySubjectId(int subjectId) {
        return questionRepository.getQuestionsBySubjectId(subjectId);
    }

    @Override
    public List<QuestionDTO> getQuestionsByExamId(int examId) {
        return questionRepository.getQuestionsByExamId(examId);
    }

    @Override
    public List<QuestionDTO> getStudentQuestionsByExamId(int examId) {
        List<QuestionDTO> questions = new ArrayList<>();
        for (QuestionDTO question : questionRepository.getQuestionsByExamId(examId)) {
            questions.add(withoutAnswerKey(question));
        }
        return questions;
    }

    /**
     * A student may see the question, the options and the marks, never the key or the
     * reference answer while the exam can still be taken.
     */
    private QuestionDTO withoutAnswerKey(QuestionDTO question) {
        question.setCorrectOption(null);
        question.setSampleAnswer(null);
        return question;
    }

    @Override
    @Transactional
    public int attachQuestionsToExam(int examId, List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BadRequestException("Select at least one question to add to the exam");
        }

        Set<Integer> distinctQuestionIds = new java.util.TreeSet<>();
        for (Integer questionId : questionIds) {
            if (questionId != null && questionId > 0) {
                distinctQuestionIds.add(questionId);
            }
        }
        for (int questionId : distinctQuestionIds) {
            questionRepository.lockQuestionForUpdate(questionId);
        }

        Exam exam = questionPaperMutationPolicy.lockAndValidate(examId);
        requirePendingPaper(exam);
        try {
            LocalDateTime now = clock == null ? LocalDateTime.now() : LocalDateTime.now(clock);
            if (now.isAfter(ExamCreationValidator.resolveWindowStart(exam))) {
                throw new ExamInUseException("The exam has already started and its question paper is locked");
            }
        } catch (ExamInUseException | BadRequestException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BadRequestException("The exam schedule could not be read");
        }

        Set<Integer> distinctQuestionIdsForAttach = new LinkedHashSet<>();
        int attached = 0;
        int order = examQuestionRepository.getNextQuestionOrder(examId);
        for (Integer questionId : questionIds) {
            if (questionId == null || questionId <= 0 || !distinctQuestionIdsForAttach.add(questionId)) {
                continue;
            }
            Question question = questionRepository.findQuestionDetailsById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question " + questionId + " does not exist"));
            if (question.getSubject() == null || question.getSubject().getSubjectId() != exam.getSubject().getSubjectId()) {
                throw new BadRequestException("Question " + questionId + " belongs to a different subject than the exam");
            }
            if (examQuestionRepository.attachQuestion(examId, questionId, order)) {
                attached++;
                order++;
            }
        }
        return attached;
    }

    @Override
    @Transactional
    public boolean updateQuestion(QuestionDTO question) {
        QuestionValidator.validate(question);

        questionRepository.lockQuestionForUpdate(question.getQuestionId());
        Set<Integer> assignedExamIds = new TreeSet<>(
                examQuestionRepository.findExamIdsByQuestionId(question.getQuestionId()));
        for (Integer examId : assignedExamIds) {
            questionPaperMutationPolicy.lockAndValidate(examId);
        }

        if (attemptAnswerRepository.hasAttemptHistory(question.getQuestionId())) {
            throw new ExamInUseException(
                    "This question has already been attempted and cannot be edited.");
        }

        if (question.getSubjectId() > 0
                && examQuestionRepository.isQuestionAssignedToAnyExam(question.getQuestionId())) {
            for (int examSubjectId : examQuestionRepository.findSubjectIdsByQuestionId(question.getQuestionId())) {
                if (examSubjectId != question.getSubjectId()) {
                    throw new BadRequestException(
                            "This question is attached to an exam with a different subject and cannot be moved.");
                }
            }
        }

        return questionRepository.updateQuestion(question);
    }

    @Override
    @Transactional
    public boolean deleteQuestionById(int questionId) {
        questionRepository.lockQuestionForUpdate(questionId);
        Set<Integer> assignedExamIds = new TreeSet<>(examQuestionRepository.findExamIdsByQuestionId(questionId));
        for (Integer examId : assignedExamIds) {
            questionPaperMutationPolicy.lockAndValidate(examId);
        }
        if (attemptAnswerRepository.hasAttemptHistory(questionId)) {
            throw new ExamInUseException("This question has student-answer history and cannot be deleted.");
        }
        if (examQuestionRepository.isQuestionAssignedToAnyExam(questionId)) {
            throw new ExamInUseException(
                    "This question is used by an exam and cannot be deleted. Delete the exam that uses it first.");
        }
        return questionRepository.deleteQuestionById(questionId);
    }

    private void requirePendingPaper(Exam exam) {
        if (!ExamConstants.EXAM_STATUS_PENDING.equalsIgnoreCase(exam.getStatus())) {
            throw new BadRequestException("Questions can only be changed while the exam is pending approval");
        }
    }
}
