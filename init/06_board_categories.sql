-- board_categories: 게시판별 카테고리
CREATE TABLE IF NOT EXISTS board_categories (
    id        BIGSERIAL PRIMARY KEY,
    board_id  BIGINT       NOT NULL,
    name      VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_board_categories_board_id ON board_categories(board_id);

-- posts 카테고리 참조
ALTER TABLE posts ADD COLUMN IF NOT EXISTS category_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_posts_category_id ON posts(category_id);

-- boards 전체 카테고리 사용 여부
ALTER TABLE boards ADD COLUMN IF NOT EXISTS use_all_category BOOLEAN NOT NULL DEFAULT false;
