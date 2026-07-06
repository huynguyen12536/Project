ALTER TABLE uploaded_media_objects
    ADD COLUMN IF NOT EXISTS checksum_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS checksum_algorithm VARCHAR(32),
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS thumbnail_url TEXT,
    ADD COLUMN IF NOT EXISTS thumbnail_object_key TEXT,
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS metadata_error TEXT;

CREATE INDEX IF NOT EXISTS idx_uploaded_media_lecture_asset
    ON uploaded_media_objects (lecture_id, asset_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_uploaded_media_processing_status
    ON uploaded_media_objects (processing_status, created_at DESC);
