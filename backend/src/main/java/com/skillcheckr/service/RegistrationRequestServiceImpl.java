package com.skillcheckr.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.skillcheckr.model.RegistrationRequest;
import com.skillcheckr.repository.RegistrationRequestRepository;

@Service
public class RegistrationRequestServiceImpl implements RegistrationRequestService {

	@Autowired
    private RegistrationRequestRepository requestRepository;
	
	@Override
	public boolean saveRequest(RegistrationRequest request) {
		// TODO Auto-generated method stub
		return requestRepository.saveRequest(request);
	}
//
	@Override
	public List<RegistrationRequest> getAllRequests() {
		// TODO Auto-generated method stub
		return requestRepository.getAllRequests();
	}

	@Override
	public boolean  deleteRequest(int id) {
		// TODO Auto-generated method stub
		return requestRepository.deleteRequestById(id);
	}

}
