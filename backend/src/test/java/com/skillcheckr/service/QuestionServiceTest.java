package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.constant.ExamConstants;
import com.skillcheckr.exception.BadRequestException;
import com.skillcheckr.exception.ExamInUseException;
import com.skillcheckr.exception.ResourceNotFoundException;
import com.skillcheckr.model.Exam;
import com.skillcheckr.model.Question;
import com.skillcheckr.model.QuestionDTO;
import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.AttemptAnswerRepository;
import com.skillcheckr.repository.ExamQuestionRepository;
import com.skillcheckr.repository.ExamRepository;
import com.skillcheckr.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private static QuestionDTO mcq() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("2+2?");
        dto.setSubjectId(1);
        dto.setOption1("3");
        dto.setOption2("4");
        dto.setCorrectOption("4");
        return dto;
    }

    private static QuestionDTO subjective() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("Explain polymorphism");
        dto.setSubjectId(1);
        dto.setQuestionType(ExamConstants.QUESTION_TYPE_QUESTION_ANSWER);
        return dto;
    }

    private static Exam openExam(int examId, int subjectId) {
        Exam exam = new Exam();
        exam.setExamId(examId);
        exam.setStatus(ExamConstants.EXAM_STATUS_UPCOMING);
        exam.setDate("2099-01-15 09:00:00");
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(12, 0));
        Subject subject = new Subject();
        subject.setSubjectId(subjectId);
        exam.setSubject(subject);
        return exam;
    }

    @Test
    void saveQuestionsWithAnswers_forwardsAValidListToTheRepository() {
        QuestionDTO dto = mcq();

        questionService.saveQuestionsWithAnswers(List.of(dto));

        verify(questionRepository).saveQuestionWithAnswers(List.of(dto));
    }

    @Test
    void saveQuestionsWithAnswers_rejectsAnEmptyList() {
        assertThatThrownBy(() -> questionService.saveQuestionsWithAnswers(List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one question is required");
        verify(questionRepository, never()).saveQuestionWithAnswers(anyList());
    }

    @Test
    void saveQuestionsWithAnswers_rejectsAMcqWithoutEnoughOptions() {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestion("2+2?");
        dto.setSubjectId(1);
        dto.setOption1("3");
        dto.setCorrectOption("3");

        assertThatThrownBy(() -> questionService.saveQuestionsWithAnswers(List.of(dto)))
                .isInstanceOf(BadRequestException.class);
        verify(questionRepository, never()).saveQuestionWithAnswers(anyList());
    }

    @Test
    void getQuestionsBySubjectId_delegatesToRepository() {
        when(questionRepository.getQuestionsBySubjectId(1)).thenReturn(List.of(mcq()));

        assertThat(questionService.getQuestionsBySubjectId(1)).hasSize(1);
        verify(questionRepository).getQuestionsBySubjectId(1);
    }

    @Test
    void getQuestionsByExamId_delegatesToRepository() {
        when(questionRepository.getQuestionsByExamId(2)).thenReturn(List.of(mcq()));

        assertThat(questionService.getQuestionsByExamId(2)).hasSize(1);
        verify(questionRepository).getQuestionsByExamId(2);
    }

    @Test
    void getStudentQuestionsByExamId_stripsTheAnswerKey() {
        QuestionDTO dto = mcq();
        dto.setSampleAnswer("the reference answer");
        when(questionRepository.getQuestionsByExamId(2)).thenReturn(List.of(dto));

        List<QuestionDTO> questions = questionService.getStudentQuestionsByExamId(2);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getCorrectOption()).isNull();
        assertThat(questions.get(0).getSampleAnswer()).isNull();
        assertThat(questions.get(0).getOption1()).isEqualTo("3");
    }

    @Test
    void attachQuestionsToExam_attachesDistinctQuestionsInOrder() {
        when(examRepository.getExamById(3)).thenReturn(Optional.of(openExam(3, 1)));
        when(examQuestionRepository.getNextQuestionOrder(3)).thenReturn(1);
        when(questionRepository.findQuestionDetailsById(anyInt())).thenReturn(Optional.of(questionInSubject(1)));
        when(examQuestionRepository.attachQuestion(anyInt(), anyInt(), anyInt())).thenReturn(true);

        int attached = questionService.attachQuestionsToExam(3, List.of(10, 10, 11));

        assertThat(attached).isEqualTo(2);
        verify(examQuestionRepository).attachQuestion(3, 10, 1);
        verify(examQuestionRepository).attachQuestion(3, 11, 2);
    }

    @Test
    void attachQuestionsToExam_countsOnlyNewlyAttachedQuestions() {
        when(examRepository.getExamById(3)).thenReturn(Optional.of(openExam(3, 1)));
        when(examQuestionRepository.getNextQuestionOrder(3)).thenReturn(1);
        when(questionRepository.findQuestionDetailsById(anyInt())).thenReturn(Optional.of(questionInSubject(1)));
        // 10 is new and gets a row; 11 is already in the exam, so the repository inserts nothing.
        when(examQuestionRepository.attachQuestion(3, 10, 1)).thenReturn(true);
        when(examQuestionRepository.attachQuestion(3, 11, 2)).thenReturn(false);

        int attached = questionService.attachQuestionsToExam(3, List.of(10, 11));

        assertThat(attached).isEqualTo(1);
    }

    @Test
    void attachQuestionsToExam_doesNotConsumeAnOrderForAnAlreadyAttachedQuestion() {
        when(examRepository.getExamById(3)).thenReturn(Optional.of(openExam(3, 1)));
        when(examQuestionRepository.getNextQuestionOrder(3)).thenReturn(1);
        when(questionRepository.findQuestionDetailsById(anyInt())).thenReturn(Optional.of(questionInSubject(1)));
        when(examQuestionRepository.attachQuestion(3, 10, 1)).thenReturn(false);
        when(examQuestionRepository.attachQuestion(3, 11, 1)).thenReturn(true);

        questionService.attachQuestionsToExam(3, List.of(10, 11));

        // The skipped attachment must not leave a gap in question_order.
        verify(examQuestionRepository).attachQuestion(3, 11, 1);
    }

    @Test
    void attachQuestionsToExam_rejectsAnEmptySelection() {
        assertThatThrownBy(() -> questionService.attachQuestionsToExam(3, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Select at least one question to add to the exam");
    }

    @Test
    void attachQuestionsToExam_reportsAMissingExam() {
        when(examRepository.getExamById(3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.attachQuestionsToExam(3, List.of(10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exam not found with id: 3");
    }

    @Test
    void attachQuestionsToExam_rejectsQuestionsFromAnotherSubject() {
        when(examRepository.getExamById(3)).thenReturn(Optional.of(openExam(3, 1)));
        when(examQuestionRepository.getNextQuestionOrder(3)).thenReturn(1);
        when(questionRepository.findQuestionDetailsById(10)).thenReturn(Optional.of(questionInSubject(2)));

        assertThatThrownBy(() -> questionService.attachQuestionsToExam(3, List.of(10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("belongs to a different subject");
        verify(examQuestionRepository, never()).attachQuestion(anyInt(), anyInt(), anyInt());
    }

    @Test
    void attachQuestionsToExam_refusesOnceTheExamHasStarted() {
        Exam started = openExam(3, 1);
        started.setDate("2020-01-15 09:00:00");
        started.setStartTime(LocalTime.of(9, 0));
        started.setEndTime(LocalTime.of(12, 0));
        when(examRepository.getExamById(3)).thenReturn(Optional.of(started));

        assertThatThrownBy(() -> questionService.attachQuestionsToExam(3, List.of(10)))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("question paper is locked");
    }

    private static Question questionInSubject(int subjectId) {
        Question question = new Question();
        question.setQuestionId(10);
        Subject subject = new Subject();
        subject.setSubjectId(subjectId);
        question.setSubject(subject);
        return question;
    }

    @Test
    void deleteQuestionById_delegatesToRepository() {
        when(examQuestionRepository.isQuestionAssignedToAnyExam(3)).thenReturn(false);
        when(questionRepository.deleteQuestionById(3)).thenReturn(true);

        assertThat(questionService.deleteQuestionById(3)).isTrue();
        verify(questionRepository).deleteQuestionById(3);
    }

    @Test
    void deleteQuestionById_refusesToRemoveAQuestionUsedByAnExam() {
        when(examQuestionRepository.isQuestionAssignedToAnyExam(3)).thenReturn(true);

        assertThatThrownBy(() -> questionService.deleteQuestionById(3))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("cannot be deleted");
        verify(questionRepository, never()).deleteQuestionById(anyInt());
    }

    @Test
    void getQuestionById_delegatesToRepository() {
        when(questionRepository.getQuestionById(4)).thenReturn(Optional.of(mcq()));

        assertThat(questionService.getQuestionById(4)).isPresent();
    }

    @Test
    void updateQuestion_rejectsEditWhenTheQuestionHasAttemptHistory() {
        QuestionDTO dto = mcq();
        dto.setQuestionId(5);
        when(attemptAnswerRepository.hasAttemptHistory(5)).thenReturn(true);

        assertThatThrownBy(() -> questionService.updateQuestion(dto))
                .isInstanceOf(ExamInUseException.class)
                .hasMessageContaining("already been attempted");

        verify(questionRepository, never()).updateQuestion(any());
    }

    @Test
    void updateQuestion_rejectsSubjectChangeWhenTheQuestionIsAssignedToAnExam() {
        QuestionDTO dto = mcq();
        dto.setQuestionId(5);
        dto.setSubjectId(2);
        when(attemptAnswerRepository.hasAttemptHistory(5)).thenReturn(false);
        when(examQuestionRepository.isQuestionAssignedToAnyExam(5)).thenReturn(true);
        when(examQuestionRepository.findSubjectIdsByQuestionId(5)).thenReturn(List.of(1));

        assertThatThrownBy(() -> questionService.updateQuestion(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different subject");

        verify(questionRepository, never()).updateQuestion(any());
    }
}
