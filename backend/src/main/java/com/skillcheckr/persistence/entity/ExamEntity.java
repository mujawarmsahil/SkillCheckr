package com.skillcheckr.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam")
@Getter
@Setter
@NoArgsConstructor
public class ExamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Integer examId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "exam_name", nullable = false, length = 150)
    private String examName;

    @Column(name = "exam_type", length = 50)
    private String examType;

    @Column(name = "exam_date", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime examDate;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIME")
    private java.time.LocalTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIME")
    private java.time.LocalTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "pass_marks", nullable = false)
    private Integer passMarks;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "paper_frozen")
    private Boolean paperFrozen;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
