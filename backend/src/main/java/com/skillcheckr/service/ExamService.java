package com.skillcheckr.service;

import java.util.List;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Subject;

public interface ExamService {

	Subject saveExam(Exam exam);

	List<Exam> viewAllExams();

	boolean deleteExamById(int examId);

	boolean acceptExam(int examId);

	List<Exam> viewAllUpcomingExam();

	List<Exam> viewAllCompletedExam();

	Exam getExamById(int examId);

	List<Exam> getExamsByTeacherId(int teacherId);
}
