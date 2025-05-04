package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Exams;
import com.example.demo.Repository.AddExamsRepository;


@Service
public class AddExamsServiceImpl implements AddExamsService{

	@Autowired
	public AddExamsRepository addExamsRepo;
	

	public boolean saveAllExams(Exams exam) {
		// TODO Auto-generated method stub
		return addExamsRepo.saveAllExams(exam);
	}

}
