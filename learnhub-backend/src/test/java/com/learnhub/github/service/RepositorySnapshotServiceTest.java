package com.learnhub.github.service;

import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.exception.RepositoryNotSelectedException;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositorySnapshotServiceTest {

    @Mock
    private RepositorySnapshotRepository repository;

    @Mock
    private RepositorySelectionService selectionService;

    @InjectMocks
    private RepositorySnapshotService service;

    private UUID testUserId;
    private Long testGithubRepoId;
    private UUID testSnapshotId;
    private RepositorySnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testGithubRepoId = 123456789L;
        testSnapshotId = UUID.randomUUID();

        testSnapshot = RepositorySnapshot.builder()
            .id(testSnapshotId)
            .userId(testUserId)
            .githubRepoId(testGithubRepoId)
            .branch("main")
            .commitSha("abc123def456")
            .filesCount(150)
            .totalSizeKb(5000L)
            .languages(Map.of("Java", 50, "Python", 30, "SQL", 20))
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void createSnapshot_Success() {
        Map<String, Integer> languages = Map.of("Java", 50, "Python", 30);

        doNothing().when(selectionService).findSelectedRepository(testUserId, testGithubRepoId);
        when(repository.save(any(RepositorySnapshot.class))).thenReturn(testSnapshot);

        RepositorySnapshot result = service.createSnapshot(
            testUserId,
            testGithubRepoId,
            "main",
            "abc123def456",
            150,
            5000L,
            languages
        );

        assertNotNull(result);
        assertEquals(testSnapshotId, result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testGithubRepoId, result.getGithubRepoId());
        assertEquals("main", result.getBranch());
        assertEquals(150, result.getFilesCount());
        assertEquals(5000L, result.getTotalSizeKb());
        verify(selectionService, times(1)).findSelectedRepository(testUserId, testGithubRepoId);
        verify(repository, times(1)).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_DefaultsBranchToMain() {
        doNothing().when(selectionService).findSelectedRepository(testUserId, testGithubRepoId);

        RepositorySnapshot snapshotWithNullBranch = testSnapshot.toBuilder().branch("main").build();
        when(repository.save(any(RepositorySnapshot.class))).thenReturn(snapshotWithNullBranch);

        RepositorySnapshot result = service.createSnapshot(
            testUserId,
            testGithubRepoId,
            null, // branch is null
            "abc123def456",
            150,
            5000L,
            null
        );

        assertNotNull(result);
        assertEquals("main", result.getBranch());
        verify(repository, times(1)).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_NullUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(null, testGithubRepoId, "main", "abc123", 100, 1000L, null));

        verify(repository, never()).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_InvalidGithubRepoId_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, 0L, "main", "abc123", 100, 1000L, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, -1L, "main", "abc123", 100, 1000L, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, null, "main", "abc123", 100, 1000L, null));

        verify(repository, never()).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_NegativeFilesCount_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, testGithubRepoId, "main", "abc123", -1, 1000L, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, testGithubRepoId, "main", "abc123", null, 1000L, null));

        verify(repository, never()).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_NegativeTotalSize_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, testGithubRepoId, "main", "abc123", 100, -1L, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.createSnapshot(testUserId, testGithubRepoId, "main", "abc123", 100, null, null));

        verify(repository, never()).save(any(RepositorySnapshot.class));
    }

    @Test
    void createSnapshot_RepositoryNotSelected_ThrowsException() {
        doThrow(new RepositoryNotSelectedException("Repository not selected"))
            .when(selectionService).findSelectedRepository(testUserId, testGithubRepoId);

        assertThrows(RepositoryNotSelectedException.class,
            () -> service.createSnapshot(testUserId, testGithubRepoId, "main", "abc123", 100, 1000L, null));

        verify(repository, never()).save(any(RepositorySnapshot.class));
    }

    @Test
    void getSnapshot_Success() {
        when(repository.findByIdAndUserId(testSnapshotId, testUserId))
            .thenReturn(Optional.of(testSnapshot));

        RepositorySnapshot result = service.getSnapshot(testSnapshotId, testUserId);

        assertNotNull(result);
        assertEquals(testSnapshotId, result.getId());
        assertEquals(testUserId, result.getUserId());
        verify(repository, times(1)).findByIdAndUserId(testSnapshotId, testUserId);
    }

    @Test
    void getSnapshot_NotFound_ThrowsException() {
        when(repository.findByIdAndUserId(testSnapshotId, testUserId))
            .thenReturn(Optional.empty());

        assertThrows(SnapshotNotFoundException.class,
            () -> service.getSnapshot(testSnapshotId, testUserId));

        verify(repository, times(1)).findByIdAndUserId(testSnapshotId, testUserId);
    }

    @Test
    void getSnapshot_NotOwnedByUser_ThrowsException() {
        UUID differentUserId = UUID.randomUUID();

        when(repository.findByIdAndUserId(testSnapshotId, differentUserId))
            .thenReturn(Optional.empty());

        assertThrows(SnapshotNotFoundException.class,
            () -> service.getSnapshot(testSnapshotId, differentUserId));

        verify(repository, times(1)).findByIdAndUserId(testSnapshotId, differentUserId);
    }

    @Test
    void getSnapshot_NullIds_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getSnapshot(null, testUserId));

        assertThrows(IllegalArgumentException.class,
            () -> service.getSnapshot(testSnapshotId, null));

        assertThrows(IllegalArgumentException.class,
            () -> service.getSnapshot(null, null));

        verify(repository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void deleteSnapshot_Success() {
        when(repository.findByIdAndUserId(testSnapshotId, testUserId))
            .thenReturn(Optional.of(testSnapshot));
        doNothing().when(repository).delete(testSnapshot);

        service.deleteSnapshot(testSnapshotId, testUserId);

        verify(repository, times(1)).findByIdAndUserId(testSnapshotId, testUserId);
        verify(repository, times(1)).delete(testSnapshot);
    }

    @Test
    void deleteSnapshot_NotOwnedByUser_ThrowsException() {
        UUID differentUserId = UUID.randomUUID();

        when(repository.findByIdAndUserId(testSnapshotId, differentUserId))
            .thenReturn(Optional.empty());

        assertThrows(SnapshotNotFoundException.class,
            () -> service.deleteSnapshot(testSnapshotId, differentUserId));

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteSnapshot_NullIds_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.deleteSnapshot(null, testUserId));

        assertThrows(IllegalArgumentException.class,
            () -> service.deleteSnapshot(testSnapshotId, null));

        verify(repository, never()).delete(any());
    }

    @Test
    void findUserSnapshots_ReturnsListOrderedByDateDescending() {
        RepositorySnapshot snapshot2 = testSnapshot.toBuilder()
            .id(UUID.randomUUID())
            .createdAt(LocalDateTime.now().minusHours(1))
            .build();

        List<RepositorySnapshot> expectedSnapshots = List.of(testSnapshot, snapshot2);

        when(repository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(expectedSnapshots);

        List<RepositorySnapshot> result = service.findUserSnapshots(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testSnapshotId, result.get(0).getId());
        verify(repository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }

    @Test
    void findUserSnapshots_EmptyList() {
        when(repository.findByUserIdOrderByCreatedAtDesc(testUserId))
            .thenReturn(List.of());

        List<RepositorySnapshot> result = service.findUserSnapshots(testUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByUserIdOrderByCreatedAtDesc(testUserId);
    }
}
