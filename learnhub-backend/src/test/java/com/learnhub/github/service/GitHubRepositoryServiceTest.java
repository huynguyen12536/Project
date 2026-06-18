package com.learnhub.github.service;

import com.learnhub.auth.oauth.GitHubApiClient;
import com.learnhub.auth.oauth.exception.GitHubAPIException;
import com.learnhub.auth.oauth.exception.GitHubRateLimitException;
import com.learnhub.github.dto.GitHubRepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubRepositoryServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private GitHubRepositoryService gitHubRepositoryService;

    private String testAccessToken = "test-access-token-123";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(gitHubApiClient, redisTemplate);
    }

    @Test
    void testListRepositories_WithValidToken_ReturnsRepositories() {
        // Arrange
        var mockRepo = new GitHubApiClient.GitHubRepository(
            123L,
            "test-repo",
            "https://github.com/user/test-repo",
            "Test repository",
            "Java",
            "2026-06-18T14:30:00Z",
            2048,
            false,
            false
        );

        var mockResponse = new GitHubApiClient.GitHubRepositoriesResponse(
            List.of(mockRepo),
            4999,
            5000,
            System.currentTimeMillis() / 1000 + 3600,
            false
        );

        when(gitHubApiClient.listRepositories(testAccessToken, 1, 30))
            .thenReturn(mockResponse);

        // Act
        GitHubRepositoryResponse response = gitHubRepositoryService.listRepositories(
            testAccessToken,
            1,
            30,
            false
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.repositories().size());
        assertEquals("test-repo", response.repositories().get(0).name());
        assertEquals(1, response.pagination().currentPage());
        assertEquals(4999, response.rateLimit().remaining());

        verify(gitHubApiClient, times(1)).listRepositories(testAccessToken, 1, 30);
    }

    @Test
    void testListRepositories_WithInvalidPerPage_DefaultsTo30() {
        // Arrange
        var mockRepo = new GitHubApiClient.GitHubRepository(
            123L, "test-repo", "https://github.com/user/test-repo",
            null, null, "2026-06-18T14:30:00Z", 2048, false, false
        );

        var mockResponse = new GitHubApiClient.GitHubRepositoriesResponse(
            List.of(mockRepo),
            4999,
            5000,
            System.currentTimeMillis() / 1000 + 3600,
            false
        );

        when(gitHubApiClient.listRepositories(testAccessToken, 1, 30))
            .thenReturn(mockResponse);

        // Act
        GitHubRepositoryResponse response = gitHubRepositoryService.listRepositories(
            testAccessToken,
            1,
            150, // Invalid, should default to 30
            false
        );

        // Assert
        assertEquals(30, response.pagination().perPage());
    }

    @Test
    void testListRepositories_WithRateLimitExceeded_ThrowsException() {
        // Arrange
        when(gitHubApiClient.listRepositories(testAccessToken, 1, 30))
            .thenThrow(new GitHubRateLimitException("Rate limit exceeded", 12345L));

        // Act & Assert
        assertThrows(GitHubRateLimitException.class, () -> {
            gitHubRepositoryService.listRepositories(testAccessToken, 1, 30, false);
        });

        verify(gitHubApiClient, times(1)).listRepositories(testAccessToken, 1, 30);
    }

    @Test
    void testListRepositories_WithGitHubAPIError_ReturnsEmptyResponse() {
        // Arrange
        when(gitHubApiClient.listRepositories(testAccessToken, 1, 30))
            .thenThrow(new GitHubAPIException("GitHub service unavailable"));

        // Act
        GitHubRepositoryResponse response = gitHubRepositoryService.listRepositories(
            testAccessToken,
            1,
            30,
            false
        );

        // Assert
        assertNotNull(response);
        assertEquals(0, response.repositories().size());
        assertEquals(0, response.rateLimit().limit());

        verify(gitHubApiClient, times(1)).listRepositories(testAccessToken, 1, 30);
    }

    @Test
    void testListRepositories_WithRefreshTrue_BypassesCache() {
        // Arrange
        var mockRepo = new GitHubApiClient.GitHubRepository(
            123L, "test-repo", "https://github.com/user/test-repo",
            null, null, "2026-06-18T14:30:00Z", 2048, false, false
        );

        var mockResponse = new GitHubApiClient.GitHubRepositoriesResponse(
            List.of(mockRepo),
            4999,
            5000,
            System.currentTimeMillis() / 1000 + 3600,
            false
        );

        when(gitHubApiClient.listRepositories(testAccessToken, 1, 30))
            .thenReturn(mockResponse);

        // Act
        GitHubRepositoryResponse response = gitHubRepositoryService.listRepositories(
            testAccessToken,
            1,
            30,
            true // Force refresh
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.repositories().size());

        verify(gitHubApiClient, times(1)).listRepositories(testAccessToken, 1, 30);
    }

    @Test
    void testInvalidateCache_DeletesCacheEntry() {
        // Act
        gitHubRepositoryService.invalidateCache(testAccessToken);

        // Assert
        verify(redisTemplate, times(1)).delete(anyString());
    }
}
