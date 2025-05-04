package com.example.demo.Service;

import com.example.demo.Model.RequestModel;
//import com.sun.net.httpserver.Request;

import java.util.*;

public interface RequestService {
	
	 public boolean saveRequest(RequestModel request);
	
	public  List<RequestModel> getAllRequests();
	 
	public boolean deleteRequest(int id);

}
