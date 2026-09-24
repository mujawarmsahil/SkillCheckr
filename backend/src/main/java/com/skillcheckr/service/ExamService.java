package com.skillcheckr.service;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.AttemptStartResult;
import com.skillcheckr.model.Subject;

public interface ExamService {

	Subject saveExam(Exam exam);

	List<Exam> viewAllExams();

	boolean deleteExamById(int examId);

	boolean acceptExam(int examId);

	boolean updateExamStatus(int examId, String status);

	List<Exam> viewAllUpcomingExam();

	List<Exam> viewAllCompletedExam();

	Optional<Exam> getExamById(int examId);

	AttemptStartResult startAttempt(int examId, int studentId);

	List<Exam> getExamsByTeacherId(int teacherId);

	boolean registerStudentForExam(int studentId, int examId);

	boolean isStudentRegisteredForExam(int studentId, int examId);

	List<Integer> getRegisteredExamIdsForStudent(int studentId);

	List<com.skillcheckr.model.ExamRegistration> getRegistrationsByStudentId(int studentId);

	List<com.skillcheckr.model.Student> getRegisteredStudentsByExamId(int examId);

	int getRegistrationCountByExamId(int examId);

	boolean unregisterStudentFromExam(int studentId, int examId);
}
