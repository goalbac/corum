-- 파일 썸네일 경로 컬럼 추가
ALTER TABLE files ADD COLUMN IF NOT EXISTS thumbnail_path VARCHAR(1000);
