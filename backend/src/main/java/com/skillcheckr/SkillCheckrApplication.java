package com.skillcheckr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class SkillCheckrApplication {

    public static void main(String[] args) {
        loadDotEnv();

        ApplicationContext context = SpringApplication.run(SkillCheckrApplication.class, args);

        JdbcTemplate template = (JdbcTemplate) context.getBean("jdbcTemplate");
        if (template != null) {
            System.out.println(" Database is connected ");
        } else {
            System.out.println("Database is not Connected ");
        }
    }

    private static void loadDotEnv() {
        String[] possiblePaths = {".env", "../.env", "backend/.env"};
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (!key.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            }
        }
    }
}

