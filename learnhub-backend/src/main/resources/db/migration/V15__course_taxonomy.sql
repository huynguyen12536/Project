-- V15: Course taxonomy foundation for admin-managed course creation

CREATE TABLE IF NOT EXISTS course_categories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(120) NOT NULL UNIQUE,
    slug            VARCHAR(140) NOT NULL UNIQUE,
    description     VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_subcategories (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id     UUID NOT NULL REFERENCES course_categories(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    slug            VARCHAR(140) NOT NULL UNIQUE,
    description     VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_subcategory_name_per_category UNIQUE (category_id, name)
);

CREATE TABLE IF NOT EXISTS course_levels (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(40) NOT NULL UNIQUE,
    label           VARCHAR(120) NOT NULL UNIQUE,
    description     VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_languages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(20) NOT NULL UNIQUE,
    label           VARCHAR(120) NOT NULL UNIQUE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_tags (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(120) NOT NULL UNIQUE,
    slug            VARCHAR(140) NOT NULL UNIQUE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_categories_order
    ON course_categories(display_order, name);

CREATE INDEX IF NOT EXISTS idx_course_subcategories_category_order
    ON course_subcategories(category_id, display_order, name);

CREATE INDEX IF NOT EXISTS idx_course_levels_order
    ON course_levels(display_order, label);

CREATE INDEX IF NOT EXISTS idx_course_languages_order
    ON course_languages(display_order, label);

CREATE INDEX IF NOT EXISTS idx_course_tags_order
    ON course_tags(display_order, name);

COMMENT ON TABLE course_categories IS
    'Top-level course categories curated by admins for teacher course creation.';

COMMENT ON TABLE course_subcategories IS
    'Second-level course taxonomy grouped under a top-level category.';

COMMENT ON TABLE course_levels IS
    'Difficulty/experience bands such as beginner, intermediate, advanced.';

COMMENT ON TABLE course_languages IS
    'Presentation languages available when teachers create and publish courses.';

COMMENT ON TABLE course_tags IS
    'Reusable skill and topic tags for discovery and later course tagging.';

INSERT INTO course_categories (name, slug, description, display_order)
VALUES
    ('Phat trien phan mem', 'phat-trien-phan-mem', 'Web, mobile, backend, DevOps va ky nang lap trinh thuc chien.', 10),
    ('CNTT va he thong', 'cntt-va-he-thong', 'Cloud, cybersecurity, networking, support va van hanh he thong.', 20),
    ('Du lieu va AI', 'du-lieu-va-ai', 'Data analytics, machine learning, AI applications va tu dong hoa.', 30),
    ('Kinh doanh va quan tri', 'kinh-doanh-va-quan-tri', 'Project, product, operations, leadership va entrepreneurship.', 40),
    ('Tai chinh va ke toan', 'tai-chinh-va-ke-toan', 'Bookkeeping, financial analysis, planning va business finance.', 50),
    ('Thiet ke va sang tao', 'thiet-ke-va-sang-tao', 'UX/UI, graphic, illustration, motion va 3D.', 60),
    ('Marketing va tang truong', 'marketing-va-tang-truong', 'Digital marketing, content, SEO, ads va brand building.', 70),
    ('Cong cu van phong', 'cong-cu-van-phong', 'Excel, docs, slides, collaboration va productivity tools.', 80),
    ('Phat trien ban than', 'phat-trien-ban-than', 'Communication, career growth, leadership va performance.', 90),
    ('Hinh anh, video va am thanh', 'hinh-anh-video-va-am-thanh', 'Photography, video production, editing va audio.', 100),
    ('Suc khoe va loi song', 'suc-khoe-va-loi-song', 'Wellness, fitness, mental health va healthy routines.', 110),
    ('Giang day va hoc thuat', 'giang-day-va-hoc-thuat', 'Teaching skills, instructional design, academic support va exam prep.', 120)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO course_subcategories (category_id, name, slug, description, display_order)
SELECT c.id, seed.name, seed.slug, seed.description, seed.display_order
FROM (
    VALUES
      ('phat-trien-phan-mem', 'Web Development', 'web-development', 'Frontend, backend va full-stack web applications.', 10),
      ('phat-trien-phan-mem', 'Programming Languages', 'programming-languages', 'Python, Java, JavaScript, Go va cac ngon ngu lap trinh pho bien.', 20),
      ('phat-trien-phan-mem', 'Mobile Development', 'mobile-development', 'iOS, Android, Flutter, React Native va mobile product.', 30),
      ('phat-trien-phan-mem', 'DevOps va Cloud Native', 'devops-va-cloud-native', 'CI/CD, containers, observability va deployment pipelines.', 40),

      ('cntt-va-he-thong', 'Cybersecurity', 'cybersecurity', 'Security fundamentals, SOC, IAM va secure operations.', 10),
      ('cntt-va-he-thong', 'Cloud Computing', 'cloud-computing', 'AWS, Azure, GCP va cloud administration.', 20),
      ('cntt-va-he-thong', 'Networking', 'networking', 'System, network, infrastructure va support operations.', 30),
      ('cntt-va-he-thong', 'IT Support', 'it-support', 'Help desk, troubleshooting va IT service delivery.', 40),

      ('du-lieu-va-ai', 'Data Analytics', 'data-analytics', 'Excel, SQL, BI dashboards va business insights.', 10),
      ('du-lieu-va-ai', 'Machine Learning', 'machine-learning', 'Modeling, evaluation va ML engineering practices.', 20),
      ('du-lieu-va-ai', 'AI Prompting', 'ai-prompting', 'Prompt engineering, copilots va AI workflow design.', 30),
      ('du-lieu-va-ai', 'Data Engineering', 'data-engineering', 'Pipelines, warehousing, ETL va data platforms.', 40),

      ('kinh-doanh-va-quan-tri', 'Project Management', 'project-management', 'Planning, delivery, agile operations va stakeholder management.', 10),
      ('kinh-doanh-va-quan-tri', 'Product Management', 'product-management', 'Discovery, roadmap, prioritization va experimentation.', 20),
      ('kinh-doanh-va-quan-tri', 'Leadership', 'leadership', 'Team leadership, coaching va organization effectiveness.', 30),
      ('kinh-doanh-va-quan-tri', 'Entrepreneurship', 'entrepreneurship', 'Launch, operate va scale small businesses va startups.', 40),

      ('tai-chinh-va-ke-toan', 'Accounting', 'accounting', 'Bookkeeping, accounting workflows va financial records.', 10),
      ('tai-chinh-va-ke-toan', 'Financial Analysis', 'financial-analysis', 'Planning, reporting, budgeting va forecasting.', 20),

      ('thiet-ke-va-sang-tao', 'UX/UI Design', 'ux-ui-design', 'Design systems, research, interaction va interface design.', 10),
      ('thiet-ke-va-sang-tao', 'Graphic Design', 'graphic-design', 'Brand, layouts, typography va visual communication.', 20),
      ('thiet-ke-va-sang-tao', '3D va Animation', '3d-va-animation', 'Motion, 3D content, animation va visualization.', 30),

      ('marketing-va-tang-truong', 'Digital Marketing', 'digital-marketing', 'Performance, campaign setup va growth channels.', 10),
      ('marketing-va-tang-truong', 'Content Marketing', 'content-marketing', 'Content strategy, storytelling va editorial systems.', 20),
      ('marketing-va-tang-truong', 'SEO va Performance Ads', 'seo-va-performance-ads', 'Search, paid media va conversion optimization.', 30),

      ('cong-cu-van-phong', 'Excel va Spreadsheet', 'excel-va-spreadsheet', 'Spreadsheet, reporting, formula va dashboard workflows.', 10),
      ('cong-cu-van-phong', 'Presentation va Docs', 'presentation-va-docs', 'Slides, docs, proposals va structured communication.', 20),
      ('cong-cu-van-phong', 'Collaboration Tools', 'collaboration-tools', 'Google Workspace, Microsoft 365, Notion va async work.', 30),

      ('phat-trien-ban-than', 'Communication Skills', 'communication-skills', 'Presentation, writing, influence va stakeholder communication.', 10),
      ('phat-trien-ban-than', 'Career Development', 'career-development', 'CV, portfolio, interview va career planning.', 20),
      ('phat-trien-ban-than', 'Personal Productivity', 'personal-productivity', 'Focus, habits, planning va execution systems.', 30),

      ('hinh-anh-video-va-am-thanh', 'Photography', 'photography', 'Camera, composition, lighting va post-processing.', 10),
      ('hinh-anh-video-va-am-thanh', 'Video Editing', 'video-editing', 'Editing, storytelling, reels va production workflows.', 20),
      ('hinh-anh-video-va-am-thanh', 'Audio Production', 'audio-production', 'Recording, mixing, podcast va music audio basics.', 30),

      ('suc-khoe-va-loi-song', 'Fitness', 'fitness', 'Movement, training plans va healthy routines.', 10),
      ('suc-khoe-va-loi-song', 'Mental Wellness', 'mental-wellness', 'Stress management, resilience va mindful habits.', 20),

      ('giang-day-va-hoc-thuat', 'Teacher Training', 'teacher-training', 'Teaching practice, facilitation va classroom design.', 10),
      ('giang-day-va-hoc-thuat', 'Instructional Design', 'instructional-design', 'Learning paths, outcomes va digital learning experiences.', 20),
      ('giang-day-va-hoc-thuat', 'Exam Prep', 'exam-prep', 'Structured prep for certifications and assessments.', 30)
) AS seed(category_slug, name, slug, description, display_order)
JOIN course_categories c ON c.slug = seed.category_slug
ON CONFLICT (slug) DO NOTHING;

INSERT INTO course_levels (code, label, description, display_order)
VALUES
    ('beginner', 'Co ban', 'Danh cho nguoi moi bat dau hoac chua co nen tang.', 10),
    ('intermediate', 'Trung cap', 'Danh cho nguoi da co nen tang va can nang cao ky nang.', 20),
    ('advanced', 'Nang cao', 'Danh cho nguoi can scenario thuc chien va depth chuyen mon.', 30),
    ('all-levels', 'Tat ca trinh do', 'Noi dung co the theo duoc o nhieu muc do khac nhau.', 40)
ON CONFLICT (code) DO NOTHING;

INSERT INTO course_languages (code, label, display_order)
VALUES
    ('vi', 'Tieng Viet', 10),
    ('en', 'English', 20),
    ('ja', 'Japanese', 30),
    ('ko', 'Korean', 40),
    ('zh', 'Chinese', 50),
    ('fr', 'French', 60)
ON CONFLICT (code) DO NOTHING;

INSERT INTO course_tags (name, slug, display_order)
VALUES
    ('React', 'react', 10),
    ('Spring Boot', 'spring-boot', 20),
    ('Node.js', 'node-js', 30),
    ('Python', 'python', 40),
    ('SQL', 'sql', 50),
    ('Docker', 'docker', 60),
    ('Kubernetes', 'kubernetes', 70),
    ('AWS', 'aws', 80),
    ('Figma', 'figma', 90),
    ('UX Research', 'ux-research', 100),
    ('Prompt Engineering', 'prompt-engineering', 110),
    ('Data Visualization', 'data-visualization', 120),
    ('Power BI', 'power-bi', 130),
    ('Google Ads', 'google-ads', 140),
    ('Project Management', 'project-management-tag', 150),
    ('Excel', 'excel', 160)
ON CONFLICT (slug) DO NOTHING;
