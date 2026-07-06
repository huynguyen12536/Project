ALTER TABLE multipart_upload_sessions
    ADD COLUMN chunk_size_bytes BIGINT NOT NULL DEFAULT 67108864,
    ADD COLUMN max_concurrency INTEGER NOT NULL DEFAULT 6;
