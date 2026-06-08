-- 푸터 HTML 컬럼 추가
ALTER TABLE site_settings ADD COLUMN IF NOT EXISTS footer_html TEXT;
