-- post_reactions: post_likes 대체 (이모지 리액션)
CREATE TABLE IF NOT EXISTS post_reactions (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT       NOT NULL,
    member_id  BIGINT       NOT NULL,
    emoji_type VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_post_reactions_unique  ON post_reactions(post_id, member_id, emoji_type);
CREATE INDEX        IF NOT EXISTS idx_post_reactions_post_id ON post_reactions(post_id);

-- 기존 post_likes 데이터 → HEART 리액션으로 마이그레이션
INSERT INTO post_reactions (post_id, member_id, emoji_type, created_at)
SELECT post_id, member_id, 'HEART', created_at FROM post_likes
ON CONFLICT DO NOTHING;

-- comment_reactions
CREATE TABLE IF NOT EXISTS comment_reactions (
    id         BIGSERIAL PRIMARY KEY,
    comment_id BIGINT       NOT NULL,
    member_id  BIGINT       NOT NULL,
    emoji_type VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_comment_reactions_unique     ON comment_reactions(comment_id, member_id, emoji_type);
CREATE INDEX        IF NOT EXISTS idx_comment_reactions_comment_id ON comment_reactions(comment_id);
