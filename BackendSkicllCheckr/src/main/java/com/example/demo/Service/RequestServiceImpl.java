package com.example.demo.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.demo.Model.RequestModel;
import com.example.demo.Repository.RequestRepository;

@Service
public class RequestServiceImpl implements RequestService {
//public class RequestRepositoryImpl implements RequestService {


	
	@Autowired
    private RequestRepository requestRepository;
	
	@Override
	public boolean saveRequest(RequestModel request) {
		// TODO Auto-generated method stub
		return requestRepository.saveRequest(request);
	}
//
	@Override
	public List<RequestModel> getAllRequests() {
		// TODO Auto-generated method stub
		return requestRepository.getAllRequest();
	}

	@Override
	public boolean  deleteRequest(int id) {
		// TODO Auto-generated method stub
		return requestRepository.deleteRequestById(id);
	}

}
