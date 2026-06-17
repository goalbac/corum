-- enabled 컬럼을 system_enabled + email_enabled 두 컬럼으로 분리
ALTER TABLE member_notification_prefs
    ADD COLUMN IF NOT EXISTS system_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_enabled  BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE member_notification_prefs
    DROP COLUMN IF EXISTS enabled;

ALTER TABLE notification_defaults
    ADD COLUMN IF NOT EXISTS system_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_enabled  BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE notification_defaults
    DROP COLUMN IF EXISTS enabled;
