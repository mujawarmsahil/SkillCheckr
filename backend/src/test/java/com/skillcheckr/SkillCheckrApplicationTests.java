package com.skillcheckr;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class SkillCheckrApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		public DataSource dataSource() throws Exception {
			DataSource ds = Mockito.mock(DataSource.class);
			Connection conn = Mockito.mock(Connection.class);
			DatabaseMetaData meta = Mockito.mock(DatabaseMetaData.class);
			Mockito.when(ds.getConnection()).thenReturn(conn);
			Mockito.when(conn.getMetaData()).thenReturn(meta);
			Mockito.when(meta.getDatabaseProductName()).thenReturn("MySQL");
			Mockito.when(conn.isValid(Mockito.anyInt())).thenReturn(true);
			return ds;
		}
	}

	@Test
	void contextLoads() {
	}

}

