-- V17: Normalize course builder ordering columns.
--
-- Existing V16 created `description` INTEGER columns for sections/lectures even though
-- the application uses `display_order`. This migration preserves old data and moves the
-- schema to the intended column names.

ALTER TABLE course_sections
    ADD COLUMN IF NOT EXISTS display_order INTEGER;

UPDATE course_sections
SET display_order = COALESCE(display_order, description, 0)
WHERE display_order IS NULL;

ALTER TABLE course_sections
    ALTER COLUMN display_order SET DEFAULT 0;

ALTER TABLE course_sections
    ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE course_lectures
    ADD COLUMN IF NOT EXISTS display_order INTEGER;

UPDATE course_lectures
SET display_order = COALESCE(display_order, description, 0)
WHERE display_order IS NULL;

ALTER TABLE course_lectures
    ALTER COLUMN display_order SET DEFAULT 0;

ALTER TABLE course_lectures
    ALTER COLUMN display_order SET NOT NULL;
