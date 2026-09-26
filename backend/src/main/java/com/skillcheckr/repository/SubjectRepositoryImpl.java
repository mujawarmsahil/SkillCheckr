package com.skillcheckr.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.skillcheckr.model.Subject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class SubjectRepositoryImpl implements SubjectRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Subject> subjectRowMapper = new RowMapper<Subject>() {
        @Override
        public Subject mapRow(ResultSet rs, int rowNum) throws SQLException {
            Subject s = new Subject();
            s.setSubjectId(rs.getInt("subject_id"));
            s.setSubjectName(rs.getString("subject_name"));
            s.setSubjectCode(rs.getString("subject_code"));
            return s;
        }
    };

    @Override
    public List<Subject> getAllSubjects() {
        try {
            return jdbcTemplate.query("SELECT * FROM subject ORDER BY subject_id DESC", subjectRowMapper);
        } catch (Exception e) {
            log.error("Error fetching subjects", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Optional<Subject> getSubjectById(int subjectId) {
        try {
            String sql = "SELECT * FROM subject WHERE subject_id = ?";
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, subjectRowMapper, subjectId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching subject by ID", e);
            return Optional.empty();
        }
    }

    @Override
    public Subject addSubject(Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return null;
        }

        String name = subject.getSubjectName().trim();
        String code = subject.getSubjectCode() != null && !subject.getSubjectCode().trim().isEmpty()
                ? subject.getSubjectCode().trim().toUpperCase()
                : generateSubjectCode(name);

        try {
            Integer existingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM subject WHERE subject_code = ?", Integer.class, code);
            if (existingCount != null && existingCount > 0) {
                // Generate a unique suffix
                code = code + "-" + (int) (Math.random() * 900 + 100);
            }

            String insertSql = "INSERT INTO subject (subject_name, subject_code) VALUES (?, ?)";
            final String finalCode = code;
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, finalCode);
                return ps;
            }, keyHolder);

            Number id = keyHolder.getKey();
            int newId = id != null ? id.intValue() : 0;
            return new Subject(newId, name, finalCode);
        } catch (Exception e) {
            log.error("Error adding subject", e);
            return null;
        }
    }

    @Override
    public boolean updateSubject(int subjectId, Subject subject) {
        if (subject == null || subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            return false;
        }

        String name = subject.getSubjectName().trim();
        String code = subject.getSubjectCode() != null && !subject.getSubjectCode().trim().isEmpty()
                ? subject.getSubjectCode().trim().toUpperCase()
                : generateSubjectCode(name);

        try {
            String updateSql = "UPDATE subject SET subject_name = ?, subject_code = ? WHERE subject_id = ?";
            int rows = jdbcTemplate.update(updateSql, name, code, subjectId);
            return rows > 0;
        } catch (Exception e) {
            log.error("Error updating subject", e);
            return false;
        }
    }

    @Override
    public boolean deleteSubjectById(int subjectId) {
        try {
            List<Integer> questionIds = jdbcTemplate.query(
                    "SELECT question_id FROM question WHERE subject_id = ?",
                    (rs, rowNum) -> rs.getInt("question_id"), subjectId);

            for (Integer qId : questionIds) {
                jdbcTemplate.update("DELETE FROM answer WHERE question_id = ?", qId);
            }
            jdbcTemplate.update("DELETE FROM question WHERE subject_id = ?", subjectId);

            jdbcTemplate.update("DELETE FROM exam WHERE subject_id = ?", subjectId);

            int rows = jdbcTemplate.update("DELETE FROM subject WHERE subject_id = ?", subjectId);
            return rows > 0;
        } catch (Exception e) {
            log.error("Error deleting subject", e);
            return false;
        }
    }

    @Override
    public int getQuestionCountBySubjectId(int subjectId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM question WHERE subject_id = ?", Integer.class, subjectId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getExamCountBySubjectId(int subjectId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM exam WHERE subject_id = ?", Integer.class, subjectId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String generateSubjectCode(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 4) {
            base = base.substring(0, 4);
        }
        if (base.isEmpty()) {
            base = "SUB";
        }
        return base + ((int) (Math.random() * 900 + 100));
    }
}
