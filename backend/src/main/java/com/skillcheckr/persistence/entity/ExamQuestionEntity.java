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
@Table(name = "exam_question")
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_question_id")
    private Integer examQuestionId;

    @Column(name = "exam_id", nullable = false)
    private Integer examId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
}
