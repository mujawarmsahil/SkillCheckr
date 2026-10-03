package com.skillcheckr.service;

import java.io.IOException;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionBankDocument;
import com.skillcheckr.repository.QuestionBankRepository;

@Service
public class QuestionBankService {

    public static final int MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024;
    private static final String GENERATED_PDF_NAME = "question-bank.pdf";

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionBankPdfGenerator pdfGenerator;

    public void saveUploadedPdf(int examId, String originalFilename, byte[] content, boolean questionOnlyConfirmed) {
        if (!questionOnlyConfirmed) {
            throw new BadRequestException("Confirm that the PDF contains student-facing questions only");
        }
        if (content == null || content.length == 0) {
            throw new BadRequestException("Choose a PDF file to upload");
        }
        if (content.length > MAX_PDF_SIZE_BYTES) {
            throw new BadRequestException("PDF files must be 10 MB or smaller");
        }
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported");
        }
        validatePdf(content);
        questionBankRepository.save(examId, safeFilename(originalFilename), content);
    }

    public QuestionBankDocument getStudentDocument(Exam exam) {
        Optional<QuestionBankDocument> uploaded = questionBankRepository.findByExamId(exam.getExamId());
        if (uploaded.isPresent()) {
            return uploaded.get();
        }

        var questions = questionService.getStudentQuestionsByExamId(exam.getExamId());
        if (questions.isEmpty()) {
            throw new ResourceNotFoundException("Question bank not found");
        }
        return new QuestionBankDocument(GENERATED_PDF_NAME, pdfGenerator.generate(exam, questions));
    }

    private void validatePdf(byte[] content) {
        if (content.length < 5 || content[0] != '%' || content[1] != 'P' || content[2] != 'D'
                || content[3] != 'F' || content[4] != '-') {
            throw new BadRequestException("The uploaded file is not a PDF");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0) {
                throw new BadRequestException("Upload a readable PDF with at least one page");
            }
        } catch (IOException ex) {
            throw new BadRequestException("The uploaded PDF could not be opened");
        }
    }

    private String safeFilename(String originalFilename) {
        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (filename.isEmpty() || filename.length() > 255) {
            return "question-bank.pdf";
        }
        return filename;
    }
}
