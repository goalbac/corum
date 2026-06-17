-- 회원별 알림 수신 설정
CREATE TABLE IF NOT EXISTS member_notification_prefs (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    notif_type  VARCHAR(60)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (member_id, notif_type)
);
CREATE INDEX IF NOT EXISTS idx_member_notif_prefs_member_id ON member_notification_prefs(member_id);

-- 알림 기본값 (관리자가 설정, 신규 회원 가입 시 적용)
CREATE TABLE IF NOT EXISTS notification_defaults (
    notif_type  VARCHAR(60)  PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    label       VARCHAR(100) NOT NULL
);

INSERT INTO notification_defaults (notif_type, label) VALUES
    ('COMMENT_ON_MY_POST',        '내 글에 댓글'),
    ('REPLY_ON_MY_COMMENT',       '내 댓글에 답글'),
    ('NEW_POST_ON_MANAGED_BOARD', '관리 게시판 새 글'),
    ('CALENDAR_EVENT',            '새 일정 등록'),
    ('MESSAGE',                   '쪽지 수신')
ON CONFLICT (notif_type) DO NOTHING;
