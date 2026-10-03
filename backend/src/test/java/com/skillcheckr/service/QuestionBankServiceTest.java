package com.skillcheckr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionBankDocument;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.repository.QuestionBankRepository;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private QuestionBankPdfGenerator pdfGenerator;

    @InjectMocks
    private QuestionBankService questionBankService;

    @Test
    void getStudentDocument_prefersUploadedPdf() {
        Exam exam = exam(9);
        QuestionBankDocument uploaded = new QuestionBankDocument("uploaded.pdf", new byte[] { 1 });
        when(questionBankRepository.findByExamId(9)).thenReturn(Optional.of(uploaded));

        assertEquals(uploaded, questionBankService.getStudentDocument(exam));
        verify(questionService, never()).getStudentQuestionsByExamId(9);
    }

    @Test
    void getStudentDocumentGeneratesPdfFromStoredQuestionsWhenNoUploadExists() {
        Exam exam = exam(9);
        List<QuestionDTO> questions = List.of(QuestionDTO.builder().question("Question").build());
        byte[] generatedPdf = new byte[] { 1, 2 };
        when(questionBankRepository.findByExamId(9)).thenReturn(Optional.empty());
        when(questionService.getStudentQuestionsByExamId(9)).thenReturn(questions);
        when(pdfGenerator.generate(exam, questions)).thenReturn(generatedPdf);

        QuestionBankDocument document = questionBankService.getStudentDocument(exam);

        assertEquals("question-bank.pdf", document.fileName());
        assertEquals(generatedPdf, document.content());
    }

    @Test
    void saveUploadedPdf_validatesAndStoresQuestionOnlyPdf() {
        byte[] pdf = new QuestionBankPdfGenerator().generate(exam(9),
                List.of(QuestionDTO.builder().question("Student question").build()));

        questionBankService.saveUploadedPdf(9, "../questions.pdf", pdf, true);

        verify(questionBankRepository).save(9, "questions.pdf", pdf);
    }

    @Test
    void saveUploadedPdf_requiresExplicitQuestionsOnlyConfirmation() {
        assertThrows(BadRequestException.class,
                () -> questionBankService.saveUploadedPdf(9, "questions.pdf", new byte[] { 1 }, false));
        verify(questionBankRepository, never()).save(9, "questions.pdf", new byte[] { 1 });
    }

    @Test
    void saveUploadedPdf_rejectsFilesThatAreNotReadablePdfs() {
        assertThrows(BadRequestException.class,
                () -> questionBankService.saveUploadedPdf(9, "questions.pdf", "not a pdf".getBytes(), true));
        verify(questionBankRepository, never()).save(9, "questions.pdf", "not a pdf".getBytes());
    }

    private Exam exam(int examId) {
        Exam exam = new Exam();
        exam.setExamId(examId);
        exam.setExamName("Sample exam");
        return exam;
    }
}
