package com.skillcheckr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.skillcheckr.model.Subject;
import com.skillcheckr.persistence.entity.SubjectEntity;
import com.skillcheckr.persistence.repository.SubjectJpaRepository;

@ExtendWith(MockitoExtension.class)
class SubjectRepositoryImplTest {

    @Mock
    private SubjectJpaRepository subjectRepository;

    @InjectMocks
    private SubjectRepositoryImpl repository;

    @Test
    void getAllSubjects_propagatesDatabaseFailure() {
        when(subjectRepository.findAllByOrderBySubjectIdDesc())
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getAllSubjects())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getAllSubjects_mapsEntitiesInRepositoryOrder() {
        SubjectEntity entity = new SubjectEntity();
        entity.setSubjectId(3);
        entity.setSubjectName("Maths");
        entity.setSubjectCode("MATH");
        when(subjectRepository.findAllByOrderBySubjectIdDesc()).thenReturn(List.of(entity));

        assertThat(repository.getAllSubjects()).singleElement().satisfies(subject -> {
            assertThat(subject.getSubjectId()).isEqualTo(3);
            assertThat(subject.getSubjectName()).isEqualTo("Maths");
            assertThat(subject.getSubjectCode()).isEqualTo("MATH");
        });
    }

    @Test
    void getSubjectById_returnsEmpty_whenTheSubjectDoesNotExist() {
        when(subjectRepository.findById(9)).thenReturn(Optional.empty());

        assertThat(repository.getSubjectById(9)).isEmpty();
    }

    @Test
    void getSubjectById_propagatesDatabaseFailureInsteadOfReturningEmpty() {
        when(subjectRepository.findById(9))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getSubjectById(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void addSubject_rejectsABlankNameWithoutTouchingTheDatabase() {
        Subject subject = new Subject();
        subject.setSubjectName("  ");

        assertThat(repository.addSubject(subject)).isNull();
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void addSubject_propagatesDatabaseFailureInsteadOfReturningNull() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");

        when(subjectRepository.existsBySubjectCode(any())).thenReturn(false);
        when(subjectRepository.save(any(SubjectEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.addSubject(subject))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void addSubject_appendsARandomSuffix_whenTheCodeAlreadyExists() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");
        when(subjectRepository.existsBySubjectCode("MATH")).thenReturn(true);
        when(subjectRepository.save(any(SubjectEntity.class))).thenAnswer(invocation -> {
            SubjectEntity entity = invocation.getArgument(0);
            entity.setSubjectId(5);
            return entity;
        });

        Subject created = repository.addSubject(subject);

        assertThat(created.getSubjectId()).isEqualTo(5);
        assertThat(created.getSubjectCode()).matches("MATH-\\d{3}");
        ArgumentCaptor<SubjectEntity> captor = ArgumentCaptor.forClass(SubjectEntity.class);
        verify(subjectRepository).save(captor.capture());
        assertThat(captor.getValue().getSubjectCode()).isEqualTo(created.getSubjectCode());
    }

    @Test
    void updateSubject_returnsFalse_whenTheSubjectDoesNotExist() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");
        when(subjectRepository.findById(9)).thenReturn(Optional.empty());

        assertThat(repository.updateSubject(9, subject)).isFalse();
    }

    @Test
    void updateSubject_propagatesDatabaseFailureInsteadOfReturningFalse() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");
        when(subjectRepository.findById(9))
                .thenThrow(new DataAccessResourceFailureException("read only"));

        assertThatThrownBy(() -> repository.updateSubject(9, subject))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getQuestionCountBySubjectId_propagatesDatabaseFailureInsteadOfReturningZero() {
        when(subjectRepository.countQuestionsBySubjectId(9))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getQuestionCountBySubjectId(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void getExamCountBySubjectId_propagatesDatabaseFailureInsteadOfReturningZero() {
        when(subjectRepository.countExamsBySubjectId(9))
                .thenThrow(new DataAccessResourceFailureException("connection reset"));

        assertThatThrownBy(() -> repository.getExamCountBySubjectId(9))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void deleteSubjectById_refusesWhenAnExamHasRecordedAttempts() {
        when(subjectRepository.findExamIdsBySubjectId(9)).thenReturn(List.of(4));
        when(subjectRepository.countAttemptsByExamId(4)).thenReturn(2);

        assertThatThrownBy(() -> repository.deleteSubjectById(9))
                .hasMessageContaining("cannot be deleted");
        verify(subjectRepository, never()).deleteExamQuestionsByExamId(anyInt());
        verify(subjectRepository, never()).deleteSubjectBySubjectId(anyInt());
    }

    @Test
    void addSubject_insertsAndReturnsTheGeneratedId() {
        Subject subject = new Subject();
        subject.setSubjectName("Maths");
        subject.setSubjectCode("MATH");
        when(subjectRepository.existsBySubjectCode("MATH")).thenReturn(false);
        when(subjectRepository.save(any(SubjectEntity.class))).thenAnswer(invocation -> {
            SubjectEntity entity = invocation.getArgument(0);
            entity.setSubjectId(12);
            return entity;
        });

        Subject created = repository.addSubject(subject);

        assertThat(created.getSubjectId()).isEqualTo(12);
        assertThat(created.getSubjectCode()).isEqualTo("MATH");
        assertThat(created.getSubjectName()).isEqualTo("Maths");
    }
}
