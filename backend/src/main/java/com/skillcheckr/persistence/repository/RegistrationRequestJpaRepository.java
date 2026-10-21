package com.skillcheckr.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillcheckr.persistence.entity.RegistrationRequestEntity;

public interface RegistrationRequestJpaRepository
        extends JpaRepository<RegistrationRequestEntity, Integer> {

    boolean existsByUsernameIgnoreCase(String username);

    List<RegistrationRequestEntity> findAllByOrderByRequestIdDesc();

    /**
     * Writes the status in one statement without loading the entity, and returns the row
     * count the caller turns into a boolean.
     */
    @Modifying
    @Query("UPDATE RegistrationRequestEntity r SET r.status = :status WHERE r.requestId = :requestId")
    int updateStatus(@Param("requestId") int requestId, @Param("status") String status);

    /**
     * Compare-and-set transition in one statement, so a lifecycle change cannot be
     * applied twice even under concurrent requests.
     */
    @Modifying
    @Query("UPDATE RegistrationRequestEntity r SET r.status = :status "
            + "WHERE r.requestId = :requestId AND r.status = :expectedStatus")
    int updateStatusIfCurrent(@Param("requestId") int requestId, @Param("status") String status,
            @Param("expectedStatus") String expectedStatus);

    /**
     * Deletes in one statement and returns the row count the caller turns into a boolean.
     */
    @Modifying
    @Query("DELETE FROM RegistrationRequestEntity r WHERE r.requestId = :requestId")
    int deleteByRequestId(@Param("requestId") int requestId);
}
