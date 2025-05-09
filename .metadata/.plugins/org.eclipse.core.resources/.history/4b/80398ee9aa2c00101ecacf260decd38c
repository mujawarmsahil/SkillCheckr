package com.example.demo.Controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Model.Exams;
import com.example.demo.Model.Subject;
import com.example.demo.Service.AddExamsService;

@RestController
@RequestMapping("/api/Exams")
@CrossOrigin(origins="https://localhost:5173")
public class AddExamsConntroller {
	
	@Autowired
	public AddExamsService addExamsService;
	
	
	
	@PostMapping("/addExams")
	public ResponseEntity<?> AddExmas(@RequestBody Exams exam){
		
	Subject  subject = addExamsService.saveAllExams(exam);
		if(subject!=null) {
			 return ResponseEntity.ok(subject);
		}
		else {
//			return ResponseEntity.status(500).body("Exams not Added");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exams not Added ");
			 
		}
	}
	
	
	@GetMapping("/viewAllExams")
	public ResponseEntity<?> viewAllExams()
	{
		List<Exams> list = addExamsService.viewAllExams() ;
		if(list==null||list.isEmpty())
		{
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No registration requests found.");
		}
		else {
			System.out.println("Show the List of the All Exams View  "+list);
			
			return ResponseEntity.ok(list);
		}
	}
	
	
	@DeleteMapping("/deleteExamById/{exam_id}")
	public ResponseEntity<String> deletExasm(@PathVariable Integer exam_id)
	{
		boolean b = addExamsService.deleteByIdExam(exam_id);
		if(b) {
			return ResponseEntity.ok("Deleting the Exasm");
		}
		else {
			 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Some Problem Data is not Deleted");
		}
		
//		return null;
		 
	}
	
	
	@PostMapping("/upComingExam")
	public ResponseEntity<?> upComingExams()
	{
		 
		return null;
	}
	
	@PostMapping("/completedExam")
	public ResponseEntity<?> completedExams()
	{ 
	
	return null;
	}

}
