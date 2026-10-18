package com.skillcheckr.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.AdminProfileEntity;

public interface AdminProfileJpaRepository extends JpaRepository<AdminProfileEntity, Integer> {

    Optional<AdminProfileEntity> findByUser_UserId(Integer userId);

    boolean existsByEmailIgnoreCaseAndUser_UserIdNot(String email, Integer userId);
}
