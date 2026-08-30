package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.ExamRepository;

@Service
public class ExamServiceImpl implements ExamService {

	@Autowired
	private ExamRepository examRepository;

	@Override
	public Subject saveExam(Exam exam) {
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
	public Exam getExamById(int examId) {
		return examRepository.getExamById(examId);
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
