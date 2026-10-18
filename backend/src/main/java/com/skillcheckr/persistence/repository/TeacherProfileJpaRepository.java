package com.skillcheckr.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.TeacherProfileEntity;

public interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileEntity, Integer> {

    Optional<TeacherProfileEntity> findByUser_UserId(Integer userId);

    boolean existsByEmailIgnoreCaseAndUser_UserIdNot(String email, Integer userId);
}
