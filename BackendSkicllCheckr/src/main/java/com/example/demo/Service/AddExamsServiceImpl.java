package com.example.demo.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Exams;
import com.example.demo.Model.Subject;
import com.example.demo.Repository.AddExamsRepository;


@Service
public class AddExamsServiceImpl implements AddExamsService{

	@Autowired
	public AddExamsRepository addExamsRepo;
	

	public Subject saveAllExams(Exams exam) {
		return addExamsRepo.saveAllExams(exam);
	}


	@Override
	public List<Exams> viewAllExams() {
		// TODO Auto-generated method stub
		return addExamsRepo.viewAllExams();
	}


	@Override
	public boolean deleteByIdExam(int exam_id) {
		// TODO Auto-generated method stub
		return addExamsRepo.deleteByIdExam(exam_id);
	}


	@Override
	public boolean upComingExam(int exam_id) {
		// TODO Auto-generated method stub
		return addExamsRepo.upComingExam( exam_id);
	}


	@Override
	public List<Exams> viewAllcomingExam() {
		// TODO Auto-generated method stub
		return addExamsRepo.viewAllcomingExam();
	}


	@Override
	public List<Exams> viewAllCompletedExam() {
		// TODO Auto-generated method stub
		return addExamsRepo.viewAllCompletedExam();
	}

}
