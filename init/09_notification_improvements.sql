-- ① inquiries.inquiry_type 컬럼 (엔티티에만 있고 DB에 없었던 컬럼)
ALTER TABLE inquiries ADD COLUMN IF NOT EXISTS inquiry_type VARCHAR(30) NOT NULL DEFAULT 'INQUIRY';

-- ② site_settings에 알림 보존 기간 + VAPID 키 컬럼 추가
ALTER TABLE site_settings ADD COLUMN IF NOT EXISTS notification_retention_days INT NOT NULL DEFAULT 30;
ALTER TABLE site_settings ADD COLUMN IF NOT EXISTS vapid_public_key  TEXT;
ALTER TABLE site_settings ADD COLUMN IF NOT EXISTS vapid_private_key TEXT;

-- ③ 브라우저 푸시 구독 정보 테이블
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT        NOT NULL,
    endpoint   TEXT          NOT NULL,
    p256dh     TEXT          NOT NULL,
    auth_key   TEXT          NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_push_subs_endpoint  ON push_subscriptions(endpoint);
CREATE        INDEX IF NOT EXISTS idx_push_subs_member_id ON push_subscriptions(member_id);

-- ④ notification_defaults 에 누락된 타입 추가
INSERT INTO notification_defaults (notif_type, system_enabled, email_enabled, label) VALUES
    ('INQUIRY',       true,  false, '새 문의 접수 (관리자)'),
    ('INQUIRY_REPLY', true,  false, '내 문의 답변')
ON CONFLICT (notif_type) DO NOTHING;
