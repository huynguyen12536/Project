-- V16: Core course tables including courses, sections, lectures, pricing, coupons, reviews, and quizzes.
--
-- Purpose: Core tables for course creation flow, curriculum building, pricing setup, and admin review.
--

-- ============================================================================
-- TABLE: courses
-- ============================================================================

CREATE TABLE IF NOT EXISTS courses (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    instructor_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title                   VARCHAR(255) NOT NULL,
    subtitle                VARCHAR(500),
    description             TEXT,
    thumbnail_url           TEXT,
    promo_video_url         TEXT,
    category_id             UUID REFERENCES course_categories(id),
    subcategory_id          UUID REFERENCES course_subcategories(id),
    level_id             UUID REFERENCES course_levels(id),
    language_id             UUID REFERENCES course_languages(id),
    status                  VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED
    total_video_duration_seconds INTEGER NOT NULL DEFAULT 0,
    lecture_count           INTEGER NOT NULL DEFAULT 0,
    student_count           INTEGER NOT NULL DEFAULT 0,
    average_rating         NUMERIC(3,2),
    rejection_reason      TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_courses_instructor_id ON courses(instructor_id);
CREATE INDEX IF NOT EXISTS idx_courses_status ON courses(status);
CREATE INDEX IF NOT EXISTS idx_courses_category ON courses(category_id);

-- ============================================================================
-- TABLE: course_sections
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_sections (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id             UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title                   VARCHAR(255) NOT NULL,
    description            INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_sections_course ON course_sections(course_id);
-- ============================================================================
-- TABLE: course_lectures
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_lectures (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id            UUID NOT NULL REFERENCES course_sections(id) ON DELETE CASCADE,
    course_id             UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title                   VARCHAR(255) NOT NULL,
    type                    VARCHAR(50) NOT NULL DEFAULT 'VIDEO', -- VIDEO, ARTICLE, QUIZ
    content               TEXT,
    video_url             TEXT,
    duration_seconds         INTEGER,
    description           INTEGER NOT NULL DEFAULT 0,
    is_free_preview      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_course_lectures_section ON course_lectures(section_id);
CREATE INDEX IF NOT EXISTS idx_course_lectures_course ON course_lectures(course_id);

-- ============================================================================
-- TABLE: course_tags
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_tags (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES course_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, tag_id)
);
-- ============================================================================
-- TABLE: course_pricing
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_pricing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    price_vnd NUMERIC(12,0) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_pricing_course ON course_pricing(course_id);
-- ============================================================================
-- TABLE: course_coupons
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_coupons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED_AMOUNT
    discount_value NUMERIC(12,0) NOT NULL,
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_coupons_course ON course_coupons(course_id);
CREATE INDEX IF NOT EXISTS idx_course_coupons_code ON course_coupons(code);
-- ============================================================================
-- TABLE: course_enrollments
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    price_paid NUMERIC(12,0),
    coupon_used UUID REFERENCES course_coupons(id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_course_enrollments_unique ON course_enrollments(course_id, user_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_user ON course_enrollments(user_id);
-- ============================================================================
-- TABLE: course_reviews
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <=5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_reviews_course ON course_reviews(course_id);
CREATE INDEX IF NOT EXISTS idx_course_reviews_reviewer ON course_reviews(reviewer_id);
-- ============================================================================
-- TABLE: course_quizzes
-- ============================================================================
CREATE TABLE IF NOT EXISTS course_quizzes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lecture_id UUID NOT NULL REFERENCES course_lectures(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    pass_percent_required INTEGER NOT NULL DEFAULT 80, -- 0-100
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_quizzes_lecture ON course_quizzes(lecture_id);
-- ============================================================================
-- TABLE: quiz_questions
-- ============================================================================
CREATE TABLE IF NOT EXISTS quiz_questions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id UUID NOT NULL REFERENCES course_quizzes(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL DEFAULT 'MULTIPLE_CHOICE', -- MULTIPLE_CHOICE, TRUE_FALSE
    explanation TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_quiz ON quiz_questions(quiz_id);
-- ============================================================================
-- TABLE: quiz_answers
-- ============================================================================
CREATE TABLE IF NOT EXISTS quiz_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_quiz_answers_question ON quiz_answers(question_id);
-- ============================================================================
-- TABLE: learning_progress
-- ============================================================================
CREATE TABLE IF NOT EXISTS learning_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lecture_id UUID NOT NULL REFERENCES course_lectures(id) ON DELETE CASCADE,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INTEGER NOT NULL DEFAULT 0,
    last_watched_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, lecture_id)
);
CREATE INDEX IF NOT EXISTS idx_learning_progress_user_course ON learning_progress(user_id, course_id);
CREATE INDEX IF NOT EXISTS idx_learning_progress_lecture ON learning_progress(lecture_id);
