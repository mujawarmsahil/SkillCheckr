package com.skillcheckr.validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.QuestionDTO;

/**
 * Question authoring rules. A question has to be answerable before it can be stored:
 * a multiple choice question needs distinct options and exactly one key, a descriptive
 * question needs a positive word limit when one is given.
 */
public final class QuestionValidator {

    public static final int MAX_QUESTION_LENGTH = 4000;
    public static final int MAX_OPTION_LENGTH = 500;
    public static final int MAX_SAMPLE_ANSWER_LENGTH = 5000;
    public static final int MAX_MARKS = 100;
    public static final int MAX_WORD_LIMIT = 5000;
    private static final int MIN_MCQ_OPTIONS = 2;
    private static final int MAX_MCQ_OPTIONS = 4;

    private QuestionValidator() {
    }

    public static void validate(List<QuestionDTO> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new BadRequestException("At least one question is required");
        }
        for (QuestionDTO question : questions) {
            validate(question);
        }
    }

    public static void validate(QuestionDTO question) {
        if (question == null) {
            throw new BadRequestException("Invalid question data");
        }

        String text = question.getQuestion() == null ? "" : question.getQuestion().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Question text is required");
        }
        if (text.length() > MAX_QUESTION_LENGTH) {
            throw new BadRequestException("Question text must be at most " + MAX_QUESTION_LENGTH + " characters");
        }

        String questionType = normalizeQuestionType(question.getQuestionType());
        question.setQuestionType(questionType);

        if (question.getSubjectId() <= 0) {
            throw new BadRequestException("A valid subject is required for every question");
        }
        if (question.getMarks() <= 0) {
            throw new BadRequestException("Question marks must be greater than 0");
        }
        if (question.getMarks() > MAX_MARKS) {
            throw new BadRequestException("Question marks must be at most " + MAX_MARKS);
        }
        if (question.getWordLimit() != null) {
            if (question.getWordLimit() <= 0 || question.getWordLimit() > MAX_WORD_LIMIT) {
                throw new BadRequestException("Word limit must be between 1 and " + MAX_WORD_LIMIT);
            }
        }

        if (ExamConstants.QUESTION_TYPE_MCQ.equals(questionType)) {
            validateOptions(question);
            question.setWordLimit(null);
        } else {
            rejectOptions(question);
            String sampleAnswer = question.getSampleAnswer() == null ? "" : question.getSampleAnswer().trim();
            if (sampleAnswer.length() > MAX_SAMPLE_ANSWER_LENGTH) {
                throw new BadRequestException(
                        "Reference answer must be at most " + MAX_SAMPLE_ANSWER_LENGTH + " characters");
            }
            question.setSampleAnswer(sampleAnswer.isEmpty() ? null : sampleAnswer);
        }
    }

    private static void validateOptions(QuestionDTO question) {
        List<String> options = new ArrayList<>();
        options.add(question.getOption1());
        options.add(question.getOption2());
        options.add(question.getOption3());
        options.add(question.getOption4());

        Set<String> distinct = new LinkedHashSet<>();
        int provided = 0;
        for (String option : options) {
            if (option == null || option.isBlank()) {
                continue;
            }
            String trimmed = option.trim();
            if (trimmed.length() > MAX_OPTION_LENGTH) {
                throw new BadRequestException("Each option must be at most " + MAX_OPTION_LENGTH + " characters");
            }
            provided++;
            if (!distinct.add(trimmed.toLowerCase())) {
                throw new BadRequestException("Options must be unique within a question");
            }
        }

        if (provided < MIN_MCQ_OPTIONS) {
            throw new BadRequestException("A multiple choice question needs at least "
                    + MIN_MCQ_OPTIONS + " options");
        }
        if (provided > MAX_MCQ_OPTIONS) {
            throw new BadRequestException("A multiple choice question supports at most "
                    + MAX_MCQ_OPTIONS + " options");
        }

        String correctOption = question.getCorrectOption() == null ? "" : question.getCorrectOption().trim();
        if (correctOption.isEmpty()) {
            throw new BadRequestException("Select the correct option for this question");
        }
        if (!distinct.contains(correctOption.toLowerCase())) {
            throw new BadRequestException("The correct option must be one of the question options");
        }
        question.setCorrectOption(correctOption);
    }

    private static void rejectOptions(QuestionDTO question) {
        if (notBlank(question.getOption1()) || notBlank(question.getOption2())
                || notBlank(question.getOption3()) || notBlank(question.getOption4())) {
            throw new BadRequestException("A descriptive question cannot have options");
        }
        question.setCorrectOption(null);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeQuestionType(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return ExamConstants.QUESTION_TYPE_MCQ;
        }
        String trimmed = questionType.trim();
        if (ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(trimmed)) {
            return ExamConstants.QUESTION_TYPE_MCQ;
        }
        if (ExamConstants.QUESTION_TYPE_QUESTION_ANSWER.equalsIgnoreCase(trimmed)) {
            return ExamConstants.QUESTION_TYPE_QUESTION_ANSWER;
        }
        throw new BadRequestException("'" + trimmed + "' is not a supported question type");
    }
}
