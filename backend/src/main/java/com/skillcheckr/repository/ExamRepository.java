package com.skillcheckr.repository;

import java.util.List;
import java.util.Optional;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamRegistration;
import com.skillcheckr.model.Student;
import com.skillcheckr.model.Subject;

public interface ExamRepository {

	Subject saveExam(Exam exam);

	List<Exam> getAllExams();

	boolean deleteExamById(int examId);

	boolean acceptExam(int examId);

	boolean updateExamStatus(int examId, String status);

	List<Exam> getAllUpcomingExams();

	List<Exam> getAllCompletedExams();

	Optional<Exam> getExamById(int examId);

	List<Exam> getExamsByTeacherId(int teacherId);

	boolean registerStudentForExam(int studentId, int examId);

	boolean isStudentRegisteredForExam(int studentId, int examId);

	List<Integer> getRegisteredExamIdsForStudent(int studentId);

	List<ExamRegistration> getRegistrationsByStudentId(int studentId);

	List<Student> getRegisteredStudentsByExamId(int examId);

	int getRegistrationCountByExamId(int examId);

	boolean unregisterStudentFromExam(int studentId, int examId);
}