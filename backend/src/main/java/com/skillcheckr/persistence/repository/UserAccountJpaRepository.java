package com.skillcheckr.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillcheckr.persistence.entity.UserAccountEntity;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Integer> {

    Optional<UserAccountEntity> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndUserIdNot(String username, Integer userId);
}
