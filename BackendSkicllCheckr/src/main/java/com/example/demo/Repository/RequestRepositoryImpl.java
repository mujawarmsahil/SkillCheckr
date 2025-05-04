package com.example.demo.Repository;




import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.demo.Model.RequestModel;

@Repository
public class RequestRepositoryImpl implements RequestRepository {
	
	List<RequestModel> list;

    @Autowired
    private DataSource dataSource;
    
    
    @Autowired
    private JdbcTemplate jdbcTemplate; 
    

    @Override
    public boolean saveRequest(RequestModel request) {
        int result = 0;
//        System.out.println("Database is Connected ");
        String sql = "INSERT INTO Request (name, contact, email, requested_role, status,username,password) VALUES (?, ?, ?, ?, ?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getName());
            ps.setString(2, request.getContact());
            ps.setString(3, request.getEmail());
            ps.setString(4, request.getRequested_role());
            ps.setString(5, "Pending");
            ps.setString(6, request.getUsername());  
            ps.setString(7, request.getPassword());  

            result = ps.executeUpdate(); 

        } catch (Exception e) {
            e.printStackTrace(); // log properly 
        }
        return result >0?true :false;  
        }

	
    @Override
    public List<RequestModel> getAllRequest() {
        List<RequestModel> list = jdbcTemplate.query("SELECT * FROM Request", new RowMapper<RequestModel>() {
            @Override
            public RequestModel mapRow(ResultSet rs, int rowNum) throws SQLException {
                RequestModel rqm = new RequestModel();
                rqm.setRequest_id(rs.getInt("request_id")); 
                rqm.setName(rs.getString("name"));
                rqm.setContact(rs.getString("contact"));
                rqm.setEmail(rs.getString("email"));
                rqm.setRequested_role(rs.getString("requested_role"));
                rqm.setStatus(rs.getString("status")); // if you added status in model
                
                return rqm;
            }
        });
        return list;
    }

    
    @Override
    public RequestModel getRequestById(int id) {
        String query = "SELECT * FROM request WHERE request_id = ?";

        return jdbcTemplate.queryForObject(query, new org.springframework.jdbc.core.RowMapper<RequestModel>() {
            @Override
            public RequestModel mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                RequestModel req = new RequestModel();
                req.setRequest_id(rs.getInt("request_id"));
                req.setName(rs.getString("name"));
                req.setContact(rs.getString("contact"));
                req.setEmail(rs.getString("email"));
                req.setUsername(rs.getString("username"));
                req.setPassword(rs.getString("password"));
                req.setRequested_role(rs.getString("requested_role"));
                req.setStatus(rs.getString("status"));
                return req;
            }
        }, id); // 👈 arguments at the end
    }


	@Override
	public boolean deleteRequestById(int id) {
		// TODO Auto-generated method stub
		int value=jdbcTemplate.update("delete from request where request_id="+id);
		return value>0?true:false;
//		return false;
	}

	
	
}
