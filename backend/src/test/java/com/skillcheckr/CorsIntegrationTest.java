package com.skillcheckr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorsPreflightForVercelFrontend() throws Exception {
        mockMvc.perform(options("/api/authentication/login")
                .header("Origin", "https://skill-checkr.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://skill-checkr.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testCorsPreflightForLocalhost() throws Exception {
        mockMvc.perform(options("/api/authentication/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
