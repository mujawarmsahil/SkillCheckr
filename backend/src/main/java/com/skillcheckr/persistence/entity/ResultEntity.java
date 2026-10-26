package com.skillcheckr.persistence.entity;

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
@Table(name = "result")
@Getter
@Setter
@NoArgsConstructor
public class ResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Integer resultId;

    @Column(name = "attempt_id", nullable = false)
    private Integer attemptId;

    @Column(name = "exam_id", nullable = false)
    private Integer examId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "marks_obtained", nullable = false)
    private Integer marksObtained;

    @Column(name = "percentage")
    private Double percentage;

    @Column(name = "passing_marks")
    private Integer passingMarks;

    @Column(name = "submitted_at", columnDefinition = "DATETIME")
    private java.time.LocalDateTime submittedAt;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "version", nullable = false)
    private Integer version;
}
