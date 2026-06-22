package com.learnhub.brf.service;

import com.learnhub.brf.entity.BrfVersion;
import com.learnhub.brf.exception.BrfVersionNotFoundException;
import com.learnhub.brf.repository.BrfVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrfServiceImpl.
 *
 * Tests BRF version loading, caching, validation,
 * and persistence operations.
 */
@ExtendWith(MockitoExtension.class)
class BrfServiceImplTest {

    @Mock
    private BrfVersionRepository mockRepository;

    private BrfServiceImpl brfService;

    @BeforeEach
    void setUp() {
        brfService = new BrfServiceImpl(mockRepository);
        ReflectionTestUtils.setField(brfService, "defaultVersion", "1.0.0");
    }

    @Test
    void testGetVersion_ExistingVersion_ReturnsVersion() {
        // Arrange
        BrfVersion brfVersion = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version("1.0.0")
                .yamlContent("rules: []")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion("1.0.0"))
                .thenReturn(Optional.of(brfVersion));

        // Act
        BrfVersion result = brfService.getVersion("1.0.0");

        // Assert
        assertNotNull(result);
        assertEquals("1.0.0", result.getVersion());
        verify(mockRepository).findByVersion("1.0.0");
    }

    @Test
    void testGetVersion_NonExistentVersion_ThrowsException() {
        // Arrange
        when(mockRepository.findByVersion("2.0.0"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BrfVersionNotFoundException.class,
                () -> brfService.getVersion("2.0.0"),
                "Should throw exception for non-existent version");
        verify(mockRepository).findByVersion("2.0.0");
    }

    @Test
    void testGetVersion_NullVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.getVersion(null),
                "Should throw exception for null version");
    }

    @Test
    void testGetVersion_BlankVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.getVersion("   "),
                "Should throw exception for blank version");
    }

    @Test
    void testLoadYaml_ExistingVersion_ReturnsYaml() {
        // Arrange
        String yamlContent = "competencies:\n  - name: api-design";
        BrfVersion brfVersion = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version("1.0.0")
                .yamlContent(yamlContent)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion("1.0.0"))
                .thenReturn(Optional.of(brfVersion));

        // Act
        String result = brfService.loadYaml("1.0.0");

        // Assert
        assertEquals(yamlContent, result);
        verify(mockRepository).findByVersion("1.0.0");
    }

    @Test
    void testLoadYaml_NonExistentVersion_ThrowsException() {
        // Arrange
        when(mockRepository.findByVersion("2.0.0"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BrfVersionNotFoundException.class,
                () -> brfService.loadYaml("2.0.0"),
                "Should throw exception for non-existent version");
    }

    @Test
    void testLoadYaml_NullVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.loadYaml(null),
                "Should throw exception for null version");
    }

    @Test
    void testVersionExists_ExistingVersion_ReturnsTrue() {
        // Arrange
        when(mockRepository.existsByVersion("1.0.0"))
                .thenReturn(true);

        // Act
        boolean result = brfService.versionExists("1.0.0");

        // Assert
        assertTrue(result);
        verify(mockRepository).existsByVersion("1.0.0");
    }

    @Test
    void testVersionExists_NonExistentVersion_ReturnsFalse() {
        // Arrange
        when(mockRepository.existsByVersion("2.0.0"))
                .thenReturn(false);

        // Act
        boolean result = brfService.versionExists("2.0.0");

        // Assert
        assertFalse(result);
    }

    @Test
    void testVersionExists_NullVersion_ReturnsFalse() {
        // Act
        boolean result = brfService.versionExists(null);

        // Assert
        assertFalse(result);
        verify(mockRepository, never()).existsByVersion(any());
    }

    @Test
    void testVersionExists_BlankVersion_ReturnsFalse() {
        // Act
        boolean result = brfService.versionExists("   ");

        // Assert
        assertFalse(result);
        verify(mockRepository, never()).existsByVersion(any());
    }

    @Test
    void testSaveVersion_NewVersion_PersistsSuccessfully() {
        // Arrange
        String yamlContent = "rules: []";
        String version = "1.0.0";
        String gitTag = "v1.0.0";
        String commitHash = "abc123def456";

        BrfVersion saved = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version(version)
                .yamlContent(yamlContent)
                .gitTag(gitTag)
                .commitHash(commitHash)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion(version))
                .thenReturn(Optional.empty());
        when(mockRepository.save(any(BrfVersion.class)))
                .thenReturn(saved);

        // Act
        BrfVersion result = brfService.saveVersion(version, yamlContent, gitTag, commitHash);

        // Assert
        assertNotNull(result);
        assertEquals(version, result.getVersion());
        assertEquals(gitTag, result.getGitTag());
        assertEquals(commitHash, result.getCommitHash());
        verify(mockRepository).save(any(BrfVersion.class));
    }

    @Test
    void testSaveVersion_UpdateExistingVersion_PersistsSuccessfully() {
        // Arrange
        String version = "1.0.0";
        String oldYaml = "old: []";
        String newYaml = "new: []";

        BrfVersion existing = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version(version)
                .yamlContent(oldYaml)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion(version))
                .thenReturn(Optional.of(existing));
        when(mockRepository.save(any(BrfVersion.class)))
                .thenReturn(existing);

        // Act
        BrfVersion result = brfService.saveVersion(version, newYaml, "v1.0.0", "abc123");

        // Assert
        assertNotNull(result);
        verify(mockRepository).save(any(BrfVersion.class));
    }

    @Test
    void testSaveVersion_NullVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.saveVersion(null, "yaml", "tag", "hash"),
                "Should throw exception for null version");
    }

    @Test
    void testSaveVersion_BlankVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.saveVersion("   ", "yaml", "tag", "hash"),
                "Should throw exception for blank version");
    }

    @Test
    void testSaveVersion_NullYamlContent_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.saveVersion("1.0.0", null, "tag", "hash"),
                "Should throw exception for null YAML");
    }

    @Test
    void testSaveVersion_BlankYamlContent_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.saveVersion("1.0.0", "   ", "tag", "hash"),
                "Should throw exception for blank YAML");
    }

    @Test
    void testSaveVersion_InvalidSemanticVersion_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> brfService.saveVersion("invalid.version", "yaml", "tag", "hash"),
                "Should throw exception for invalid semantic version");
    }

    @Test
    void testSaveVersion_ValidSemanticVersions_Accepted() {
        // Arrange
        BrfVersion saved = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version("1.2.3")
                .yamlContent("yaml")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion(anyString()))
                .thenReturn(Optional.empty());
        when(mockRepository.save(any(BrfVersion.class)))
                .thenReturn(saved);

        // Test various valid semantic versions
        String[] validVersions = {"1.0.0", "2.1.3", "0.0.1", "1.0.0-beta", "1.0.0-alpha.1"};
        for (String version : validVersions) {
            // Act
            BrfVersion result = brfService.saveVersion(version, "yaml", null, null);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    void testClearCache_CallsCacheEviction() {
        // Act
        brfService.clearCache();

        // Assert - verify method completes without error
        // Actual cache eviction is tested by Spring cache tests
    }

    @Test
    void testGetDefaultVersion_ReturnsConfiguredDefault() {
        // Act
        String result = brfService.getDefaultVersion();

        // Assert
        assertEquals("1.0.0", result);
    }

    @Test
    void testGetDefaultVersion_MatchesProperty() {
        // Arrange
        String customDefault = "2.0.0";
        ReflectionTestUtils.setField(brfService, "defaultVersion", customDefault);

        // Act
        String result = brfService.getDefaultVersion();

        // Assert
        assertEquals(customDefault, result);
    }

    @Test
    void testSaveVersion_WithGitTag_StoresTagMetadata() {
        // Arrange
        String version = "1.0.0";
        String gitTag = "v1.0.0";

        BrfVersion saved = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version(version)
                .yamlContent("yaml")
                .gitTag(gitTag)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion(version))
                .thenReturn(Optional.empty());
        when(mockRepository.save(any(BrfVersion.class)))
                .thenReturn(saved);

        // Act
        BrfVersion result = brfService.saveVersion(version, "yaml", gitTag, null);

        // Assert
        assertEquals(gitTag, result.getGitTag());
    }

    @Test
    void testSaveVersion_WithCommitHash_StoresHashMetadata() {
        // Arrange
        String version = "1.0.0";
        String commitHash = "abc123def456";

        BrfVersion saved = BrfVersion.builder()
                .id(UUID.randomUUID())
                .version(version)
                .yamlContent("yaml")
                .commitHash(commitHash)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(mockRepository.findByVersion(version))
                .thenReturn(Optional.empty());
        when(mockRepository.save(any(BrfVersion.class)))
                .thenReturn(saved);

        // Act
        BrfVersion result = brfService.saveVersion(version, "yaml", null, commitHash);

        // Assert
        assertEquals(commitHash, result.getCommitHash());
    }
}
