package com.skillcheckr.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam_attempt")
@Getter
@Setter
@NoArgsConstructor
public class ExamAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Integer attemptId;

    @Column(name = "exam_id", nullable = false)
    private Integer examId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime expiresAt;

    @Column(name = "submitted_at", columnDefinition = "DATETIME")
    private LocalDateTime submittedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
