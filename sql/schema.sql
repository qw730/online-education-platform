-- ===========================================================================
--  Online Personal Education Platform - MySQL schema
-- ===========================================================================
--  Run this script before starting the backend if you prefer a clean,
--  version-controlled schema instead of Hibernate's automatic DDL:
--
--      mysql -u root -p < sql/schema.sql
--
--  Then set  spring.jpa.hibernate.ddl-auto=none  in application.yml.
--
--  NOTE: run this file with UTF-8 client encoding:
--      mysql --default-character-set=utf8mb4 -u root -p < sql/schema.sql
-- ===========================================================================

DROP DATABASE IF EXISTS online_education;
CREATE DATABASE online_education
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE online_education;

-- ---------------------------------------------------------------------------
-- 1. Accounts: students and administrators
-- ---------------------------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT 'login name',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt hash',
    real_name   VARCHAR(50)           COMMENT 'display name',
    email       VARCHAR(100)          COMMENT 'contact email',
    role        VARCHAR(20)  NOT NULL COMMENT 'STUDENT / ADMIN',
    avatar      VARCHAR(255),
    bio         VARCHAR(255),
    status      INT          NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at  DATETIME,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB COMMENT 'Platform accounts';

-- ---------------------------------------------------------------------------
-- 2. Course categories
-- ---------------------------------------------------------------------------
CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    icon        VARCHAR(50),
    sort_order  INT                   DEFAULT 0,
    created_at  DATETIME,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB COMMENT 'Course categories';

-- ---------------------------------------------------------------------------
-- 3. Courses
-- ---------------------------------------------------------------------------
CREATE TABLE course (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200)  NOT NULL,
    subtitle       VARCHAR(255),
    description    TEXT,
    cover_url      VARCHAR(500),
    category_id    BIGINT        NOT NULL,
    teacher_name   VARCHAR(50),
    level          VARCHAR(20)            DEFAULT 'BEGINNER' COMMENT 'BEGINNER / INTERMEDIATE / ADVANCED',
    price          DECIMAL(10, 2)         DEFAULT 0.00,
    duration_hours INT                    DEFAULT 0,
    tags           VARCHAR(255)           COMMENT 'comma separated keywords, used by the recommender',
    status         INT           NOT NULL DEFAULT 1 COMMENT '1 published, 0 draft',
    student_count  INT           NOT NULL DEFAULT 0,
    rating         DECIMAL(3, 2)          DEFAULT 0.00,
    created_at     DATETIME,
    updated_at     DATETIME,
    PRIMARY KEY (id),
    KEY idx_course_category (category_id),
    KEY idx_course_status (status),
    CONSTRAINT fk_course_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB COMMENT 'Courses';

-- ---------------------------------------------------------------------------
-- 4. Lessons (chapters) of a course - drive the learning progress
-- ---------------------------------------------------------------------------
CREATE TABLE lesson (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    course_id        BIGINT       NOT NULL,
    title            VARCHAR(200) NOT NULL,
    summary          VARCHAR(500),
    content          TEXT,
    video_url        VARCHAR(500),
    duration_minutes INT                   DEFAULT 0,
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       DATETIME,
    updated_at       DATETIME,
    PRIMARY KEY (id),
    KEY idx_lesson_course (course_id),
    CONSTRAINT fk_lesson_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE = InnoDB COMMENT 'Course chapters';

-- ---------------------------------------------------------------------------
-- 5. Enrolment ("选课")
-- ---------------------------------------------------------------------------
CREATE TABLE enrollment (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    course_id         BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' COMMENT 'ENROLLED / COMPLETED / DROPPED',
    progress_percent  INT         NOT NULL DEFAULT 0,
    completed_lessons INT         NOT NULL DEFAULT 0,
    total_lessons     INT         NOT NULL DEFAULT 0,
    enrolled_at       DATETIME,
    completed_at      DATETIME,
    last_study_at     DATETIME,
    created_at        DATETIME,
    updated_at        DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollment_user_course (user_id, course_id),
    KEY idx_enrollment_user (user_id),
    KEY idx_enrollment_course (course_id),
    CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE = InnoDB COMMENT 'Student enrolments';

-- ---------------------------------------------------------------------------
-- 6. Per-lesson learning record
-- ---------------------------------------------------------------------------
CREATE TABLE lesson_progress (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    user_id         BIGINT   NOT NULL,
    course_id       BIGINT   NOT NULL,
    lesson_id       BIGINT   NOT NULL,
    completed       TINYINT(1) NOT NULL DEFAULT 0,
    watched_minutes INT      NOT NULL DEFAULT 0,
    last_viewed_at  DATETIME,
    created_at      DATETIME,
    updated_at      DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_progress_user_lesson (user_id, lesson_id),
    KEY idx_progress_user_course (user_id, course_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id)
) ENGINE = InnoDB COMMENT 'Lesson level learning records';

-- ---------------------------------------------------------------------------
-- 7. Study plans
-- ---------------------------------------------------------------------------
CREATE TABLE study_plan (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    course_id    BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    goal         VARCHAR(500),
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    daily_minutes INT                  DEFAULT 30,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PLANNED'
        COMMENT 'PLANNED / IN_PROGRESS / COMPLETED / ABANDONED',
    remark       VARCHAR(500),
    created_at   DATETIME,
    updated_at   DATETIME,
    PRIMARY KEY (id),
    KEY idx_plan_user (user_id),
    KEY idx_plan_course (course_id),
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_plan_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE = InnoDB COMMENT 'Personal study plans';

CREATE TABLE study_plan_item (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    plan_id      BIGINT       NOT NULL,
    lesson_id    BIGINT,
    title        VARCHAR(200) NOT NULL,
    planned_date DATE         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / DONE',
    completed_at DATETIME,
    created_at   DATETIME,
    updated_at   DATETIME,
    PRIMARY KEY (id),
    KEY idx_plan_item_plan (plan_id),
    CONSTRAINT fk_plan_item_plan FOREIGN KEY (plan_id) REFERENCES study_plan (id)
) ENGINE = InnoDB COMMENT 'Tasks of a study plan';

-- ---------------------------------------------------------------------------
-- 8. Quizzes
-- ---------------------------------------------------------------------------
CREATE TABLE quiz (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    course_id        BIGINT       NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(500),
    duration_minutes INT                   DEFAULT 0,
    pass_score       INT                   DEFAULT 60 COMMENT 'percent needed to pass',
    max_attempts     INT                   DEFAULT 0 COMMENT '0 = unlimited',
    status           INT          NOT NULL DEFAULT 1 COMMENT '1 published, 0 draft',
    created_at       DATETIME,
    updated_at       DATETIME,
    PRIMARY KEY (id),
    KEY idx_quiz_course (course_id),
    CONSTRAINT fk_quiz_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE = InnoDB COMMENT 'Course quizzes';

CREATE TABLE question (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    quiz_id        BIGINT      NOT NULL,
    content        TEXT        NOT NULL,
    type           VARCHAR(20) NOT NULL DEFAULT 'SINGLE' COMMENT 'SINGLE / MULTIPLE / TRUE_FALSE',
    correct_answer VARCHAR(50) NOT NULL COMMENT 'A / A,C / T / F',
    score          INT         NOT NULL DEFAULT 10,
    analysis       VARCHAR(500),
    sort_order     INT         NOT NULL DEFAULT 0,
    created_at     DATETIME,
    updated_at     DATETIME,
    PRIMARY KEY (id),
    KEY idx_question_quiz (quiz_id),
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id)
) ENGINE = InnoDB COMMENT 'Quiz questions';

CREATE TABLE question_option (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    question_id BIGINT       NOT NULL,
    option_key  VARCHAR(4)   NOT NULL,
    content     VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME,
    updated_at  DATETIME,
    PRIMARY KEY (id),
    KEY idx_option_question (question_id),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE = InnoDB COMMENT 'Selectable options of a question';

-- ---------------------------------------------------------------------------
-- 9. Quiz submissions
-- ---------------------------------------------------------------------------
CREATE TABLE quiz_attempt (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    user_id          BIGINT   NOT NULL,
    quiz_id          BIGINT   NOT NULL,
    course_id        BIGINT   NOT NULL,
    score            INT      NOT NULL DEFAULT 0,
    total_score      INT      NOT NULL DEFAULT 0,
    correct_count    INT      NOT NULL DEFAULT 0,
    total_count      INT      NOT NULL DEFAULT 0,
    passed           TINYINT(1) NOT NULL DEFAULT 0,
    duration_seconds INT               DEFAULT 0,
    answers_json     TEXT     COMMENT 'raw answers as JSON, for review',
    created_at       DATETIME,
    updated_at       DATETIME,
    PRIMARY KEY (id),
    KEY idx_attempt_user (user_id),
    KEY idx_attempt_quiz (quiz_id),
    CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id)
) ENGINE = InnoDB COMMENT 'Quiz submissions';

-- ===========================================================================
--  Demo data
-- ===========================================================================
--  This file only creates the structure. The demo accounts, categories,
--  courses, lessons and quizzes are inserted automatically the first time the
--  backend starts against an empty database:
--
--      com.edu.platform.config.DemoDataInitializer
--
--  Default credentials:  admin / admin123      student / student123
--
--  Set  app.init-demo-data=false  in application.yml if you would rather keep
--  the database empty. A ready-to-import snapshot of the seeded data lives in
--  sql/demo-data.sql.
-- ===========================================================================
