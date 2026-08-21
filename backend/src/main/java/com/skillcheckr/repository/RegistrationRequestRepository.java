package com.skillcheckr.repository;
import java.util.*;
import com.skillcheckr.model.RegistrationRequest;

public interface RegistrationRequestRepository {
	
public boolean saveRequest(RegistrationRequest request);



 public List <RegistrationRequest> getAllRequests();
 
 
 public boolean deleteRequestById(int id);
 
 
 RegistrationRequest getRequestById(int id);  
 
}
