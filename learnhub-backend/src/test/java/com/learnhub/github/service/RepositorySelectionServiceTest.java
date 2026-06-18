package com.learnhub.github.service;

import com.learnhub.github.entity.UserGithubSelection;
import com.learnhub.github.exception.RepositoryAlreadySelectedException;
import com.learnhub.github.exception.RepositoryNotSelectedException;
import com.learnhub.github.repository.UserGithubSelectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositorySelectionServiceTest {

    @Mock
    private UserGithubSelectionRepository repository;

    @InjectMocks
    private RepositorySelectionService service;

    private UUID testUserId;
    private Long testGithubRepoId;
    private UserGithubSelection testSelection;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testGithubRepoId = 123456789L;
        testSelection = UserGithubSelection.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .githubRepoId(testGithubRepoId)
            .selectedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void selectRepository_Success() {
        when(repository.existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId)).thenReturn(false);
        when(repository.save(any(UserGithubSelection.class))).thenReturn(testSelection);

        UserGithubSelection result = service.selectRepository(testUserId, testGithubRepoId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testGithubRepoId, result.getGithubRepoId());
        verify(repository, times(1)).existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
        verify(repository, times(1)).save(any(UserGithubSelection.class));
    }

    @Test
    void selectRepository_DuplicateSelection_ThrowsException() {
        when(repository.existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId)).thenReturn(true);

        assertThrows(RepositoryAlreadySelectedException.class,
            () -> service.selectRepository(testUserId, testGithubRepoId));

        verify(repository, times(1)).existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
        verify(repository, never()).save(any(UserGithubSelection.class));
    }

    @Test
    void selectRepository_NullUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.selectRepository(null, testGithubRepoId));

        verify(repository, never()).save(any(UserGithubSelection.class));
    }

    @Test
    void selectRepository_InvalidRepoId_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.selectRepository(testUserId, 0L));

        assertThrows(IllegalArgumentException.class,
            () -> service.selectRepository(testUserId, -1L));

        assertThrows(IllegalArgumentException.class,
            () -> service.selectRepository(testUserId, null));

        verify(repository, never()).save(any(UserGithubSelection.class));
    }

    @Test
    void isRepositorySelected_WhenSelected_ReturnsTrue() {
        when(repository.existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId)).thenReturn(true);

        boolean result = service.isRepositorySelected(testUserId, testGithubRepoId);

        assertTrue(result);
        verify(repository, times(1)).existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
    }

    @Test
    void isRepositorySelected_WhenNotSelected_ReturnsFalse() {
        when(repository.existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId)).thenReturn(false);

        boolean result = service.isRepositorySelected(testUserId, testGithubRepoId);

        assertFalse(result);
        verify(repository, times(1)).existsByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
    }

    @Test
    void findSelectedRepository_ReturnsSelection() {
        when(repository.findByUserIdAndGithubRepoId(testUserId, testGithubRepoId))
            .thenReturn(Optional.of(testSelection));

        UserGithubSelection result = service.findSelectedRepository(testUserId, testGithubRepoId);

        assertNotNull(result);
        assertEquals(testSelection.getId(), result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testGithubRepoId, result.getGithubRepoId());
        verify(repository, times(1)).findByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
    }

    @Test
    void findSelectedRepository_NotFound_ThrowsException() {
        when(repository.findByUserIdAndGithubRepoId(testUserId, testGithubRepoId))
            .thenReturn(Optional.empty());

        assertThrows(RepositoryNotSelectedException.class,
            () -> service.findSelectedRepository(testUserId, testGithubRepoId));

        verify(repository, times(1)).findByUserIdAndGithubRepoId(testUserId, testGithubRepoId);
    }

    @Test
    void findUserSelections_ReturnsListOfSelections() {
        UserGithubSelection selection2 = UserGithubSelection.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .githubRepoId(987654321L)
            .selectedAt(LocalDateTime.now())
            .build();

        List<UserGithubSelection> expectedSelections = List.of(testSelection, selection2);

        when(repository.findByUserId(testUserId)).thenReturn(expectedSelections);

        List<UserGithubSelection> result = service.findUserSelections(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testSelection.getId(), result.get(0).getId());
        assertEquals(selection2.getId(), result.get(1).getId());
        verify(repository, times(1)).findByUserId(testUserId);
    }

    @Test
    void findUserSelections_EmptyList() {
        when(repository.findByUserId(testUserId)).thenReturn(List.of());

        List<UserGithubSelection> result = service.findUserSelections(testUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByUserId(testUserId);
    }
}
