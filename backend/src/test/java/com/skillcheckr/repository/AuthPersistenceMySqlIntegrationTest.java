package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.skillcheckr.model.UserProfileDTO;
import com.skillcheckr.persistence.entity.StudentProfileEntity;
import com.skillcheckr.persistence.entity.UserAccountEntity;
import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthRepositoryImpl.class)
@Testcontainers(disabledWithoutDocker = true)
class AuthPersistenceMySqlIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserAccountJpaRepository accountRepository;

    @Autowired
    private StudentProfileJpaRepository studentRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Test
    void flywaySchemaAndJpaAuthProfileOperationsWorkAgainstMySql() {
        UserAccountEntity account = new UserAccountEntity();
        account.setUsername("jpa-student");
        account.setPassword("$2a$10$stored-hash");
        account.setRole("Student");
        account.setAuthProvider("LOCAL");
        account.setStatus("Active");
        accountRepository.saveAndFlush(account);

        StudentProfileEntity student = new StudentProfileEntity();
        student.setUser(account);
        student.setName("Student");
        student.setEmail("jpa-student@example.test");
        student.setContact("555-0100");
        student.setStatus("Active");
        studentRepository.saveAndFlush(student);

        assertThat(authRepository.findByUsername("JPA-STUDENT")).isPresent();
        assertThat(authRepository.getStudentIdByUserId(account.getUserId())).isEqualTo(student.getStudentId());

        UserProfileDTO update = UserProfileDTO.builder()
                .userId(account.getUserId())
                .username("updated-student")
                .name("Updated Student")
                .email("updated@example.test")
                .contact("555-0101")
                .profileImage("profile-image")
                .build();
        UserProfileDTO updatedProfile = authRepository.updateUserProfile(update).orElseThrow();

        assertThat(updatedProfile.getUsername()).isEqualTo("updated-student");
        assertThat(updatedProfile.getRoleId()).isEqualTo(student.getStudentId());
        assertThat(updatedProfile.getName()).isEqualTo("Updated Student");
        assertThat(authRepository.getUserProfile(account.getUserId()).orElseThrow().getEmail())
                .isEqualTo("updated@example.test");
    }
}
