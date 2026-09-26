package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Question;
import com.skillcheckr.mapper.QuestionRowMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void saveQuestionWithAnswers(List<QuestionDTO> questions) {
		if (questions == null || questions.isEmpty()) {
			return;
		}

		for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
			QuestionDTO dto = questions.get(questionIndex);
			if (dto == null || dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
				continue;
			}

			String insertQuestionSql = "INSERT INTO question(subject_id, question_text, question_type, marks, word_limit) VALUES(?, ?, ?, ?, ?)";
			KeyHolder keyHolder = new GeneratedKeyHolder();

			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, dto.getSubjectId());
				ps.setString(2, dto.getQuestion().trim());
				ps.setString(3, dto.getQuestionType() == null || dto.getQuestionType().isBlank()
						? ExamConstants.QUESTION_TYPE_MCQ : dto.getQuestionType().trim());
				ps.setInt(4, dto.getMarks() > 0 ? dto.getMarks() : 1);
				if (dto.getWordLimit() == null) {
					ps.setNull(5, java.sql.Types.INTEGER);
				} else {
					ps.setInt(5, dto.getWordLimit());
				}
				return ps;
			}, keyHolder);

			Number generatedKey = keyHolder.getKey();
			if (generatedKey == null) {
				continue;
			}
			int questionId = generatedKey.intValue();
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
					String insertAnswerSql = "INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)";
					jdbcTemplate.update(insertAnswerSql, questionId, dto.getSampleAnswer().trim(), true);
				}
			}
		}
	}

	private void saveOption(String option, String correctOption, int questionId) {
		String insertAnswerSql = "INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)";
		boolean isCorrect = option != null && correctOption != null && option.trim().equalsIgnoreCase(correctOption.trim());
		jdbcTemplate.update(insertAnswerSql, questionId, option, isCorrect);
	}

	@Override
	public List<QuestionDTO> getAllQuestions() {
		try {
			String selectQuestionSql = "SELECT q.question_id, q.subject_id, q.question_text, s.subject_name "
					+ "FROM question q LEFT JOIN subject s ON q.subject_id = s.subject_id ORDER BY q.question_id DESC";
			List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(selectQuestionSql);

			List<QuestionDTO> questions = new ArrayList<>();
			for (Map<String, Object> row : questionRows) {
				int questionId = ((Number) row.get("question_id")).intValue();
				int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
				String questionText = (String) row.get("question_text");
				String subjectName = (String) row.get("subject_name");
				questions.add(buildQuestionDto(questionId, subjectId, questionText, subjectName));
			}
			return questions;
		} catch (Exception e) {
			log.error("Error fetching all questions", e);
			return new ArrayList<>();
		}
	}

	@Override
	public Optional<QuestionDTO> getQuestionById(int questionId) {
		try {
			String selectQuestionSql = "SELECT q.question_id, q.subject_id, q.question_text, s.subject_name "
					+ "FROM question q LEFT JOIN subject s ON q.subject_id = s.subject_id WHERE q.question_id = ?";
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectQuestionSql, questionId);
			if (rows.isEmpty()) return Optional.empty();

			Map<String, Object> row = rows.get(0);
			int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
			String questionText = (String) row.get("question_text");
			String subjectName = (String) row.get("subject_name");
			return Optional.of(buildQuestionDto(questionId, subjectId, questionText, subjectName));
		} catch (Exception e) {
			log.error("Error fetching question by ID", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<Question> findQuestionDetailsById(int questionId) {
		String sql = "SELECT question_id, subject_id, question_text, question_type, marks, word_limit "
				+ "FROM question WHERE question_id = ?";
		List<Question> questions = jdbcTemplate.query(sql, QuestionRowMapper.INSTANCE, questionId);
		return questions.stream().findFirst();
	}

	@Override
	public boolean updateQuestion(QuestionDTO dto) {
		if (dto == null || dto.getQuestionId() <= 0) return false;
		try {
			int questionId = dto.getQuestionId();
			if (dto.getSubjectId() > 0) {
				jdbcTemplate.update("UPDATE question SET question_text = ?, subject_id = ?, question_type = ?, marks = ?, word_limit = ? WHERE question_id = ?",
						dto.getQuestion(), dto.getSubjectId(), normalizedQuestionType(dto), dto.getMarks() > 0 ? dto.getMarks() : 1,
						dto.getWordLimit(), questionId);
			} else {
				jdbcTemplate.update("UPDATE question SET question_text = ?, question_type = ?, marks = ?, word_limit = ? WHERE question_id = ?",
						dto.getQuestion(), normalizedQuestionType(dto), dto.getMarks() > 0 ? dto.getMarks() : 1,
						dto.getWordLimit(), questionId);
			}

			jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", questionId);

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
					String insertAnswerSql = "INSERT INTO answer(question_id, option_text, is_correct) VALUES(?, ?, ?)";
					jdbcTemplate.update(insertAnswerSql, questionId, dto.getSampleAnswer().trim(), true);
				}
			}
			return true;
		} catch (Exception e) {
			log.error("Error updating question", e);
			return false;
		}
	}

	@Override
	public List<QuestionDTO> getQuestionsBySubjectId(int subjectId) {
		try {
			String selectQuestionSql = "SELECT question_id, subject_id, question_text FROM question WHERE subject_id = ?";
			List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(selectQuestionSql, subjectId);

			List<QuestionDTO> questions = new ArrayList<>();
			for (Map<String, Object> row : questionRows) {
				int questionId = ((Number) row.get("question_id")).intValue();
				String questionText = (String) row.get("question_text");
				questions.add(buildQuestionDto(questionId, subjectId, questionText, null));
			}
			return questions;
		} catch (Exception e) {
			log.error("Error fetching questions by subject ID", e);
			return new ArrayList<>();
		}
	}

	@Override
	public List<QuestionDTO> getQuestionsByExamId(int examId) {
		try {
			String assignmentSql = "SELECT q.question_id, q.subject_id, q.question_text, q.question_type, q.marks, q.word_limit, "
					+ "s.subject_name, eq.question_order FROM exam_question eq "
					+ "JOIN question q ON q.question_id = eq.question_id "
					+ "LEFT JOIN subject s ON s.subject_id = q.subject_id "
					+ "WHERE eq.exam_id = ? ORDER BY eq.question_order ASC";
			List<Map<String, Object>> assignedRows;
			try {
				assignedRows = jdbcTemplate.queryForList(assignmentSql, examId);
			} catch (Exception ignored) {
				assignedRows = null;
			}
			if (assignedRows != null && !assignedRows.isEmpty()) {
				return mapQuestionRows(assignedRows, examId);
			}

			String getSubjectSql = "SELECT subject_id FROM exam WHERE exam_id = ?";
			Integer subjectId = jdbcTemplate.queryForObject(getSubjectSql, Integer.class, examId);
			if (subjectId != null) {
				List<QuestionDTO> questions = getQuestionsBySubjectId(subjectId);
				for (QuestionDTO q : questions) q.setExamId(examId);
				return questions;
			}
		} catch (Exception e) {
			log.error("Error fetching questions by exam ID", e);
		}
		return new ArrayList<>();
	}

	private QuestionDTO buildQuestionDto(int questionId, int subjectId, String questionText, String subjectName) {
		QuestionDTO dto = new QuestionDTO();
		dto.setQuestionId(questionId);
		dto.setSubjectId(subjectId);
		dto.setQuestion(questionText);
		dto.setSubjectName(subjectName);
		loadQuestionMetadata(dto);
		populateAnswers(dto, questionId);
		return dto;
	}

	private List<QuestionDTO> mapQuestionRows(List<Map<String, Object>> questionRows, int examId) {
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
			populateAnswers(dto, questionId);
			questions.add(dto);
		}
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

	private void loadQuestionMetadata(QuestionDTO dto) {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
					"SELECT question_type, marks, word_limit FROM question WHERE question_id = ?", dto.getQuestionId());
			if (rows != null && !rows.isEmpty()) setQuestionMetadata(dto, rows.get(0));
		} catch (Exception ignored) {
			// Metadata is optional for legacy records and test fixtures.
		}
	}

	private String normalizedQuestionType(QuestionDTO dto) {
		return dto.getQuestionType() == null || dto.getQuestionType().isBlank() ? ExamConstants.QUESTION_TYPE_MCQ : dto.getQuestionType().trim();
	}

	@Override
	public boolean deleteQuestionById(int questionId) {
		try {
			jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", questionId);
			int rows = jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", questionId);
			return rows > 0;
		} catch (Exception e) {
			log.error("Error deleting question", e);
			return false;
		}
	}
}
