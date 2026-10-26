package com.skillcheckr.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Subject;
import com.skillcheckr.persistence.entity.QuestionEntity;
import com.skillcheckr.persistence.repository.AnswerJpaRepository;
import com.skillcheckr.persistence.repository.QuestionJpaRepository;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final QuestionJpaRepository questionRepository;
    private final AnswerJpaRepository answerRepository;

    public QuestionRepositoryImpl(JdbcTemplate jdbcTemplate,
            QuestionJpaRepository questionRepository, AnswerJpaRepository answerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @Override
    @Transactional
    public void saveQuestionWithAnswers(List<QuestionDTO> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
            QuestionDTO dto = questions.get(questionIndex);
            if (dto == null || dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
                throw new BadRequestException("Every question needs question text");
            }
            if (dto.getSubjectId() <= 0) {
                throw new BadRequestException("Every question needs a subject");
            }
            if (dto.getExamId() != null && dto.getExamId() > 0) {
                validateExamSubject(dto.getExamId(), dto.getSubjectId());
            }

            QuestionEntity entity = new QuestionEntity();
            entity.setSubjectId(dto.getSubjectId());
            entity.setQuestionText(dto.getQuestion().trim());
            entity.setQuestionType(dto.getQuestionType() == null || dto.getQuestionType().isBlank()
                    ? ExamConstants.QUESTION_TYPE_MCQ : dto.getQuestionType().trim());
            entity.setMarks(dto.getMarks() > 0 ? dto.getMarks() : 1);
            entity.setWordLimit(dto.getWordLimit());
            QuestionEntity saved = questionRepository.saveAndFlush(entity);
            int questionId = saved.getQuestionId();

            if (dto.getExamId() != null && dto.getExamId() > 0) {
                jdbcTemplate.update("INSERT INTO exam_question (exam_id, question_id, question_order) VALUES (?, ?, ?)",
                        dto.getExamId(), questionId, questionIndex + 1);
            }

            boolean isMcq = (dto.getOption1() != null && !dto.getOption1().trim().isEmpty())
                    || (dto.getOption2() != null && !dto.getOption2().trim().isEmpty())
                    || ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(dto.getQuestionType());

            if (isMcq) {
                if (dto.getOption1() != null && !dto.getOption1().trim().isEmpty()) {
                    saveOption(dto.getOption1().trim(), dto.getCorrectOption(), questionId);
                }
                if (dto.getOption2() != null && !dto.getOption2().trim().isEmpty()) {
                    saveOption(dto.getOption2().trim(), dto.getCorrectOption(), questionId);
                }
                if (dto.getOption3() != null && !dto.getOption3().trim().isEmpty()) {
                    saveOption(dto.getOption3().trim(), dto.getCorrectOption(), questionId);
                }
                if (dto.getOption4() != null && !dto.getOption4().trim().isEmpty()) {
                    saveOption(dto.getOption4().trim(), dto.getCorrectOption(), questionId);
                }
            } else {
                if (dto.getSampleAnswer() != null && !dto.getSampleAnswer().trim().isEmpty()) {
                    com.skillcheckr.persistence.entity.AnswerEntity answer = new com.skillcheckr.persistence.entity.AnswerEntity();
                    answer.setQuestionId(questionId);
                    answer.setOptionText(dto.getSampleAnswer().trim());
                    answer.setCorrect(true);
                    answerRepository.save(answer);
                }
            }
        }
    }

    private void saveOption(String option, String correctOption, int questionId) {
        com.skillcheckr.persistence.entity.AnswerEntity answer = new com.skillcheckr.persistence.entity.AnswerEntity();
        answer.setQuestionId(questionId);
        answer.setOptionText(option);
        boolean isCorrect = option != null && correctOption != null && option.trim().equalsIgnoreCase(correctOption.trim());
        answer.setCorrect(isCorrect);
        answerRepository.save(answer);
    }

    private void validateExamSubject(int examId, int subjectId) {
        List<Integer> examSubjects = questionRepository.findExamSubjectId(examId);
        if (examSubjects.isEmpty()) {
            throw new ResourceNotFoundException("Exam " + examId + " does not exist");
        }
        if (examSubjects.get(0) != subjectId) {
            throw new BadRequestException("The question subject must match the exam subject");
        }
    }

    @Override
    public List<QuestionDTO> getAllQuestions() {
        String selectQuestionSql = "SELECT q.question_id, q.subject_id, q.question_text, q.question_type, q.marks, q.word_limit, s.subject_name "
                + "FROM question q LEFT JOIN subject s ON q.subject_id = s.subject_id ORDER BY q.question_id DESC";
        List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(selectQuestionSql);

        List<QuestionDTO> questions = new ArrayList<>();
        for (Map<String, Object> row : questionRows) {
            int questionId = ((Number) row.get("question_id")).intValue();
            int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
            String questionText = (String) row.get("question_text");
            String subjectName = (String) row.get("subject_name");
            QuestionDTO dto = new QuestionDTO();
            dto.setQuestionId(questionId);
            dto.setSubjectId(subjectId);
            dto.setQuestion(questionText);
            dto.setSubjectName(subjectName);
            Object type = row.get("question_type");
            if (type != null) dto.setQuestionType(type.toString());
            Object marks = row.get("marks");
            if (marks instanceof Number) dto.setMarks(((Number) marks).intValue());
            Object wl = row.get("word_limit");
            if (wl instanceof Number) dto.setWordLimit(((Number) wl).intValue());
            questions.add(dto);
        }
        populateAnswersBatch(questions);
        return questions;
    }

    @Override
    public Optional<QuestionDTO> getQuestionById(int questionId) {
        String selectQuestionSql = "SELECT q.question_id, q.subject_id, q.question_text, q.question_type, q.marks, q.word_limit, s.subject_name "
                + "FROM question q LEFT JOIN subject s ON q.subject_id = s.subject_id WHERE q.question_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectQuestionSql, questionId);
        if (rows.isEmpty()) return Optional.empty();

        Map<String, Object> row = rows.get(0);
        int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
        String questionText = (String) row.get("question_text");
        String subjectName = (String) row.get("subject_name");
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(questionId);
        dto.setSubjectId(subjectId);
        dto.setQuestion(questionText);
        dto.setSubjectName(subjectName);
        Object type = row.get("question_type");
        if (type != null) dto.setQuestionType(type.toString());
        Object marks = row.get("marks");
        if (marks instanceof Number) dto.setMarks(((Number) marks).intValue());
        Object wl = row.get("word_limit");
        if (wl instanceof Number) dto.setWordLimit(((Number) wl).intValue());
        populateAnswers(dto, questionId);
        return Optional.of(dto);
    }

    @Override
    public Optional<Question> findQuestionDetailsById(int questionId) {
        return questionRepository.findById(questionId).map(entity -> {
            Question question = new Question();
            question.setQuestionId(entity.getQuestionId());
            if (entity.getSubjectId() != null) {
                Subject subject = new Subject();
                subject.setSubjectId(entity.getSubjectId());
                question.setSubject(subject);
            }
            question.setQuestionText(entity.getQuestionText());
            question.setQuestionType(entity.getQuestionType());
            question.setMarks(entity.getMarks() == null ? 0 : entity.getMarks());
            question.setWordLimit(entity.getWordLimit());
            return question;
        });
    }

    @Override
    public void lockQuestionForUpdate(int questionId) {
        jdbcTemplate.queryForList("SELECT question_id FROM question WHERE question_id = ? FOR UPDATE",
                Integer.class, questionId);
    }

    @Override
    public List<Integer> findQuestionIdsBySubjectId(int subjectId) {
        return questionRepository.findQuestionIdsBySubjectId(subjectId);
    }

    @Override
    @Transactional
    public boolean updateQuestion(QuestionDTO dto) {
        if (dto == null || dto.getQuestionId() <= 0) return false;
        int questionId = dto.getQuestionId();

        Optional<QuestionEntity> existing = questionRepository.findById(questionId);
        if (existing.isEmpty()) {
            return false;
        }
        QuestionEntity entity = existing.get();
        if (dto.getQuestion() != null) entity.setQuestionText(dto.getQuestion().trim());
        if (dto.getSubjectId() > 0) entity.setSubjectId(dto.getSubjectId());
        entity.setQuestionType(normalizedQuestionType(dto));
        entity.setMarks(dto.getMarks() > 0 ? dto.getMarks() : 1);
        entity.setWordLimit(dto.getWordLimit());
        questionRepository.saveAndFlush(entity);

        answerRepository.deleteByQuestionId(questionId);

        boolean isMcq = (dto.getOption1() != null && !dto.getOption1().trim().isEmpty())
                || (dto.getOption2() != null && !dto.getOption2().trim().isEmpty())
                || ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(dto.getQuestionType());

        if (isMcq) {
            if (dto.getOption1() != null && !dto.getOption1().trim().isEmpty()) {
                saveOption(dto.getOption1().trim(), dto.getCorrectOption(), questionId);
            }
            if (dto.getOption2() != null && !dto.getOption2().trim().isEmpty()) {
                saveOption(dto.getOption2().trim(), dto.getCorrectOption(), questionId);
            }
            if (dto.getOption3() != null && !dto.getOption3().trim().isEmpty()) {
                saveOption(dto.getOption3().trim(), dto.getCorrectOption(), questionId);
            }
            if (dto.getOption4() != null && !dto.getOption4().trim().isEmpty()) {
                saveOption(dto.getOption4().trim(), dto.getCorrectOption(), questionId);
            }
        } else {
            if (dto.getSampleAnswer() != null && !dto.getSampleAnswer().trim().isEmpty()) {
                com.skillcheckr.persistence.entity.AnswerEntity answer = new com.skillcheckr.persistence.entity.AnswerEntity();
                answer.setQuestionId(questionId);
                answer.setOptionText(dto.getSampleAnswer().trim());
                answer.setCorrect(true);
                answerRepository.save(answer);
            }
        }
        return true;
    }

    @Override
    public List<QuestionDTO> getQuestionsBySubjectId(int subjectId) {
        String selectQuestionSql = "SELECT question_id, subject_id, question_text, question_type, marks, word_limit FROM question WHERE subject_id = ?";
        List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(selectQuestionSql, subjectId);

        List<QuestionDTO> questions = new ArrayList<>();
        for (Map<String, Object> row : questionRows) {
            int questionId = ((Number) row.get("question_id")).intValue();
            String questionText = (String) row.get("question_text");
            QuestionDTO dto = new QuestionDTO();
            dto.setQuestionId(questionId);
            dto.setSubjectId(subjectId);
            dto.setQuestion(questionText);
            Object type = row.get("question_type");
            if (type != null) dto.setQuestionType(type.toString());
            Object marks = row.get("marks");
            if (marks instanceof Number) dto.setMarks(((Number) marks).intValue());
            Object wl = row.get("word_limit");
            if (wl instanceof Number) dto.setWordLimit(((Number) wl).intValue());
            questions.add(dto);
        }
        populateAnswersBatch(questions);
        return questions;
    }

    @Override
    public List<QuestionDTO> getQuestionsByExamId(int examId) {
        String assignmentSql = "SELECT q.question_id, q.subject_id, q.question_text, q.question_type, q.marks, q.word_limit, "
                + "s.subject_name, eq.question_order FROM exam_question eq "
                + "JOIN question q ON q.question_id = eq.question_id "
                + "LEFT JOIN subject s ON s.subject_id = q.subject_id "
                + "WHERE eq.exam_id = ? ORDER BY eq.question_order ASC";
        List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(assignmentSql, examId);

        List<QuestionDTO> questions = new ArrayList<>();
        for (Map<String, Object> row : questionRows) {
            int questionId = ((Number) row.get("question_id")).intValue();
            QuestionDTO dto = new QuestionDTO();
            dto.setQuestionId(questionId);
            dto.setExamId(examId);
            dto.setSubjectId(row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0);
            dto.setQuestion((String) row.get("question_text"));
            dto.setSubjectName((String) row.get("subject_name"));
            setQuestionMetadata(dto, row);
            questions.add(dto);
        }
        populateAnswersBatch(questions);
        return questions;
    }

    private void populateAnswers(QuestionDTO dto, int questionId) {
        List<Map<String, Object>> answerRows = jdbcTemplate.queryForList(
                "SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC", questionId);
        if (answerRows.size() >= 2) {
            dto.setQuestionType(ExamConstants.QUESTION_TYPE_MCQ);
            if (answerRows.size() > 0) dto.setOption1((String) answerRows.get(0).get("option_text"));
            if (answerRows.size() > 1) dto.setOption2((String) answerRows.get(1).get("option_text"));
            if (answerRows.size() > 2) dto.setOption3((String) answerRows.get(2).get("option_text"));
            if (answerRows.size() > 3) dto.setOption4((String) answerRows.get(3).get("option_text"));
            setOptionIds(dto, answerRows);
            for (Map<String, Object> answer : answerRows) {
                Object correct = answer.get("is_correct");
                if ((correct instanceof Boolean && (Boolean) correct)
                        || (correct instanceof Number && ((Number) correct).intValue() == 1)) {
                    dto.setCorrectOption((String) answer.get("option_text"));
                }
            }
        } else {
            dto.setQuestionType(ExamConstants.QUESTION_TYPE_QUESTION_ANSWER);
            if (!answerRows.isEmpty()) {
                dto.setSampleAnswer((String) answerRows.get(0).get("option_text"));
            }
        }
    }

    private void populateAnswersBatch(List<QuestionDTO> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (QuestionDTO q : questions) {
            if (q.getQuestionId() > 0) ids.add(q.getQuestionId());
        }
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> answerRows = jdbcTemplate.queryForList(
                "SELECT answer_id, question_id, option_text, is_correct FROM answer WHERE question_id IN ("
                        + placeholders + ") ORDER BY question_id, answer_id ASC", ids.toArray());
        java.util.Map<Integer, List<Map<String, Object>>> byQuestion = new java.util.HashMap<>();
        for (Map<String, Object> row : answerRows) {
            int qid = ((Number) row.get("question_id")).intValue();
            byQuestion.computeIfAbsent(qid, k -> new ArrayList<>()).add(row);
        }
        for (QuestionDTO dto : questions) {
            List<Map<String, Object>> rows = byQuestion.get(dto.getQuestionId());
            if (rows == null) rows = java.util.List.of();
            applyAnswers(dto, rows);
        }
    }

    private void applyAnswers(QuestionDTO dto, List<Map<String, Object>> answerRows) {
        if (answerRows.size() >= 2) {
            dto.setQuestionType(ExamConstants.QUESTION_TYPE_MCQ);
            if (answerRows.size() > 0) dto.setOption1((String) answerRows.get(0).get("option_text"));
            if (answerRows.size() > 1) dto.setOption2((String) answerRows.get(1).get("option_text"));
            if (answerRows.size() > 2) dto.setOption3((String) answerRows.get(2).get("option_text"));
            if (answerRows.size() > 3) dto.setOption4((String) answerRows.get(3).get("option_text"));
            setOptionIds(dto, answerRows);
            for (Map<String, Object> answer : answerRows) {
                Object correct = answer.get("is_correct");
                if ((correct instanceof Boolean && (Boolean) correct)
                        || (correct instanceof Number && ((Number) correct).intValue() == 1)) {
                    dto.setCorrectOption((String) answer.get("option_text"));
                }
            }
        } else {
            dto.setQuestionType(ExamConstants.QUESTION_TYPE_QUESTION_ANSWER);
            if (!answerRows.isEmpty()) {
                dto.setSampleAnswer((String) answerRows.get(0).get("option_text"));
            }
        }
    }

    private void setQuestionMetadata(QuestionDTO dto, Map<String, Object> row) {
        Object type = row.get("question_type");
        if (type != null) dto.setQuestionType(type.toString());
        Object marks = row.get("marks");
        if (marks instanceof Number) dto.setMarks(((Number) marks).intValue());
        Object wordLimit = row.get("word_limit");
        if (wordLimit instanceof Number) dto.setWordLimit(((Number) wordLimit).intValue());
    }

    private void setOptionIds(QuestionDTO dto, List<Map<String, Object>> answerRows) {
        if (answerRows.size() > 0) dto.setOption1Id(((Number) answerRows.get(0).get("answer_id")).intValue());
        if (answerRows.size() > 1) dto.setOption2Id(((Number) answerRows.get(1).get("answer_id")).intValue());
        if (answerRows.size() > 2) dto.setOption3Id(((Number) answerRows.get(2).get("answer_id")).intValue());
        if (answerRows.size() > 3) dto.setOption4Id(((Number) answerRows.get(3).get("answer_id")).intValue());
    }

    private String normalizedQuestionType(QuestionDTO dto) {
        return dto.getQuestionType() == null || dto.getQuestionType().isBlank() ? ExamConstants.QUESTION_TYPE_MCQ : dto.getQuestionType().trim();
    }

    @Override
    public boolean deleteQuestionById(int questionId) {
        answerRepository.deleteByQuestionId(questionId);
        questionRepository.deleteExamQuestionLinksByQuestionId(questionId);
        questionRepository.deleteById(questionId);
        return true;
    }
}
