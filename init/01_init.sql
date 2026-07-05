-- =============================================
-- 사단법인 홈페이지 DDL (완전 통합본)
-- 원칙: FK 제약조건 없음, CHECK 없음, ENUM 없음
--       인덱스만 사용, 정합성은 애플리케이션에서 보장
-- =============================================

-- =============================================
-- 1. 회원 / 그룹 / 약관
-- =============================================

CREATE TABLE members (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(50)  NOT NULL,
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    name                 VARCHAR(100),
    gender               VARCHAR(10),
    phone                VARCHAR(30),
    address              VARCHAR(500),
    birth_date           DATE,
    home_phone           VARCHAR(30),
    occupation           VARCHAR(200),
    work_phone           VARCHAR(30),
    newsletter_yn        BOOLEAN      NOT NULL DEFAULT FALSE,
    profile_image_url    VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT FALSE,
    is_locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    login_fail_count     INT          NOT NULL DEFAULT 0,
    locked_at            TIMESTAMP,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    joined_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    withdrawn_at         TIMESTAMP,
    withdrawn_ip         VARCHAR(50),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_members_username ON members(username);
CREATE UNIQUE INDEX idx_members_email    ON members(email);
CREATE INDEX idx_members_is_active       ON members(is_active);
CREATE INDEX idx_members_joined_at       ON members(joined_at);

CREATE TABLE groups (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    sort_order  INT          NOT NULL DEFAULT 0,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_groups_parent_id ON groups(parent_id);
CREATE INDEX idx_groups_type      ON groups(type);

CREATE TABLE member_groups (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT    NOT NULL,
    group_id    BIGINT    NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_member_groups_unique ON member_groups(member_id, group_id);
CREATE INDEX idx_member_groups_member_id     ON member_groups(member_id);
CREATE INDEX idx_member_groups_group_id      ON member_groups(group_id);

CREATE TABLE terms (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(50)  NOT NULL,
    version         INT          NOT NULL DEFAULT 1,
    content         TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT FALSE,
    require_reagree BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_terms_type      ON terms(type);
CREATE INDEX idx_terms_is_active ON terms(is_active);

CREATE TABLE member_terms_agreements (
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT    NOT NULL,
    terms_id  BIGINT    NOT NULL,
    agreed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    agreed_ip VARCHAR(50)
);
CREATE UNIQUE INDEX idx_mta_unique ON member_terms_agreements(member_id, terms_id);
CREATE INDEX idx_mta_member_id    ON member_terms_agreements(member_id);
CREATE INDEX idx_mta_terms_id     ON member_terms_agreements(terms_id);

CREATE TABLE admin_member_memos (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL,
    created_by BIGINT,
    memo       TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_member_memos_member_id ON admin_member_memos(member_id);

CREATE TABLE member_login_logs (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT,
    action     VARCHAR(20)  NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    success    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mll_member_id  ON member_login_logs(member_id);
CREATE INDEX idx_mll_created_at ON member_login_logs(created_at);

-- =============================================
-- 2. 메뉴 / 관리자 패널 / 사이트 설정 / 감사로그
-- =============================================

CREATE TABLE menus (
    id                    BIGSERIAL PRIMARY KEY,
    parent_id             BIGINT,
    name                  VARCHAR(100) NOT NULL,
    description           VARCHAR(500),
    menu_type             VARCHAR(20)  NOT NULL DEFAULT 'PAGE',
    page_type             VARCHAR(20),
    target_id             BIGINT,
    url                   VARCHAR(500),
    url_auto              BOOLEAN      NOT NULL DEFAULT TRUE,
    new_window            BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order            INT          NOT NULL DEFAULT 0,
    is_hidden             BOOLEAN      NOT NULL DEFAULT FALSE,
    hide_if_no_permission BOOLEAN      NOT NULL DEFAULT FALSE,
    access_type           VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    show_holiday          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_menus_parent_id ON menus(parent_id);
CREATE INDEX idx_menus_is_active ON menus(is_active);

CREATE TABLE menu_group_permissions (
    id       BIGSERIAL PRIMARY KEY,
    menu_id  BIGINT NOT NULL,
    group_id BIGINT NOT NULL
);
CREATE UNIQUE INDEX idx_mgp_unique ON menu_group_permissions(menu_id, group_id);
CREATE INDEX idx_mgp_menu_id      ON menu_group_permissions(menu_id);
CREATE INDEX idx_mgp_group_id     ON menu_group_permissions(group_id);

CREATE TABLE menu_calendar_targets (
    id          BIGSERIAL PRIMARY KEY,
    menu_id     BIGINT NOT NULL,
    calendar_id BIGINT NOT NULL
);
CREATE INDEX idx_menu_calendar_targets_menu_id ON menu_calendar_targets(menu_id);

CREATE TABLE admin_menus (
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT,
    name       VARCHAR(100) NOT NULL,
    url        VARCHAR(500),
    icon       VARCHAR(100),
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_admin_menus_parent_id ON admin_menus(parent_id);

CREATE TABLE admin_menu_group_permissions (
    id            BIGSERIAL PRIMARY KEY,
    admin_menu_id BIGINT  NOT NULL,
    group_id      BIGINT  NOT NULL,
    can_view      BOOLEAN NOT NULL DEFAULT FALSE,
    can_edit      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX idx_amgp_unique    ON admin_menu_group_permissions(admin_menu_id, group_id);
CREATE INDEX idx_amgp_admin_menu_id    ON admin_menu_group_permissions(admin_menu_id);
CREATE INDEX idx_amgp_group_id         ON admin_menu_group_permissions(group_id);

CREATE TABLE site_settings (
    id                          BIGSERIAL PRIMARY KEY,
    site_name                   VARCHAR(200),
    site_description            VARCHAR(500),
    favicon_url                 VARCHAR(500),
    maintenance_mode            BOOLEAN      NOT NULL DEFAULT FALSE,
    maintenance_message         VARCHAR(500),
    maintenance_until           TIMESTAMP,
    login_fail_limit            INT          NOT NULL DEFAULT 5,
    session_timeout_min         INT          NOT NULL DEFAULT 60,
    allow_concurrent_login      BOOLEAN      NOT NULL DEFAULT TRUE,
    file_max_size_mb            INT          NOT NULL DEFAULT 10,
    file_allowed_extensions     VARCHAR(500),
    smtp_host                   VARCHAR(200),
    smtp_port                   INT          NOT NULL DEFAULT 587,
    smtp_username               VARCHAR(200),
    smtp_password_enc           VARCHAR(500),
    smtp_use_tls                BOOLEAN      NOT NULL DEFAULT TRUE,
    footer_html                 TEXT,
    logo_url                    VARCHAR(500),
    contact_address             VARCHAR(500),
    contact_phone               VARCHAR(100),
    admin_email                 VARCHAR(200),
    notification_retention_days INT          NOT NULL DEFAULT 30,
    vapid_public_key            TEXT,
    vapid_private_key           TEXT,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by                  BIGINT
);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    member_id    BIGINT,
    action_type  VARCHAR(50)  NOT NULL,
    target_table VARCHAR(100),
    target_id    BIGINT,
    before_value TEXT,
    after_value  TEXT,
    ip_address   VARCHAR(50),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_member_id   ON audit_logs(member_id);
CREATE INDEX idx_audit_logs_action_type ON audit_logs(action_type);
CREATE INDEX idx_audit_logs_created_at  ON audit_logs(created_at);

-- =============================================
-- 3. 게시판 / 댓글 / 파일
-- =============================================

CREATE TABLE boards (
    id                      BIGSERIAL PRIMARY KEY,
    menu_id                 BIGINT,
    name                    VARCHAR(100) NOT NULL,
    board_type              VARCHAR(20)  NOT NULL DEFAULT 'POST',
    use_comment             BOOLEAN      NOT NULL DEFAULT TRUE,
    use_like                BOOLEAN      NOT NULL DEFAULT TRUE,
    use_anonymous           BOOLEAN      NOT NULL DEFAULT FALSE,
    use_notice              BOOLEAN      NOT NULL DEFAULT TRUE,
    use_all_category        BOOLEAN      NOT NULL DEFAULT FALSE,
    use_alias_writer        BOOLEAN      NOT NULL DEFAULT FALSE,
    notice_count_limit      INT          NOT NULL DEFAULT 5,
    file_max_size_mb        INT,
    file_allowed_extensions VARCHAR(500),
    file_max_count          INT          NOT NULL DEFAULT 5,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_boards_menu_id   ON boards(menu_id);
CREATE INDEX idx_boards_is_active ON boards(is_active);

CREATE TABLE board_group_permissions (
    id           BIGSERIAL PRIMARY KEY,
    board_id     BIGINT  NOT NULL,
    group_id     BIGINT  NOT NULL,
    can_read     BOOLEAN NOT NULL DEFAULT TRUE,
    can_write    BOOLEAN NOT NULL DEFAULT FALSE,
    can_comment  BOOLEAN NOT NULL DEFAULT FALSE,
    can_download BOOLEAN NOT NULL DEFAULT TRUE,
    can_manage   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX idx_bgp_unique ON board_group_permissions(board_id, group_id);
CREATE INDEX idx_bgp_board_id      ON board_group_permissions(board_id);
CREATE INDEX idx_bgp_group_id      ON board_group_permissions(group_id);

CREATE TABLE board_categories (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_board_categories_board_id ON board_categories(board_id);

-- 게시판별 "다른 이름으로 게시" 기능에서 선택 가능한 표시 이름 목록
CREATE TABLE board_writer_identities (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_board_writer_identities_board_id ON board_writer_identities(board_id);

CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    board_id    BIGINT       NOT NULL,
    member_id   BIGINT,
    category_id BIGINT,
    title       VARCHAR(500) NOT NULL,
    content     TEXT,
    writer_name VARCHAR(100),
    is_notice   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_hidden   BOOLEAN      NOT NULL DEFAULT FALSE,
    view_count  INT          NOT NULL DEFAULT 0,
    like_count  INT          NOT NULL DEFAULT 0,
    client_ip   VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  BIGINT
);
CREATE INDEX idx_posts_board_id    ON posts(board_id);
CREATE INDEX idx_posts_member_id   ON posts(member_id);
CREATE INDEX idx_posts_category_id ON posts(category_id);
CREATE INDEX idx_posts_is_notice   ON posts(is_notice);
CREATE INDEX idx_posts_created_at  ON posts(created_at);

CREATE TABLE post_likes (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    member_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_post_likes_unique ON post_likes(post_id, member_id);
CREATE INDEX idx_post_likes_post_id       ON post_likes(post_id);

CREATE TABLE post_reactions (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    emoji_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_post_reactions_unique  ON post_reactions(post_id, member_id, emoji_type);
CREATE INDEX idx_post_reactions_post_id        ON post_reactions(post_id);

CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT    NOT NULL,
    parent_id   BIGINT,
    member_id   BIGINT,
    writer_name VARCHAR(100),
    content     TEXT      NOT NULL,
    depth       INT       NOT NULL DEFAULT 0,
    sort_order  INT       NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN   NOT NULL DEFAULT FALSE,
    client_ip   VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comments_post_id   ON comments(post_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_member_id ON comments(member_id);

CREATE TABLE comment_reactions (
    id         BIGSERIAL PRIMARY KEY,
    comment_id BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    emoji_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_comment_reactions_unique     ON comment_reactions(comment_id, member_id, emoji_type);
CREATE INDEX idx_comment_reactions_comment_id        ON comment_reactions(comment_id);

CREATE TABLE files (
    id             BIGSERIAL  PRIMARY KEY,
    target_type    VARCHAR(50)   NOT NULL,
    target_id      BIGINT        NOT NULL,
    original_name  VARCHAR(500)  NOT NULL,
    stored_name    VARCHAR(500)  NOT NULL,
    storage_path   VARCHAR(1000) NOT NULL,
    thumbnail_path VARCHAR(1000),
    mime_type      VARCHAR(200),
    file_size      BIGINT        NOT NULL DEFAULT 0,
    download_count INT           NOT NULL DEFAULT 0,
    uploaded_by    BIGINT,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_files_target      ON files(target_type, target_id);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);

CREATE TABLE file_download_logs (
    id         BIGSERIAL PRIMARY KEY,
    file_id    BIGINT    NOT NULL,
    member_id  BIGINT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_fdl_file_id    ON file_download_logs(file_id);
CREATE INDEX idx_fdl_created_at ON file_download_logs(created_at);

CREATE TABLE document_folders (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT       NOT NULL,
    parent_id  BIGINT,
    name       VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_folders_board_id  ON document_folders(board_id);
CREATE INDEX idx_doc_folders_parent_id ON document_folders(parent_id);

CREATE TABLE document_versions (
    id             BIGSERIAL PRIMARY KEY,
    post_id        BIGINT       NOT NULL,
    version_number INT          NOT NULL DEFAULT 1,
    file_id        BIGINT,
    change_note    VARCHAR(500),
    created_by     BIGINT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doc_versions_post_id ON document_versions(post_id);

-- =============================================
-- 4. 쪽지 / 문의 / 캘린더
-- =============================================

CREATE TABLE messages (
    id         BIGSERIAL PRIMARY KEY,
    sender_id  BIGINT,
    title      VARCHAR(500) NOT NULL,
    content    TEXT,
    is_notice  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_messages_sender_id  ON messages(sender_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

CREATE TABLE message_recipients (
    id                      BIGSERIAL PRIMARY KEY,
    message_id              BIGINT    NOT NULL,
    recipient_id            BIGINT    NOT NULL,
    is_read                 BOOLEAN   NOT NULL DEFAULT FALSE,
    read_at                 TIMESTAMP,
    is_deleted_by_sender    BOOLEAN   NOT NULL DEFAULT FALSE,
    is_deleted_by_recipient BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mr_message_id   ON message_recipients(message_id);
CREATE INDEX idx_mr_recipient_id ON message_recipients(recipient_id);

CREATE TABLE inquiries (
    id            BIGSERIAL PRIMARY KEY,
    member_id     BIGINT,
    writer_name   VARCHAR(100),
    inquiry_type  VARCHAR(30)  NOT NULL DEFAULT 'INQUIRY',
    title         VARCHAR(500) NOT NULL,
    content       TEXT,
    contact_phone VARCHAR(50),
    contact_email VARCHAR(255),
    client_ip     VARCHAR(50),
    status        VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    reply_content TEXT,
    replied_at    TIMESTAMP,
    replied_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inquiries_member_id  ON inquiries(member_id);
CREATE INDEX idx_inquiries_status     ON inquiries(status);
CREATE INDEX idx_inquiries_created_at ON inquiries(created_at);

CREATE TABLE inquiry_memos (
    id         BIGSERIAL PRIMARY KEY,
    inquiry_id BIGINT    NOT NULL,
    created_by BIGINT,
    memo       TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inquiry_memos_inquiry_id ON inquiry_memos(inquiry_id);

CREATE TABLE calendars (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    color         VARCHAR(20),
    description   VARCHAR(500),
    calendar_type VARCHAR(20)  NOT NULL DEFAULT 'GENERAL',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_events (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT       NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    location        VARCHAR(500),
    start_at        TIMESTAMP    NOT NULL,
    end_at          TIMESTAMP,
    is_all_day      BOOLEAN      NOT NULL DEFAULT FALSE,
    recurrence_type VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    recurrence_rule TEXT,
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ce_calendar_id ON calendar_events(calendar_id);
CREATE INDEX idx_ce_start_at    ON calendar_events(start_at);

CREATE TABLE calendar_group_permissions (
    id          BIGSERIAL PRIMARY KEY,
    calendar_id BIGINT  NOT NULL,
    group_id    BIGINT  NOT NULL,
    can_read    BOOLEAN NOT NULL DEFAULT TRUE,
    can_write   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX idx_cgp_unique ON calendar_group_permissions(calendar_id, group_id);
CREATE INDEX idx_cgp_calendar_id   ON calendar_group_permissions(calendar_id);
CREATE INDEX idx_cgp_group_id      ON calendar_group_permissions(group_id);

-- =============================================
-- 5. 팝업 / 배너 / 안내페이지 / 통계
-- =============================================

CREATE TABLE popups (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content_type    VARCHAR(20)  NOT NULL DEFAULT 'IMAGE',
    content         TEXT,
    image_url       VARCHAR(500),
    link_url        VARCHAR(500),
    link_new_window BOOLEAN      NOT NULL DEFAULT FALSE,
    position        VARCHAR(20)  NOT NULL DEFAULT 'CENTER',
    priority        INT          NOT NULL DEFAULT 0,
    start_at        TIMESTAMP,
    end_at          TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_popups_is_active ON popups(is_active);
CREATE INDEX idx_popups_start_end ON popups(start_at, end_at);

CREATE TABLE popup_target_pages (
    id             BIGSERIAL PRIMARY KEY,
    popup_id       BIGINT      NOT NULL,
    target_type    VARCHAR(20) NOT NULL DEFAULT 'ALL',
    target_menu_id BIGINT
);
CREATE INDEX idx_ptp_popup_id ON popup_target_pages(popup_id);

CREATE TABLE banners (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT,
    link_url        VARCHAR(500),
    link_new_window BOOLEAN      NOT NULL DEFAULT FALSE,
    bg_color        VARCHAR(30),
    text_align      VARCHAR(10),
    start_at        TIMESTAMP,
    end_at          TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_banners_is_active ON banners(is_active);

CREATE TABLE content_pages (
    id         BIGSERIAL PRIMARY KEY,
    menu_id    BIGINT,
    title      VARCHAR(200) NOT NULL,
    content    TEXT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_content_pages_menu_id ON content_pages(menu_id);

CREATE TABLE content_page_histories (
    id              BIGSERIAL PRIMARY KEY,
    content_page_id BIGINT    NOT NULL,
    content         TEXT,
    created_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cph_content_page_id ON content_page_histories(content_page_id);

CREATE TABLE dashboard_widgets (
    id              BIGSERIAL PRIMARY KEY,
    menu_id         BIGINT,
    widget_type     VARCHAR(50)  NOT NULL,
    title           VARCHAR(200),
    description     VARCHAR(500),
    target_board_id BIGINT,
    post_count      INT          NOT NULL DEFAULT 5,
    sort_order      INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    extra_config    TEXT,
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      BIGINT
);
CREATE INDEX idx_dashboard_widgets_menu_id ON dashboard_widgets(menu_id);

CREATE TABLE visit_logs (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    request_uri VARCHAR(1000),
    referer     VARCHAR(1000),
    visit_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_visit_logs_visit_date ON visit_logs(visit_date);
CREATE INDEX idx_visit_logs_member_id  ON visit_logs(member_id);
CREATE INDEX idx_visit_logs_ip_address ON visit_logs(ip_address);

CREATE TABLE visit_stats (
    id            BIGSERIAL PRIMARY KEY,
    stat_date     DATE      NOT NULL,
    total_visits  INT       NOT NULL DEFAULT 0,
    unique_visits INT       NOT NULL DEFAULT 0,
    login_visits  INT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_visit_stats_date ON visit_stats(stat_date);

CREATE TABLE search_logs (
    id           BIGSERIAL PRIMARY KEY,
    member_id    BIGINT,
    keyword      VARCHAR(500) NOT NULL,
    search_type  VARCHAR(50),
    result_count INT          NOT NULL DEFAULT 0,
    ip_address   VARCHAR(50),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_search_logs_keyword    ON search_logs(keyword);
CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);

CREATE TABLE smtp_send_logs (
    id            BIGSERIAL PRIMARY KEY,
    member_id     BIGINT,
    to_email      VARCHAR(255) NOT NULL,
    subject       VARCHAR(500),
    send_type     VARCHAR(50),
    success       BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_smtp_send_logs_member_id  ON smtp_send_logs(member_id);
CREATE INDEX idx_smtp_send_logs_created_at ON smtp_send_logs(created_at);

-- =============================================
-- 6. 알림 / 웹 푸시
-- =============================================

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(500),
    content    TEXT,
    link_url   VARCHAR(500),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_member_id ON notifications(member_id);
CREATE INDEX idx_notifications_is_read   ON notifications(member_id, is_read);

CREATE TABLE notification_defaults (
    notif_type     VARCHAR(60)  PRIMARY KEY,
    system_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    email_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    label          VARCHAR(100) NOT NULL
);

CREATE TABLE member_notification_prefs (
    id             BIGSERIAL PRIMARY KEY,
    member_id      BIGINT      NOT NULL,
    notif_type     VARCHAR(60) NOT NULL,
    system_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    email_enabled  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_mnp_unique ON member_notification_prefs(member_id, notif_type);

CREATE TABLE push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT        NOT NULL,
    endpoint   TEXT          NOT NULL,
    p256dh     TEXT          NOT NULL,
    auth_key   TEXT          NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_push_subs_endpoint  ON push_subscriptions(endpoint);
CREATE INDEX idx_push_subs_member_id        ON push_subscriptions(member_id);

-- =============================================
-- 7. 인증 (JWT 무효화 토큰)
-- =============================================

-- 강제 로그아웃/계정 잠금/비밀번호 재설정 등으로 무효화된 JWT 목록.
-- 서버 재시작에도 유지되도록 DB에 저장(기존 인메모리 방식 대체).
-- 원문 토큰이 아닌 SHA-256 해시만 저장한다.
CREATE TABLE invalidated_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_invalidated_tokens_expires_at ON invalidated_tokens(expires_at);

-- =============================================
-- 기본 데이터 삽입
-- =============================================

INSERT INTO site_settings (
    site_name, site_description, login_fail_limit,
    session_timeout_min, allow_concurrent_login,
    file_max_size_mb, smtp_port, smtp_use_tls,
    notification_retention_days
) VALUES (
    'Corum', '단체/협회 홈페이지', 5,
    60, TRUE,
    10, 587, TRUE,
    30
);

INSERT INTO groups (name, description, type, sort_order, is_system) VALUES
    ('운영', '관리자 패널 접근 권한을 가진 운영 그룹', 'ADMIN', 1, TRUE),
    ('일반', '일반 페이지 접근 권한을 가진 일반 그룹', 'NORMAL', 2, TRUE);

INSERT INTO groups (parent_id, name, description, type, sort_order, is_system) VALUES
    (1, '최고관리자', '모든 권한 보유', 'ADMIN', 1, TRUE),
    (2, '정회원',     '정식 가입 회원', 'NORMAL', 1, FALSE),
    (2, '일반회원',   '가입 기본값',    'NORMAL', 2, FALSE);

-- 기본 관리자 계정 (password: password123)
INSERT INTO members (username, email, password_hash, name, is_active, is_locked, login_fail_count, joined_at)
VALUES ('admin', 'admin@corum.com', '$2a$10$DrgltpyzAAwMqBoUdpZfpOj1wP8LmjdfMd5geKFL3oXRB9vkAdiRi', '관리자', TRUE, FALSE, 0, NOW());

INSERT INTO member_groups (member_id, group_id, assigned_at)
SELECT m.id, g.id, NOW()
FROM members m, groups g
WHERE m.username = 'admin' AND g.name = '최고관리자';

INSERT INTO terms (type, version, content, is_active, require_reagree) VALUES
    ('SERVICE', 1, '이용약관 내용을 입력해주세요.', TRUE, FALSE),
    ('PRIVACY', 1, '개인정보처리방침 내용을 입력해주세요.', TRUE, FALSE);

-- 기본 캘린더
INSERT INTO calendars (name, color, description, calendar_type, is_active) VALUES
    ('주요 일정',       '#4f6ef7', '단체 주요 일정', 'GENERAL', TRUE),
    ('대한민국의 휴일', '#dc2626', '대한민국 공휴일 및 기념일', 'HOLIDAY', TRUE);

-- 알림 기본 설정
INSERT INTO notification_defaults (notif_type, system_enabled, email_enabled, label) VALUES
    ('NEW_MESSAGE',   TRUE, FALSE, '새 쪽지'),
    ('NEW_COMMENT',   TRUE, FALSE, '내 글에 댓글'),
    ('NEW_REPLY',     TRUE, FALSE, '댓글에 답글'),
    ('SYSTEM_NOTICE', TRUE, FALSE, '시스템 공지'),
    ('INQUIRY',       TRUE, FALSE, '새 문의 접수 (관리자)'),
    ('INQUIRY_REPLY', TRUE, FALSE, '내 문의 답변');

-- 관리자 메뉴 시드
INSERT INTO admin_menus (parent_id, name, url, icon, sort_order, is_active) VALUES
    (NULL, '대시보드', '/admin',  'ti ti-layout-dashboard', 1, TRUE),
    (NULL, '콘텐츠',   NULL,      'ti ti-folders',          2, TRUE),
    (NULL, '운영',     NULL,      'ti ti-settings-2',       3, TRUE),
    (NULL, '회원',     NULL,      'ti ti-users',            4, TRUE),
    (NULL, '설정',     NULL,      'ti ti-settings',         5, TRUE);

INSERT INTO admin_menus (parent_id, name, url, icon, sort_order, is_active) VALUES
    (2, '메뉴 관리',        '/admin/menus',             'ti ti-menu-2',           1, TRUE),
    (2, '게시판 관리',      '/admin/boards',            'ti ti-layout-list',      2, TRUE),
    (2, '대시보드 관리',    '/admin/dashboard-widgets', 'ti ti-layout-dashboard', 3, TRUE),
    (2, '캘린더 관리',      '/admin/calendars',         'ti ti-calendar',         4, TRUE),
    (2, '안내 페이지 관리', '/admin/content-pages',     'ti ti-file-text',        5, TRUE),
    (2, '팝업/배너 관리',   '/admin/display',           'ti ti-speakerphone',     6, TRUE);

INSERT INTO admin_menus (parent_id, name, url, icon, sort_order, is_active) VALUES
    (3, '문의 관리', '/admin/inquiries', 'ti ti-mail',         1, TRUE),
    (3, '로그',      '/admin/stats',     'ti ti-list-details', 2, TRUE);

INSERT INTO admin_menus (parent_id, name, url, icon, sort_order, is_active) VALUES
    (4, '회원 관리', '/admin/members', 'ti ti-users',      1, TRUE),
    (4, '그룹 관리', '/admin/groups',  'ti ti-shield',     2, TRUE),
    (4, '약관 관리', '/admin/terms',   'ti ti-file-check', 3, TRUE);

INSERT INTO admin_menus (parent_id, name, url, icon, sort_order, is_active) VALUES
    (5, '사이트 설정',      '/admin/settings',          'ti ti-settings', 1, TRUE),
    (5, '관리자 메뉴 권한', '/admin/admin-permissions', 'ti ti-lock',     2, TRUE),
    (5, '시스템 도구',      '/admin/tools',             'ti ti-tool',     3, TRUE);
