package com.example.demo.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.demo.Model.User;

@Repository
public class AuthRepositoryImpl implements AuthRepository   {
	
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public User login(String username, String password) {
		// TODO Auto-generated method stub
		
		String sql = "SELECT *FROM user WHERE username = ? AND password = ? ";
		return jdbcTemplate.queryForObject(sql, new Object[] {username,password}, new  RowMapper<User>() {
		
			@Override
		    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
		        User u = new User();
		        u.setUser_id(rs.getInt("user_id"));
		        u.setUsername(rs.getString("username"));
		        u.setPassword(rs.getString("password"));
		        u.setUser_role(rs.getString("user_role"));
		        return u;
		    }
		
			
		});
			 
		 
//		return null;
	}

	@Override
	public int getStudentIdByUserId(int userId) {
		// TODO Auto-generated method stub
		String sql = "SELECT student_id FROM student WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{userId}, Integer.class);
	}

	@Override
	public int getTeacherIdByUserId(int userId) {
		// TODO Auto-generated method stub
		String sql = "SELECT teacher_id FROM teacher WHERE user_id = ? ";
		return jdbcTemplate.queryForObject(sql,new Object[] {userId}, Integer.class);
//		return 0;
	}

	@Override
	public int getAdminIdByUserId(int userId) {
		 String sql = "SELECT admin_id FROM admin WHERE user_id = ?";
		    return jdbcTemplate.queryForObject(sql, new Object[]{userId}, Integer.class);
	}
	
	
	
	

}
