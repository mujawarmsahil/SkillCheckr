package com.example.demo.Repository;
import java.util.*;
import com.example.demo.Model.RequestModel;

public interface RequestRepository {
	
public boolean saveRequest(RequestModel request);



 public List <RequestModel> getAllRequest();
 
 
 public boolean deleteRequestById(int id);
 
 
 RequestModel getRequestById(int id);  
 
}
