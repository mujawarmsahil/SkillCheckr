package com.skillcheckr.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;

class QuestionBankPdfGeneratorTest {

    @Test
    void generatedPdfContainsQuestionsAndOptionsButNotReferenceAnswers() throws IOException {
        Exam exam = new Exam();
        exam.setExamName("Physics Exam");
        QuestionDTO question = QuestionDTO.builder()
                .question("Which planet is closest to the Sun?")
                .option1("Mercury")
                .option2("Venus")
                .correctOption("Mercury")
                .sampleAnswer("PRIVATE REFERENCE ANSWER")
                .build();

        byte[] pdf = new QuestionBankPdfGenerator().generate(exam, List.of(question));
        String text;
        try (var document = Loader.loadPDF(pdf)) {
            text = new PDFTextStripper().getText(document);
        }

        assertTrue(text.contains("Which planet is closest to the Sun?"));
        assertTrue(text.contains("Mercury"));
        assertFalse(text.contains("PRIVATE REFERENCE ANSWER"));
    }
}
