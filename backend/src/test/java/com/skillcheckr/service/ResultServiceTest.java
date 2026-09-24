package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.ExamResultDTO;
import com.skillcheckr.model.ExamSubmissionDTO;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.QuestionRepository;
import com.skillcheckr.repository.ResultRepository;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private ResultServiceImpl resultService;

    @Test
    void submitExam_calculatesMcqScore_andSavesResult() {
        Exam exam = new Exam();
        exam.setExamId(1);
        exam.setExamName("Maths Exam");
        exam.setExamType("MCQ");
        exam.setTotalMarks(100);
        exam.setPassingMarks(40);
        Subject subject = new Subject(1, "Maths", "MATH101");
        exam.setSubject(subject);

        QuestionDTO q1 = new QuestionDTO();
        q1.setQuestionId(10);
        q1.setQuestion("2+2?");
        q1.setCorrectOption("4");

        QuestionDTO q2 = new QuestionDTO();
        q2.setQuestionId(20);
        q2.setQuestion("5+5?");
        q2.setCorrectOption("10");

        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(questionRepository.getQuestionsByExamId(1)).thenReturn(List.of(q1, q2));
        when(resultRepository.createSubmittedAttempt(1, 7)).thenReturn(1);
        when(resultRepository.insertSubmissionResult(any(ExamResultDTO.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());

        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setExamId(1);
        submission.setStudentId(7);
        submission.setExamType("MCQ");
        submission.setStudentName("Alice");
        Map<String, String> answers = new HashMap<>();
        answers.put("10", "4");  // Correct -> 50 marks
        answers.put("20", "99"); // Wrong -> 0 marks
        submission.setMcqAnswers(answers);

        ExamResultDTO result = resultService.submitExam(submission);

        assertThat(result).isNotNull();
        assertThat(result.getMarksObtained()).isEqualTo(50);
        assertThat(result.getTotalMarks()).isEqualTo(100);
        assertThat(result.getPassingMarks()).isEqualTo(40);
        assertThat(result.getStatus()).isEqualTo("Pass");
        assertThat(result.getStudentName()).isEqualTo("Alice");

        verify(resultRepository).createSubmittedAttempt(1, 7);
        verify(resultRepository).insertSubmissionResult(any(ExamResultDTO.class));
    }

    @Test
    void submitExam_usesProvidedAttemptId_whenPresent() {
        Exam exam = new Exam();
        exam.setExamId(1);
        exam.setExamType("MCQ");

        when(examRepository.getExamById(1)).thenReturn(Optional.of(exam));
        when(questionRepository.getQuestionsByExamId(1)).thenReturn(List.of());
        when(resultRepository.insertSubmissionResult(any(ExamResultDTO.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());

        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setExamId(1);
        submission.setStudentId(7);
        submission.setAttemptId(5);

        ExamResultDTO result = resultService.submitExam(submission);

        assertThat(result.getAttemptId()).isEqualTo(5);
        verify(resultRepository, never()).createSubmittedAttempt(anyInt(), anyInt());
        verify(resultRepository).insertSubmissionResult(any(ExamResultDTO.class));
    }

    @Test
    void submitExam_handlesDisqualification() {
        Exam exam = new Exam();
        exam.setExamId(2);
        exam.setExamType("MCQ");
        when(examRepository.getExamById(2)).thenReturn(Optional.of(exam));
        when(questionRepository.getQuestionsByExamId(2)).thenReturn(List.of());
        when(resultRepository.insertSubmissionResult(any(ExamResultDTO.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());

        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setExamId(2);
        submission.setStudentId(8);
        submission.setDisqualified(true);
        submission.setDisqualificationReason("Tab switch limit exceeded");

        ExamResultDTO result = resultService.submitExam(submission);

        assertThat(result.isDisqualified()).isTrue();
        assertThat(result.getMarksObtained()).isEqualTo(0);
        assertThat(result.getStatus()).contains("Disqualified: Tab switch limit exceeded");
    }

    @Test
    void submitExam_handlesDescriptiveExam() {
        Exam exam = new Exam();
        exam.setExamId(3);
        exam.setExamType("QUESTION_ANSWER");
        exam.setTotalMarks(50);

        QuestionDTO q1 = new QuestionDTO();
        q1.setQuestionId(30);
        q1.setQuestion("Define encapsulation");
        q1.setSampleAnswer("Data hiding mechanism");

        when(examRepository.getExamById(3)).thenReturn(Optional.of(exam));
        when(questionRepository.getQuestionsByExamId(3)).thenReturn(List.of(q1));
        when(resultRepository.insertSubmissionResult(any(ExamResultDTO.class)))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());

        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setExamId(3);
        submission.setStudentId(9);
        submission.setExamType("QUESTION_ANSWER");
        Map<String, String> textAnswers = new HashMap<>();
        textAnswers.put("30", "Encapsulation is data wrapping");
        submission.setTextAnswers(textAnswers);

        ExamResultDTO result = resultService.submitExam(submission);

        assertThat(result.getStatus()).isEqualTo("Submitted for Evaluation");
        assertThat(result.getQuestionBreakdown()).hasSize(1);
    }

    @Test
    void getResultsByStudentId_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getResultsByStudentId(2)).thenReturn(List.of(dto));

        List<ExamResultDTO> actual = resultService.getResultsByStudentId(2);

        assertThat(actual).hasSize(1);
        verify(resultRepository).getResultsByStudentId(2);
    }

    @Test
    void getResultByExamAndStudent_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getResultByExamAndStudent(5, 2)).thenReturn(Optional.of(dto));

        assertThat(resultService.getResultByExamAndStudent(5, 2)).containsSame(dto);
        verify(resultRepository).getResultByExamAndStudent(5, 2);
    }

    @Test
    void getAllResults_delegatesToRepository() {
        ExamResultDTO dto = ExamResultDTO.builder().resultId(1).build();
        when(resultRepository.getAllResults()).thenReturn(List.of(dto));

        List<ExamResultDTO> actual = resultService.getAllResults();

        assertThat(actual).hasSize(1);
        verify(resultRepository).getAllResults();
    }
}