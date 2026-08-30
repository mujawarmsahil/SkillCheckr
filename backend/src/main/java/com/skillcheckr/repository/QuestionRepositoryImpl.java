package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.QuestionDTO;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void saveQuestionWithAnswers(List<QuestionDTO> questions) {
		if (questions == null || questions.isEmpty()) {
			return;
		}

		for (QuestionDTO dto : questions) {
			if (dto == null || dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
				continue;
			}

			String insertQuestionSql = "INSERT INTO question(subject_id, question_text) VALUES(?, ?)";
			KeyHolder keyHolder = new GeneratedKeyHolder();

			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, dto.getSubjectId());
				ps.setString(2, dto.getQuestion().trim());
				return ps;
			}, keyHolder);

			Number generatedKey = keyHolder.getKey();
			if (generatedKey == null) {
				continue;
			}
			int questionId = generatedKey.intValue();

			boolean isMcq = (dto.getOption1() != null && !dto.getOption1().trim().isEmpty())
					|| (dto.getOption2() != null && !dto.getOption2().trim().isEmpty())
					|| "MCQ".equalsIgnoreCase(dto.getQuestionType());

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
				// Question-Answer / Subjective: Save sample answer / rubric if provided
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

			List<QuestionDTO> list = new ArrayList<>();
			for (Map<String, Object> row : questionRows) {
				int questionId = ((Number) row.get("question_id")).intValue();
				int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
				String questionText = (String) row.get("question_text");
				String subjectName = (String) row.get("subject_name");

				String selectAnswerSql = "SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC";
				List<Map<String, Object>> answerRows = jdbcTemplate.queryForList(selectAnswerSql, questionId);

				QuestionDTO dto = new QuestionDTO();
				dto.setQuestionId(questionId);
				dto.setSubjectId(subjectId);
				dto.setQuestion(questionText);
				dto.setSubjectName(subjectName);

				if (answerRows.size() >= 2) {
					dto.setQuestionType("MCQ");
					if (answerRows.size() > 0) dto.setOption1((String) answerRows.get(0).get("option_text"));
					if (answerRows.size() > 1) dto.setOption2((String) answerRows.get(1).get("option_text"));
					if (answerRows.size() > 2) dto.setOption3((String) answerRows.get(2).get("option_text"));
					if (answerRows.size() > 3) dto.setOption4((String) answerRows.get(3).get("option_text"));

					for (Map<String, Object> ans : answerRows) {
						Object isCorrObj = ans.get("is_correct");
						boolean isCorr = false;
						if (isCorrObj instanceof Boolean) {
							isCorr = (Boolean) isCorrObj;
						} else if (isCorrObj instanceof Number) {
							isCorr = ((Number) isCorrObj).intValue() == 1;
						}
						if (isCorr) {
							dto.setCorrectOption((String) ans.get("option_text"));
						}
					}
				} else {
					dto.setQuestionType("QUESTION_ANSWER");
					if (!answerRows.isEmpty()) {
						dto.setSampleAnswer((String) answerRows.get(0).get("option_text"));
					}
				}

				list.add(dto);
			}
			return list;
		} catch (Exception e) {
			System.err.println("Error fetching all questions: " + e.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public QuestionDTO getQuestionById(int questionId) {
		try {
			String selectQuestionSql = "SELECT q.question_id, q.subject_id, q.question_text, s.subject_name "
					+ "FROM question q LEFT JOIN subject s ON q.subject_id = s.subject_id WHERE q.question_id = ?";
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectQuestionSql, questionId);
			if (rows.isEmpty()) return null;

			Map<String, Object> row = rows.get(0);
			int subjectId = row.get("subject_id") != null ? ((Number) row.get("subject_id")).intValue() : 0;
			String questionText = (String) row.get("question_text");
			String subjectName = (String) row.get("subject_name");

			String selectAnswerSql = "SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC";
			List<Map<String, Object>> answerRows = jdbcTemplate.queryForList(selectAnswerSql, questionId);

			QuestionDTO dto = new QuestionDTO();
			dto.setQuestionId(questionId);
			dto.setSubjectId(subjectId);
			dto.setQuestion(questionText);
			dto.setSubjectName(subjectName);

			if (answerRows.size() >= 2) {
				dto.setQuestionType("MCQ");
				if (answerRows.size() > 0) dto.setOption1((String) answerRows.get(0).get("option_text"));
				if (answerRows.size() > 1) dto.setOption2((String) answerRows.get(1).get("option_text"));
				if (answerRows.size() > 2) dto.setOption3((String) answerRows.get(2).get("option_text"));
				if (answerRows.size() > 3) dto.setOption4((String) answerRows.get(3).get("option_text"));

				for (Map<String, Object> ans : answerRows) {
					Object isCorrObj = ans.get("is_correct");
					boolean isCorr = false;
					if (isCorrObj instanceof Boolean) {
						isCorr = (Boolean) isCorrObj;
					} else if (isCorrObj instanceof Number) {
						isCorr = ((Number) isCorrObj).intValue() == 1;
					}
					if (isCorr) {
						dto.setCorrectOption((String) ans.get("option_text"));
					}
				}
			} else {
				dto.setQuestionType("QUESTION_ANSWER");
				if (!answerRows.isEmpty()) {
					dto.setSampleAnswer((String) answerRows.get(0).get("option_text"));
				}
			}
			return dto;
		} catch (Exception e) {
			System.err.println("Error fetching question by ID: " + e.getMessage());
			return null;
		}
	}

	@Override
	public boolean updateQuestion(QuestionDTO dto) {
		if (dto == null || dto.getQuestionId() <= 0) return false;
		try {
			int questionId = dto.getQuestionId();
			if (dto.getSubjectId() > 0) {
				jdbcTemplate.update("UPDATE question SET question_text = ?, subject_id = ? WHERE question_id = ?",
						dto.getQuestion(), dto.getSubjectId(), questionId);
			} else {
				jdbcTemplate.update("UPDATE question SET question_text = ? WHERE question_id = ?",
						dto.getQuestion(), questionId);
			}

			// Clear old answers and re-insert updated ones
			jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", questionId);

			boolean isMcq = (dto.getOption1() != null && !dto.getOption1().trim().isEmpty())
					|| (dto.getOption2() != null && !dto.getOption2().trim().isEmpty())
					|| "MCQ".equalsIgnoreCase(dto.getQuestionType());

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
			System.err.println("Error updating question: " + e.getMessage());
			return false;
		}
	}

	@Override
	public List<QuestionDTO> getQuestionsBySubjectId(int subjectId) {
		try {
			String selectQuestionSql = "SELECT question_id, subject_id, question_text FROM question WHERE subject_id = ?";
			List<Map<String, Object>> questionRows = jdbcTemplate.queryForList(selectQuestionSql, subjectId);

			List<QuestionDTO> list = new ArrayList<>();
			for (Map<String, Object> row : questionRows) {
				int questionId = ((Number) row.get("question_id")).intValue();
				String questionText = (String) row.get("question_text");

				String selectAnswerSql = "SELECT answer_id, option_text, is_correct FROM answer WHERE question_id = ? ORDER BY answer_id ASC";
				List<Map<String, Object>> answerRows = jdbcTemplate.queryForList(selectAnswerSql, questionId);

				QuestionDTO dto = new QuestionDTO();
				dto.setQuestionId(questionId);
				dto.setSubjectId(subjectId);
				dto.setQuestion(questionText);

				if (answerRows.size() >= 2) {
					dto.setQuestionType("MCQ");
					if (answerRows.size() > 0) dto.setOption1((String) answerRows.get(0).get("option_text"));
					if (answerRows.size() > 1) dto.setOption2((String) answerRows.get(1).get("option_text"));
					if (answerRows.size() > 2) dto.setOption3((String) answerRows.get(2).get("option_text"));
					if (answerRows.size() > 3) dto.setOption4((String) answerRows.get(3).get("option_text"));

					for (Map<String, Object> ans : answerRows) {
						Object isCorrObj = ans.get("is_correct");
						boolean isCorr = false;
						if (isCorrObj instanceof Boolean) {
							isCorr = (Boolean) isCorrObj;
						} else if (isCorrObj instanceof Number) {
							isCorr = ((Number) isCorrObj).intValue() == 1;
						}
						if (isCorr) {
							dto.setCorrectOption((String) ans.get("option_text"));
						}
					}
				} else {
					dto.setQuestionType("QUESTION_ANSWER");
					if (!answerRows.isEmpty()) {
						dto.setSampleAnswer((String) answerRows.get(0).get("option_text"));
					}
				}

				list.add(dto);
			}
			return list;
		} catch (Exception e) {
			System.err.println("Error fetching questions by subject ID: " + e.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public List<QuestionDTO> getQuestionsByExamId(int examId) {
		try {
			String getSubjectSql = "SELECT subject_id FROM exam WHERE exam_id = ?";
			Integer subjectId = jdbcTemplate.queryForObject(getSubjectSql, Integer.class, examId);
			if (subjectId != null) {
				List<QuestionDTO> questions = getQuestionsBySubjectId(subjectId);
				for (QuestionDTO q : questions) {
					q.setExamId(examId);
				}
				return questions;
			}
		} catch (Exception e) {
			System.err.println("Error fetching questions by exam ID: " + e.getMessage());
		}
		return new ArrayList<>();
	}

	@Override
	public boolean deleteQuestionById(int questionId) {
		try {
			jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", questionId);
			int rows = jdbcTemplate.update("DELETE FROM question WHERE question_id = ?", questionId);
			return rows > 0;
		} catch (Exception e) {
			System.err.println("Error deleting question: " + e.getMessage());
			return false;
		}
	}
}
