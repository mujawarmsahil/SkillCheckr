package com.skillcheckr.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.StudentProfileEntity;

public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileEntity, Integer> {

    Optional<StudentProfileEntity> findByUser_UserId(Integer userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndUser_UserIdNot(String email, Integer userId);

    List<StudentProfileEntity> findAllByOrderByStudentIdDesc();
}
