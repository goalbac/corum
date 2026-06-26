-- 비밀번호 강제 변경 플래그 컬럼 추가
ALTER TABLE members ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
