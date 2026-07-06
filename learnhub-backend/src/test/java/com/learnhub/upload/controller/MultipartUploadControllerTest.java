package com.learnhub.upload.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.upload.dto.request.CompleteMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompletedUploadPartRequest;
import com.learnhub.upload.dto.request.PresignMultipartUploadRequest;
import com.learnhub.upload.dto.request.StartMultipartUploadRequest;
import com.learnhub.upload.dto.response.CompleteMultipartUploadResponse;
import com.learnhub.upload.dto.response.MultipartUploadPresignResponse;
import com.learnhub.upload.dto.response.MultipartUploadSessionResponse;
import com.learnhub.upload.dto.response.PresignedPartResponse;
import com.learnhub.upload.dto.response.UploadedPartResponse;
import com.learnhub.upload.service.MultipartUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultipartUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
class MultipartUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MultipartUploadService multipartUploadService;

    private UsernamePasswordAuthenticationToken buildAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            UUID.randomUUID().toString(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
    }

    @Test
    void startUpload_returnsSessionPayload() throws Exception {
        MultipartUploadSessionResponse response = new MultipartUploadSessionResponse(
            UUID.randomUUID(),
            "upload-123",
            "videos/demo.mp4",
            "avatars",
            "COURSE_VIDEO",
            "demo.mp4",
            "video/mp4",
            1024L,
            10L * 1024 * 1024,
            5,
            3,
            LocalDateTime.now().plusHours(1),
            List.of()
        );

        when(multipartUploadService.startUpload(any(StartMultipartUploadRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/uploads/multipart/start")
                .with(authentication(buildAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartMultipartUploadRequest(
                    "demo.mp4",
                    "video/mp4",
                    1024L,
                    "COURSE_VIDEO",
                    UUID.randomUUID(),
                    UUID.randomUUID()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uploadId").value("upload-123"))
            .andExpect(jsonPath("$.objectKey").value("videos/demo.mp4"));
    }

    @Test
    void presign_returnsPartUrls() throws Exception {
        MultipartUploadPresignResponse response = new MultipartUploadPresignResponse(
            "upload-123",
            "videos/demo.mp4",
            List.of(new PresignedPartResponse(1, "http://minio/presigned"))
        );

        when(multipartUploadService.presignParts(any(PresignMultipartUploadRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/uploads/multipart/presign")
                .with(authentication(buildAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PresignMultipartUploadRequest(
                    "upload-123",
                    "videos/demo.mp4",
                    List.of(1)
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parts[0].url").value("http://minio/presigned"));
    }

    @Test
    void complete_returnsStoredAssetPayload() throws Exception {
        CompleteMultipartUploadResponse response = new CompleteMultipartUploadResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "upload-123",
            "videos/demo.mp4",
            "avatars",
            "http://192.168.184.128:9000/avatars/videos/demo.mp4",
            "video/mp4",
            1024L,
            60,
            LocalDateTime.now()
        );

        when(multipartUploadService.completeUpload(any(CompleteMultipartUploadRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/uploads/multipart/complete")
                .with(authentication(buildAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CompleteMultipartUploadRequest(
                    "upload-123",
                    "videos/demo.mp4",
                    List.of(new CompletedUploadPartRequest(1, "\"etag\"")),
                    60
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicUrl").exists())
            .andExpect(jsonPath("$.durationSeconds").value(60));
    }

    @Test
    void abort_returnsOk() throws Exception {
        doNothing().when(multipartUploadService).abortUpload(any(), any());

        mockMvc.perform(post("/api/v1/uploads/multipart/abort")
                .with(authentication(buildAuthentication()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "uploadId": "upload-123",
                      "objectKey": "videos/demo.mp4"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void status_returnsCurrentUploadedParts() throws Exception {
        MultipartUploadSessionResponse response = new MultipartUploadSessionResponse(
            UUID.randomUUID(),
            "upload-123",
            "videos/demo.mp4",
            "avatars",
            "COURSE_VIDEO",
            "demo.mp4",
            "video/mp4",
            1024L,
            10L * 1024 * 1024,
            5,
            3,
            LocalDateTime.now().plusHours(1),
            List.of(new UploadedPartResponse(1, "\"etag\"", 1024L))
        );

        when(multipartUploadService.getStatus("upload-123", "videos/demo.mp4", any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/uploads/multipart/status")
                .with(authentication(buildAuthentication()))
                .param("uploadId", "upload-123")
                .param("objectKey", "videos/demo.mp4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uploadedParts[0].partNumber").value(1));
    }
}
