package com.skillcheckr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;
import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptStartException;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.validation.ExamCreationValidator;

@Service
public class ExamServiceImpl implements ExamService {

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private ExamAttemptRepository examAttemptRepository;

	@Override
	public Subject saveExam(Exam exam) {
		ExamCreationValidator.validateExamForCreation(exam);
		return examRepository.saveExam(exam);
	}

	@Override
	public List<Exam> viewAllExams() {
		return examRepository.viewAllExams();
	}

	@Override
	public boolean deleteExamById(int examId) {
		return examRepository.deleteExamById(examId);
	}

	@Override
	public boolean acceptExam(int examId) {
		return examRepository.acceptExam(examId);
	}

	@Override
	public boolean updateExamStatus(int examId, String status) {
		return examRepository.updateExamStatus(examId, status);
	}

	@Override
	public List<Exam> viewAllUpcomingExam() {
		return examRepository.viewAllUpcomingExam();
	}

	@Override
	public List<Exam> viewAllCompletedExam() {
		return examRepository.viewAllCompletedExam();
	}

	@Override
	public Optional<Exam> getExamById(int examId) {
		return examRepository.getExamById(examId);
	}

	@Override
	public AttemptStartResult startAttempt(int examId, int studentId) {
		Exam exam = examRepository.getExamById(examId)
				.orElseThrow(() -> new AttemptStartException(404, "Exam not found"));
		if (studentId <= 0) {
			throw new AttemptStartException(404, "Student not found");
		}
		if (!examRepository.isStudentRegisteredForExam(studentId, examId)) {
			throw new AttemptStartException(403, "Student is not registered for this exam");
		}

		LocalDateTime now = LocalDateTime.now();
		for (ExamAttempt existing : examAttemptRepository.findByExamIdAndStudentId(examId, studentId)) {
			if (ExamConstants.ATTEMPT_STATUS_IN_PROGRESS.equalsIgnoreCase(existing.getStatus())
					&& existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
				return new AttemptStartResult(existing, true);
			}
		}

		if (!canStartNow(exam, now)) {
			throw new AttemptStartException(409, "Exam cannot be started at this time");
		}

		LocalDateTime expiresAt = now.plusMinutes(Math.max(0, exam.getDurationMinutes()));
		ExamAttempt attempt = ExamAttempt.builder()
				.exam(exam)
				.student(Student.builder().studentId(studentId).build())
				.startedAt(now)
				.expiresAt(expiresAt)
				.status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS)
				.build();
		int attemptId = examAttemptRepository.createAttempt(attempt);
		if (attemptId <= 0) {
			throw new AttemptStartException(409, "Exam cannot be started at this time");
		}
		attempt.setAttemptId(attemptId);
		return new AttemptStartResult(attempt, false);
	}

	private boolean canStartNow(Exam exam, LocalDateTime now) {
		String status = exam.getStatus();
		if (status == null || !(ExamConstants.EXAM_STATUS_UPCOMING.equalsIgnoreCase(status)
				|| ExamConstants.EXAM_STATUS_APPROVED.equalsIgnoreCase(status))) {
			return false;
		}
		try {
			String rawDate = exam.getDate();
			if (rawDate == null || rawDate.isBlank() || exam.getStartTime() == null || exam.getEndTime() == null) {
				return false;
			}
			String datePart = rawDate.trim().replace('T', ' ').split(" ")[0];
			LocalDate date = LocalDate.parse(datePart);
			LocalDateTime startsAt = LocalDateTime.of(date, exam.getStartTime());
			LocalDateTime endsAt = LocalDateTime.of(date, exam.getEndTime());
			if (!endsAt.isAfter(startsAt)) {
				endsAt = endsAt.plusDays(1);
			}
			return !now.isBefore(startsAt) && now.isBefore(endsAt);
		} catch (RuntimeException ex) {
			return false;
		}
	}

	@Override
	public List<Exam> getExamsByTeacherId(int teacherId) {
		return examRepository.getExamsByTeacherId(teacherId);
	}

	@Override
	public boolean registerStudentForExam(int studentId, int examId) {
		return examRepository.registerStudentForExam(studentId, examId);
	}

	@Override
	public boolean isStudentRegisteredForExam(int studentId, int examId) {
		return examRepository.isStudentRegisteredForExam(studentId, examId);
	}

	@Override
	public List<Integer> getRegisteredExamIdsForStudent(int studentId) {
		return examRepository.getRegisteredExamIdsForStudent(studentId);
	}

	@Override
	public List<com.skillcheckr.model.ExamRegistration> getRegistrationsByStudentId(int studentId) {
		return examRepository.getRegistrationsByStudentId(studentId);
	}

	@Override
	public List<com.skillcheckr.model.Student> getRegisteredStudentsByExamId(int examId) {
		return examRepository.getRegisteredStudentsByExamId(examId);
	}

	@Override
	public int getRegistrationCountByExamId(int examId) {
		return examRepository.getRegistrationCountByExamId(examId);
	}

	@Override
	public boolean unregisterStudentFromExam(int studentId, int examId) {
		return examRepository.unregisterStudentFromExam(studentId, examId);
	}
}
