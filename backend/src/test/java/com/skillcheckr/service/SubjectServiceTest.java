package com.skillcheckr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillcheckr.model.Subject;
import com.skillcheckr.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    @Test
    void getAllSubjects_delegatesToRepository() {
        Subject s = new Subject(1, "Mathematics", "MATH101");
        when(subjectRepository.getAllSubjects()).thenReturn(List.of(s));

        List<Subject> result = subjectService.getAllSubjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjectName()).isEqualTo("Mathematics");
        verify(subjectRepository).getAllSubjects();
    }

    @Test
    void addSubject_delegatesToRepository() {
        Subject s = new Subject(0, "Chemistry", "CHEM101");
        Subject saved = new Subject(2, "Chemistry", "CHEM101");
        when(subjectRepository.addSubject(s)).thenReturn(saved);

        Subject result = subjectService.addSubject(s);

        assertThat(result).isEqualTo(saved);
        verify(subjectRepository).addSubject(s);
    }

    @Test
    void deleteSubjectById_delegatesToRepository() {
        when(subjectRepository.deleteSubjectById(5)).thenReturn(true);

        boolean result = subjectService.deleteSubjectById(5);

        assertThat(result).isTrue();
        verify(subjectRepository).deleteSubjectById(5);
    }
}
