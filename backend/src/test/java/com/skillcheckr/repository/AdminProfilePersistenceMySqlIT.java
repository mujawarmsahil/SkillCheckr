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

import com.skillcheckr.persistence.repository.StudentProfileJpaRepository;
import com.skillcheckr.persistence.repository.UserAccountJpaRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AdminRepositoryImpl.class)
@Testcontainers(disabledWithoutDocker = true)
class AdminProfilePersistenceMySqlIT {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private AdminRepository adminRepository;

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
    void accountApprovalProfileLifecycleUsesTheMigratedSchema() {
        int userId = adminRepository.createUser("admin-flow-student", "$2a$10$test-hash", "Student");
        adminRepository.createStudent(userId, "Student", "555-0100", "admin-flow@example.test");

        assertThat(adminRepository.getAllStudent()).singleElement().satisfies(student -> {
            assertThat(student.getUserId()).isEqualTo(userId);
            assertThat(student.getStatus()).isEqualTo("Active");
        });

        int studentId = studentRepository.findByUser_UserId(userId).orElseThrow().getStudentId();
        assertThat(adminRepository.toggleStudentStatus(studentId, "Inactive")).isTrue();
        assertThat(accountRepository.findById(userId).orElseThrow().getStatus()).isEqualTo("Inactive");

        assertThat(adminRepository.deleteStudentById(studentId)).isTrue();
        assertThat(studentRepository.findById(studentId)).isEmpty();
        assertThat(accountRepository.findById(userId)).isEmpty();
    }
}
