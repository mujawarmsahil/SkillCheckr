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
}
