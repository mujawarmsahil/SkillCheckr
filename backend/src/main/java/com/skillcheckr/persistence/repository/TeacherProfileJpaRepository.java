package com.skillcheckr.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.TeacherProfileEntity;

public interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileEntity, Integer> {

    Optional<TeacherProfileEntity> findByUser_UserId(Integer userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndUser_UserIdNot(String email, Integer userId);

    List<TeacherProfileEntity> findAllByOrderByTeacherIdDesc();
}
