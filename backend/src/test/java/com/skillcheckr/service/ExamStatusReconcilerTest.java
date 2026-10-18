package com.skillcheckr.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.repository.ExamRepository;

@ExtendWith(MockitoExtension.class)
class ExamStatusReconcilerTest {

    @Mock
    private ExamRepository examRepository;

    @InjectMocks
    private ExamStatusReconciler reconciler;

    @Test
    void reconcilesStatusesThroughTheRepository() {
        reconciler.reconcileCompletedExams();

        verify(examRepository).syncExamStatuses();
    }
}
