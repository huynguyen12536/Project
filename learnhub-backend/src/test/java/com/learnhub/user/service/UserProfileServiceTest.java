package com.learnhub.user.service;

import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.user.dto.request.UpdateProfileRequest;
import com.learnhub.user.dto.response.UserProfileResponse;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .role("LEARNER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetProfile_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userProfileService.getProfile(testUserId);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getFirstName(), response.getFirstName());
        assertEquals(testUser.getLastName(), response.getLastName());
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testGetProfile_UserNotFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getProfile(testUserId));
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setBio("Test bio");
        request.setPhone("+1234567890");
        request.setLocation("Test City");
        request.setGithubProfileUrl("https://github.com/testuser");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(testUserId, request);

        assertNotNull(response);
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("Test bio", response.getBio());
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.updateProfile(testUserId, request));
    }

    @Test
    void testUpdateLastLogin_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userProfileService.updateLastLogin(testUserId);

        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testIsEmailVerified_True() {
        testUser.setEmailVerified(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        boolean result = userProfileService.isEmailVerified(testUserId);

        assertTrue(result);
    }

    @Test
    void testIsEmailVerified_False() {
        testUser.setEmailVerified(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        boolean result = userProfileService.isEmailVerified(testUserId);

        assertFalse(result);
    }

    @Test
    void testIsEmailVerified_UserNotFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        boolean result = userProfileService.isEmailVerified(testUserId);

        assertFalse(result);
    }
}
