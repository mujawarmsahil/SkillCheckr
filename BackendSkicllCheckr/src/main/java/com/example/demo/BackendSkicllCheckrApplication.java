package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BackendSkicllCheckrApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(BackendSkicllCheckrApplication.class, args);
		
		JdbcTemplate template= (JdbcTemplate)context.getBean("jdbcTemplate");
		if(template!=null)
		{
			 System.out.println(" Database is connected ");
		}
		else {
			System.out.println("Database is not Connected ");
		}
		
	}

}
