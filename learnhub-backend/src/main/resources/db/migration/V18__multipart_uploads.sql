CREATE TABLE multipart_upload_sessions (
    id UUID PRIMARY KEY,
    upload_id VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    total_size BIGINT NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    course_id UUID REFERENCES courses(id),
    lecture_id UUID REFERENCES course_lectures(id),
    public_url TEXT,
    error_message TEXT,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    aborted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_multipart_upload_identity
    ON multipart_upload_sessions (upload_id, object_key);

CREATE INDEX idx_multipart_upload_sessions_uploaded_by_status
    ON multipart_upload_sessions (uploaded_by, status, created_at DESC);

CREATE INDEX idx_multipart_upload_sessions_course_id
    ON multipart_upload_sessions (course_id);

CREATE INDEX idx_multipart_upload_sessions_lecture_id
    ON multipart_upload_sessions (lecture_id);

CREATE TABLE uploaded_media_objects (
    id UUID PRIMARY KEY,
    session_id UUID UNIQUE REFERENCES multipart_upload_sessions(id),
    object_key TEXT NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    public_url TEXT NOT NULL,
    filename VARCHAR(512) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    duration_seconds INTEGER,
    asset_type VARCHAR(50) NOT NULL,
    etag VARCHAR(255),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    course_id UUID REFERENCES courses(id),
    lecture_id UUID REFERENCES course_lectures(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_uploaded_media_bucket_object
    ON uploaded_media_objects (bucket_name, object_key);

CREATE INDEX idx_uploaded_media_uploaded_by
    ON uploaded_media_objects (uploaded_by, created_at DESC);

CREATE INDEX idx_uploaded_media_course_id
    ON uploaded_media_objects (course_id);

CREATE INDEX idx_uploaded_media_lecture_id
    ON uploaded_media_objects (lecture_id);
