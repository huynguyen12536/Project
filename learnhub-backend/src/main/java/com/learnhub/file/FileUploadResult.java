
package com.learnhub.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {
    private String url;
    private String checksum;
    private String checksumAlgorithm;
    private Long fileSize;
    private String mimeType;
}

