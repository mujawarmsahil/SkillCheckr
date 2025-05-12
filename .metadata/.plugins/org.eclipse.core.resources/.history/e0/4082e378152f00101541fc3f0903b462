package com.example.demo.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import com.example.demo.Model.RequestModel;
import com.example.demo.Service.RequestService;


@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "http://localhost:5173")
public class RequestController {

	@Autowired
	private RequestService reqService;

	@PostMapping("/save")
	public ResponseEntity<String> saveRequest(@RequestBody RequestModel request) {
		try {
		boolean result = reqService.saveRequest(request);
           if (result) {
			return ResponseEntity.ok("Request Submited Succefully");
		} else {
			return ResponseEntity.status(500).body("failed to submit request ");
		}
		}catch (Exception e){
			
			System.out.print(" show the error "+e);
			 return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}

	}

	@GetMapping("/viewAllRegisterUsers")
	public ResponseEntity<?> getAllRequests() {
		List<RequestModel> list = reqService.getAllRequests();

		if (list == null || list.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No registration requests found.");
			
			
		} else {

			System.out.println(list);
			return ResponseEntity.ok(list);
		}
	}
	
	@DeleteMapping("/deleteById/{request_id}")
	public ResponseEntity<String> deleteRequest(@PathVariable Integer request_id)
	{
		
		boolean b = reqService.deleteRequest(request_id);
		if(b)
		{
			 return ResponseEntity.ok("Delete the Data Succefully ");
		}
		
		else {
			 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Some ishuee data not deleted");
		}
		 
	}

}
