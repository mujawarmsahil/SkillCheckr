package com.skillcheckr.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.QuestionRepository;
import com.skillcheckr.repository.ResultRepository;

@Service
public class ResultServiceImpl implements ResultService {

	@Autowired
	private ResultRepository resultRepository;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private QuestionRepository questionRepository;

	@Override
	public ExamResultDTO submitExam(ExamSubmissionDTO submission) {
		int examId = submission.getExamId();
		int studentId = submission.getStudentId();
		int attemptId = submission.getAttemptId() > 0
				? submission.getAttemptId()
				: resultRepository.createSubmittedAttempt(examId, studentId);

		Exam exam = examRepository.getExamById(examId).orElse(null);
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

		ExamResultDTO resultDTO = ExamResultDTO.builder()
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

		return resultRepository.insertSubmissionResult(resultDTO);
	}

	@Override
	public List<ExamResultDTO> getResultsByStudentId(int studentId) {
		return resultRepository.getResultsByStudentId(studentId);
	}

	@Override
	public Optional<ExamResultDTO> getResultByExamAndStudent(int examId, int studentId) {
		return resultRepository.getResultByExamAndStudent(examId, studentId);
	}

	@Override
	public List<ExamResultDTO> getAllResults() {
		return resultRepository.getAllResults();
	}
}