package com.skillcheckr.service;

import com.skillcheckr.model.RegistrationRequest;
//import com.sun.net.httpserver.Request;

import java.util.*;

public interface RegistrationRequestService {
	
	 public boolean saveRequest(RegistrationRequest request);
	
	public  List<RegistrationRequest> getAllRequests();
	 
	public boolean deleteRequest(int id);

}
