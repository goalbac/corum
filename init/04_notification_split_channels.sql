-- enabled 컬럼을 system_enabled + email_enabled 두 컬럼으로 분리

ALTER TABLE member_notification_prefs
    ADD COLUMN system_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_enabled  BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE member_notification_prefs
    SET system_enabled = enabled, email_enabled = FALSE;

ALTER TABLE member_notification_prefs DROP COLUMN enabled;

ALTER TABLE notification_defaults
    ADD COLUMN system_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_enabled  BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE notification_defaults
    SET system_enabled = enabled, email_enabled = FALSE;

ALTER TABLE notification_defaults DROP COLUMN enabled;
