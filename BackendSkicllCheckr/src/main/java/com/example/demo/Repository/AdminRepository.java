package com.example.demo.Repository;

import com.example.demo.Model.*;
import java.util.*;

public interface AdminRepository {
	
	boolean addTeacherFromRequest(int request_id);
	boolean addStudentFromRequest(int request_id);
    boolean isUsernameExist(String username);
    boolean changeExamStatus(int examId, String status);
    public List<Teacher> getAllTeacher();
    public List<Student> getAllStudent();
    public boolean deleteTeacherbyId(int Teacher_id);
    public boolean deleteStudentid(int Student_id) ;
    

}
