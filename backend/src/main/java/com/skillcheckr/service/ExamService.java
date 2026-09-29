package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;

public interface ExamService {

	Exam saveExam(Exam exam, int teacherId);

	List<Exam> getAllExams();

	boolean deleteExamById(int examId);

	boolean acceptExam(int examId);

	boolean updateExamStatus(int examId, String status);

	List<Exam> getAllUpcomingExams();

	List<Exam> getAllCompletedExams();

	Optional<Exam> getExamById(int examId);

	AttemptStartResult startAttempt(int examId, int studentId);

	List<Exam> getExamsByTeacherId(int teacherId);

	void registerStudentForExam(int studentId, int examId);

	boolean isStudentRegisteredForExam(int studentId, int examId);

	List<Integer> getRegisteredExamIdsForStudent(int studentId);

	List<ExamRegistration> getRegistrationsByStudentId(int studentId);

	List<Student> getRegisteredStudentsByExamId(int examId);

	int getRegistrationCountByExamId(int examId);

	void unregisterStudentFromExam(int studentId, int examId);
}
