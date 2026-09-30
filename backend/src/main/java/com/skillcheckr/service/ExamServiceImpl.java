package com.skillcheckr.service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.AttemptStartException;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamAttempt;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.repository.ExamAttemptRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.validation.ExamCreationValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private ExamAttemptRepository examAttemptRepository;

	@Autowired
	private ExamSubmissionService examSubmissionService;

	@Override
	public Exam saveExam(Exam exam, int teacherId) {
		// Ownership and lifecycle status are decided by the server, never by the request body.
		exam.setTeacherId(teacherId);
		exam.setStatus(ExamConstants.EXAM_STATUS_PENDING);
		ExamCreationValidator.validateExamForCreation(exam);
		return examRepository.saveExam(exam);
	}

	@Override
	public List<Exam> getAllExams() {
		return examRepository.getAllExams();
	}

	@Override
	@Transactional
	public boolean deleteExamById(int examId) {
		return examRepository.deleteExamById(examId);
	}

	@Override
	public boolean acceptExam(int examId) {
		Exam exam = requireExam(examId);
		ExamCreationValidator.validateStatusChange(exam, ExamConstants.EXAM_STATUS_UPCOMING);
		return examRepository.acceptExam(examId);
	}

	@Override
	public boolean updateExamStatus(int examId, String status) {
		Exam exam = requireExam(examId);
		ExamCreationValidator.validateStatusChange(exam, status);
		return examRepository.updateExamStatus(examId, status.trim());
	}

	@Override
	public List<Exam> getAllUpcomingExams() {
		return examRepository.getAllUpcomingExams();
	}

	@Override
	public List<Exam> getAllCompletedExams() {
		return examRepository.getAllCompletedExams();
	}

	@Override
	public Optional<Exam> getExamById(int examId) {
		return examRepository.getExamById(examId);
	}

	@Override
	public AttemptStartResult startAttempt(int examId, int studentId) {
		Exam exam = requireExam(examId);
		if (studentId <= 0) {
			throw new AttemptStartException(404, "Student not found");
		}
		if (!examRepository.isStudentRegisteredForExam(studentId, examId)) {
			throw new AttemptStartException(403, "Student is not registered for this exam");
		}

		LocalDateTime now = LocalDateTime.now();
		List<ExamAttempt> existing = examAttemptRepository.findByExamIdAndStudentId(examId, studentId);
		for (ExamAttempt attempt : existing) {
			if (!ExamConstants.ATTEMPT_STATUS_IN_PROGRESS.equalsIgnoreCase(attempt.getStatus())) {
				throw new AttemptStartException(409, "This exam has already been submitted");
			}
			if (attempt.getExpiresAt() != null && attempt.getExpiresAt().isAfter(now)) {
				return new AttemptStartResult(attempt, true);
			}
			// The attempt window elapsed without an explicit submission. Grade what was
			// saved so the student keeps a result instead of a permanently locked exam.
			examSubmissionService.submit(examId, attempt.getAttemptId(), studentId);
			throw new AttemptStartException(409, "Your previous attempt expired and was submitted automatically");
		}

		if (!canStartNow(exam, now)) {
			throw new AttemptStartException(409, "Exam cannot be started at this time");
		}

		LocalDateTime expiresAt = now.plusMinutes(exam.getDurationMinutes());
		ExamAttempt attempt = ExamAttempt.builder()
				.exam(exam)
				.student(Student.builder().studentId(studentId).build())
				.startedAt(now)
				.expiresAt(expiresAt)
				.status(ExamConstants.ATTEMPT_STATUS_IN_PROGRESS)
				.build();
		int attemptId;
		try {
			attemptId = examAttemptRepository.createAttempt(attempt);
		} catch (DuplicateKeyException ex) {
			// The unique (exam_id, student_id) key is the final guard against a second
			// concurrent attempt slipping through the lookup above.
			throw new AttemptStartException(409, "An attempt for this exam already exists");
		}
		if (attemptId <= 0) {
			throw new AttemptStartException(409, "Exam cannot be started at this time");
		}
		attempt.setAttemptId(attemptId);
		return new AttemptStartResult(attempt, false);
	}

	private Exam requireExam(int examId) {
		if (examId <= 0) {
			throw new BadRequestException("Invalid examId");
		}
		return examRepository.getExamById(examId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
	}

	private boolean canStartNow(Exam exam, LocalDateTime now) {
		if (!ExamConstants.isOpenForRegistration(exam.getStatus())) {
			return false;
		}
		if (exam.getDate() == null || exam.getDate().isBlank() || exam.getStartTime() == null
				|| exam.getEndTime() == null) {
			return false;
		}
		try {
			LocalDateTime startsAt = ExamCreationValidator.resolveWindowStart(exam);
			LocalDateTime endsAt = ExamCreationValidator.resolveWindowEnd(exam);
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
	public void registerStudentForExam(int studentId, int examId) {
		Exam exam = requireExam(examId);
		if (!ExamConstants.isOpenForRegistration(exam.getStatus())) {
			throw new BadRequestException("Registration is closed: this exam is not open for registration");
		}
		if (hasStarted(exam)) {
			throw new BadRequestException("Registration closed: the exam has already started");
		}
		if (!examRepository.registerStudentForExam(studentId, examId)) {
			throw new IllegalStateException("Failed to register for exam.");
		}
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
	public List<ExamRegistration> getRegistrationsByStudentId(int studentId) {
		return examRepository.getRegistrationsByStudentId(studentId);
	}

	@Override
	public List<Student> getRegisteredStudentsByExamId(int examId) {
		return examRepository.getRegisteredStudentsByExamId(examId);
	}

	@Override
	public int getRegistrationCountByExamId(int examId) {
		return examRepository.getRegistrationCountByExamId(examId);
	}

	@Override
	public void unregisterStudentFromExam(int studentId, int examId) {
		Exam exam = requireExam(examId);
		if (hasStarted(exam)) {
			throw new BadRequestException("The exam has already started and registrations can no longer be removed");
		}
		if (!examRepository.unregisterStudentFromExam(studentId, examId)) {
			throw new ResourceNotFoundException("Registration not found.");
		}
	}

	private boolean hasStarted(Exam exam) {
		LocalDateTime windowStart;
		try {
			windowStart = ExamCreationValidator.resolveWindowStart(exam);
		} catch (DateTimeException | NullPointerException ex) {
			// An unreadable schedule fails closed: treat the exam as already started.
			log.warn("Could not resolve the exam start window for examId={}, treating it as started", exam.getExamId());
			return true;
		}
		return LocalDateTime.now().isAfter(windowStart);
	}
}
