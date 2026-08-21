package com.skillcheckr.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.model.QuestionDTO;

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

	private boolean tableInitialized = false;

	private synchronized void ensureResultTableExists() {
		if (tableInitialized) return;
		try {
			String createTableSql = "CREATE TABLE IF NOT EXISTS result ("
					+ "result_id INT AUTO_INCREMENT PRIMARY KEY, "
					+ "exam_id INT, "
					+ "student_id INT, "
					+ "marks_obtained INT, "
					+ "total_marks INT, "
					+ "passing_marks INT, "
					+ "percentage DOUBLE, "
					+ "status VARCHAR(50), "
					+ "submitted_at DATETIME"
					+ ")";
			jdbcTemplate.execute(createTableSql);
			tableInitialized = true;
		} catch (Exception e) {
			System.err.println("Note: Result table creation skipped or in-memory fallback used: " + e.getMessage());
			tableInitialized = true;
		}
	}

	@Override
	public ExamResultDTO submitExam(ExamSubmissionDTO submission) {
		ensureResultTableExists();

		int examId = submission.getExamId();
		int studentId = submission.getStudentId();

		Exam exam = examRepository.getExamById(examId);
		List<QuestionDTO> questions = questionRepository.getQuestionsByExamId(examId);

		int totalMarks = exam != null && exam.getTotalMarks() > 0 ? exam.getTotalMarks() : (questions.isEmpty() ? 100 : questions.size());
		int passingMarks = exam != null && exam.getPassingMarks() > 0 ? exam.getPassingMarks() : (int) Math.ceil(totalMarks * 0.4);

		int marksObtained = 0;
		List<Map<String, Object>> breakdown = new ArrayList<>();

		boolean isMcq = "MCQ".equalsIgnoreCase(submission.getExamType()) 
				|| (exam != null && "MCQ".equalsIgnoreCase(exam.getExamType()))
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

		double percentage = totalMarks > 0 ? ((double) marksObtained / totalMarks) * 100.0 : 0.0;
		String status = isMcq ? (marksObtained >= passingMarks ? "Pass" : "Fail") : "Submitted for Evaluation";

		String nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		int resultId = resultIdSequence.incrementAndGet();

		// Try DB save
		try {
			String insertSql = "INSERT INTO result (exam_id, student_id, marks_obtained, total_marks, passing_marks, percentage, status, submitted_at) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
			jdbcTemplate.update(insertSql, examId, studentId, marksObtained, totalMarks, passingMarks, percentage, status);
		} catch (Exception e) {
			System.err.println("Could not insert result into database, cached in-memory: " + e.getMessage());
		}

		ExamResultDTO resultDTO = ExamResultDTO.builder()
				.resultId(resultId)
				.examId(examId)
				.examName(exam != null ? exam.getExamName() : "Exam #" + examId)
				.examType(isMcq ? "MCQ" : "QUESTION_ANSWER")
				.subjectName(exam != null && exam.getSubject() != null ? exam.getSubject().getSubjectName() : "General")
				.subjectId(exam != null && exam.getSubject() != null ? exam.getSubject().getSubjectId() : 0)
				.studentId(studentId)
				.studentName(submission.getStudentName() != null ? submission.getStudentName() : "Student #" + studentId)
				.marksObtained(marksObtained)
				.totalMarks(totalMarks)
				.passingMarks(passingMarks)
				.percentage(Math.round(percentage * 100.0) / 100.0)
				.status(status)
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

		// Try loading from database
		try {
			ensureResultTableExists();
			String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
					+ "FROM result r "
					+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
					+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
					+ "WHERE r.student_id = ? ORDER BY r.result_id DESC";
			List<ExamResultDTO> dbResults = jdbcTemplate.query(sql, (rs, rowNum) -> ExamResultDTO.builder()
					.resultId(rs.getInt("result_id"))
					.examId(rs.getInt("exam_id"))
					.examName(rs.getString("exam_name"))
					.examType(rs.getString("exam_type") != null ? rs.getString("exam_type") : "MCQ")
					.subjectName(rs.getString("subject_name"))
					.studentId(rs.getInt("student_id"))
					.marksObtained(rs.getInt("marks_obtained"))
					.totalMarks(rs.getInt("total_marks"))
					.passingMarks(rs.getInt("passing_marks"))
					.percentage(rs.getDouble("percentage"))
					.status(rs.getString("status"))
					.submittedAt(rs.getString("submitted_at"))
					.build(), studentId);

			if (!dbResults.isEmpty()) {
				return dbResults;
			}
		} catch (Exception e) {
			System.err.println("Could not load results from database: " + e.getMessage());
		}

		return new ArrayList<>();
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		if (!allResultsCache.isEmpty()) {
			return new ArrayList<>(allResultsCache);
		}

		try {
			ensureResultTableExists();
			String sql = "SELECT r.*, e.exam_name, e.exam_type, s.subject_name "
					+ "FROM result r "
					+ "LEFT JOIN exam e ON r.exam_id = e.exam_id "
					+ "LEFT JOIN subject s ON e.subject_id = s.subject_id "
					+ "ORDER BY r.result_id DESC";
			return jdbcTemplate.query(sql, (rs, rowNum) -> ExamResultDTO.builder()
					.resultId(rs.getInt("result_id"))
					.examId(rs.getInt("exam_id"))
					.examName(rs.getString("exam_name"))
					.examType(rs.getString("exam_type") != null ? rs.getString("exam_type") : "MCQ")
					.subjectName(rs.getString("subject_name"))
					.studentId(rs.getInt("student_id"))
					.marksObtained(rs.getInt("marks_obtained"))
					.totalMarks(rs.getInt("total_marks"))
					.passingMarks(rs.getInt("passing_marks"))
					.percentage(rs.getDouble("percentage"))
					.status(rs.getString("status"))
					.submittedAt(rs.getString("submitted_at"))
					.build());
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
