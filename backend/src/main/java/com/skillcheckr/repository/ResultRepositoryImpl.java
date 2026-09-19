package com.skillcheckr.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.mapper.ExamResultRowMapper;

@Repository
public class ResultRepositoryImpl implements ResultRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private QuestionRepository questionRepository;

	private final Map<Integer, List<ExamResultDTO>> studentResultsCache = new ConcurrentHashMap<>();
	private final List<ExamResultDTO> allResultsCache = Collections.synchronizedList(new ArrayList<>());
	private final AtomicInteger resultIdSequence = new AtomicInteger(100);

	@Override
	public ExamResultDTO submitExam(ExamSubmissionDTO submission) {
		int examId = submission.getExamId();
		int studentId = submission.getStudentId();
		int attemptId = submission.getAttemptId() > 0 ? submission.getAttemptId() : createSubmittedAttempt(examId, studentId);

		Exam exam = examRepository.getExamById(examId);
		List<QuestionDTO> questions = questionRepository.getQuestionsByExamId(examId);

		int totalMarks = exam != null && exam.getTotalMarks() > 0 ? exam.getTotalMarks() : (questions.isEmpty() ? 100 : questions.size());
		int passingMarks = exam != null && exam.getPassingMarks() > 0 ? exam.getPassingMarks() : (int) Math.ceil(totalMarks * 0.4);

		int marksObtained = 0;
		List<Map<String, Object>> breakdown = new ArrayList<>();

		boolean isMcq = ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(submission.getExamType()) 
				|| (exam != null && ExamConstants.QUESTION_TYPE_MCQ.equalsIgnoreCase(exam.getExamType()))
				|| (submission.getMcqAnswers() != null && !submission.getMcqAnswers().isEmpty());

		if (isMcq) {
			Map<String, String> answers = submission.getMcqAnswers() != null ? submission.getMcqAnswers() : new HashMap<>();
			int pointsPerQuestion = questions.isEmpty() ? 1 : Math.max(1, totalMarks / questions.size());

			for (QuestionDTO q : questions) {
				String qIdKey = String.valueOf(q.getQuestionId());
				String chosenOption = answers.get(qIdKey);
				boolean isCorrect = false;

				if (chosenOption != null && q.getCorrectOption() != null) {
					isCorrect = chosenOption.trim().equalsIgnoreCase(q.getCorrectOption().trim());
				}

				if (isCorrect) {
					marksObtained += pointsPerQuestion;
				}

				Map<String, Object> qDetail = new HashMap<>();
				qDetail.put("questionId", q.getQuestionId());
				qDetail.put("question", q.getQuestion());
				qDetail.put("selectedAnswer", chosenOption);
				qDetail.put("correctAnswer", q.getCorrectOption());
				qDetail.put("isCorrect", isCorrect);
				qDetail.put("marks", isCorrect ? pointsPerQuestion : 0);
				breakdown.add(qDetail);
			}

			if (marksObtained > totalMarks) marksObtained = totalMarks;
		} else {
			// Descriptive / Question-Answer exam
			Map<String, String> textAnswers = submission.getTextAnswers() != null ? submission.getTextAnswers() : new HashMap<>();
			for (QuestionDTO q : questions) {
				String qIdKey = String.valueOf(q.getQuestionId());
				String writtenText = textAnswers.get(qIdKey);

				Map<String, Object> qDetail = new HashMap<>();
				qDetail.put("questionId", q.getQuestionId());
				qDetail.put("question", q.getQuestion());
				qDetail.put("submittedAnswer", writtenText);
				qDetail.put("sampleAnswer", q.getSampleAnswer());
				breakdown.add(qDetail);
			}
			marksObtained = totalMarks; // Default pending teacher grading
		}

		double percentage;
		String status;

		if (submission.isDisqualified()) {
			marksObtained = 0;
			percentage = 0.0;
			status = "Disqualified: " + (submission.getDisqualificationReason() != null ? submission.getDisqualificationReason() : "Academic Integrity Violation");
		} else {
			percentage = totalMarks > 0 ? ((double) marksObtained / totalMarks) * 100.0 : 0.0;
			status = isMcq ? (marksObtained >= passingMarks ? ExamConstants.RESULT_STATUS_PASS : ExamConstants.RESULT_STATUS_FAIL)
					: ExamConstants.RESULT_STATUS_SUBMITTED_FOR_EVALUATION;
		}

		String nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		int resultId = resultIdSequence.incrementAndGet();

		// Try DB save
		try {
			String insertSql = "INSERT INTO result (exam_id, student_id, marks_obtained, total_marks, passing_marks, percentage, status, submitted_at, attempt_id) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)";
			jdbcTemplate.update(insertSql, examId, studentId, marksObtained, totalMarks, passingMarks, percentage, status, attemptId);
		} catch (Exception e) {
			System.err.println("Could not insert result into database, cached in-memory: " + e.getMessage());
		}

		ExamResultDTO resultDTO = ExamResultDTO.builder()
				.resultId(resultId)
				.examId(examId)
				.examName(exam != null ? exam.getExamName() : "Exam #" + examId)
				.examType(isMcq ? ExamConstants.QUESTION_TYPE_MCQ : ExamConstants.QUESTION_TYPE_QUESTION_ANSWER)
				.subjectName(exam != null && exam.getSubject() != null ? exam.getSubject().getSubjectName() : "General")
				.subjectId(exam != null && exam.getSubject() != null ? exam.getSubject().getSubjectId() : 0)
				.studentId(studentId)
				.attemptId(attemptId)
				.studentName(submission.getStudentName() != null ? submission.getStudentName() : "Student #" + studentId)
				.marksObtained(marksObtained)
				.totalMarks(totalMarks)
				.passingMarks(passingMarks)
				.percentage(Math.round(percentage * 100.0) / 100.0)
				.status(status)
				.violationsCount(submission.getViolationsCount())
				.disqualified(submission.isDisqualified())
				.disqualificationReason(submission.getDisqualificationReason())
				.submittedAt(nowFormatted)
				.questionBreakdown(breakdown)
				.build();

		studentResultsCache.computeIfAbsent(studentId, k -> new ArrayList<>()).add(0, resultDTO);
		allResultsCache.add(0, resultDTO);

		return resultDTO;
	}

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		List<ExamResultDTO> cached = studentResultsCache.get(studentId);
		if (cached != null && !cached.isEmpty()) {
			return new ArrayList<>(cached);
		}

		try {
			String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
					+ "FROM result r "
					+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
					+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
					+ "WHERE r.student_id = ? ORDER BY r.result_id DESC";
			List<ExamResultDTO> dbResults = jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, studentId);

			if (!dbResults.isEmpty()) {
				return dbResults;
			}
		} catch (Exception e) {
			System.err.println("Could not load results from database: " + e.getMessage());
		}

		return new ArrayList<>();
	}

	@Override
	public ExamResultDTO getResultByExamAndStudent(int examId, int studentId) {
		List<ExamResultDTO> cached = studentResultsCache.get(studentId);
		if (cached != null) {
			for (ExamResultDTO r : cached) {
				if (r.getExamId() == examId) {
					return r;
				}
			}
		}

		try {
			String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
					+ "FROM result r "
					+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
					+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
					+ "WHERE r.exam_id = ? AND r.student_id = ? ORDER BY r.result_id DESC LIMIT 1";
			List<ExamResultDTO> list = jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, examId, studentId);

			if (!list.isEmpty()) {
				return list.get(0);
			}
		} catch (Exception e) {
			System.err.println("Could not query student exam result: " + e.getMessage());
		}

		return null;
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		if (!allResultsCache.isEmpty()) {
			return new ArrayList<>(allResultsCache);
		}

		try {
			String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name, stu.name AS student_name "
					+ "FROM result r "
					+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
					+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
					+ "LEFT JOIN student stu ON r.student_id = stu.student_id "
					+ "ORDER BY r.result_id DESC";
			return jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	@Override
	public ExamResultDTO findByAttemptId(int attemptId) {
		String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
				+ "FROM result r LEFT JOIN exam e ON e.exam_id = r.exam_id "
				+ "LEFT JOIN subject s ON s.subject_id = e.subject_id "
				+ "WHERE r.attempt_id = ?";
		List<ExamResultDTO> results = jdbcTemplate.query(sql, ExamResultRowMapper.INSTANCE, attemptId);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public ExamResultDTO insertSubmissionResult(ExamResultDTO result) {
		String sql = "INSERT INTO result (exam_id, student_id, marks_obtained, total_marks, passing_marks, "
				+ "percentage, status, submitted_at, attempt_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, result.getExamId());
			statement.setInt(2, result.getStudentId());
			statement.setInt(3, result.getMarksObtained());
			statement.setInt(4, result.getTotalMarks());
			statement.setInt(5, result.getPassingMarks());
			statement.setDouble(6, result.getPercentage());
			statement.setString(7, result.getStatus());
			statement.setTimestamp(8, java.sql.Timestamp.valueOf(result.getSubmittedAt()));
			statement.setInt(9, result.getAttemptId());
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key != null) result.setResultId(key.intValue());
		return result;
	}

	private int createSubmittedAttempt(int examId, int studentId) {
		String sql = "INSERT INTO exam_attempt (exam_id, student_id, started_at, expires_at, submitted_at, status) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		LocalDateTime now = LocalDateTime.now();
		KeyHolder keyHolder = new GeneratedKeyHolder();
		try {
			jdbcTemplate.update(connection -> {
				PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				statement.setInt(1, examId);
				statement.setInt(2, studentId);
				statement.setObject(3, now);
				statement.setObject(4, now);
				statement.setObject(5, now);
				statement.setString(6, ExamConstants.ATTEMPT_STATUS_SUBMITTED);
				return statement;
			}, keyHolder);
			Number key = keyHolder.getKey();
			return key == null ? 0 : key.intValue();
		} catch (Exception e) {
			System.err.println("Could not create exam attempt for result: " + e.getMessage());
			return 0;
		}
	}
}
