package com.learnhub.github.controller;

import com.learnhub.auth.oauth.exception.GitHubRateLimitException;
import com.learnhub.github.dto.GitHubRepositoryDto;
import com.learnhub.github.dto.GitHubRepositoryResponse;
import com.learnhub.github.dto.PaginationDto;
import com.learnhub.github.dto.RateLimitDto;
import com.learnhub.github.service.GitHubRepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubRepositoryController.class)
class GitHubRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubRepositoryService gitHubRepositoryService;

    @Test
    void testListRepositories_WithValidParams_Returns200() throws Exception {
        // Arrange
        var repo = new GitHubRepositoryDto(
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

        var pagination = new PaginationDto(1, 1, 30, false, null);
        var rateLimit = new RateLimitDto(4999, 5000, "2026-06-18T15:00:00Z");
        var response = new GitHubRepositoryResponse(List.of(repo), pagination, rateLimit);

        when(gitHubRepositoryService.listRepositories(
            anyString(),
            eq(1),
            eq(30),
            eq(false)
        )).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/github/repositories")
                .header("X-GitHub-Token", "test-token")
                .param("page", "1")
                .param("per_page", "30"))
            .andExpect(status().isOk());
    }

    @Test
    void testListRepositories_WithoutToken_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/github/repositories")
                .param("page", "1")
                .param("per_page", "30"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testListRepositories_WithInvalidPerPage_Returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/github/repositories")
                .header("X-GitHub-Token", "test-token")
                .param("page", "1")
                .param("per_page", "150"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testListRepositories_WithRateLimitExceeded_Returns429() throws Exception {
        // Arrange
        when(gitHubRepositoryService.listRepositories(
            anyString(),
            anyInt(),
            anyInt(),
            anyBoolean()
        )).thenThrow(new GitHubRateLimitException("Rate limit exceeded", 12345L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/github/repositories")
                .header("X-GitHub-Token", "test-token")
                .param("page", "1")
                .param("per_page", "30"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void testListRepositories_WithInvalidPage_Returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/github/repositories")
                .header("X-GitHub-Token", "test-token")
                .param("page", "0")
                .param("per_page", "30"))
            .andExpect(status().isBadRequest());
    }
}
