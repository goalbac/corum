package com.corum.backend.migration;

/** 구 홈페이지 게시판(테이블 쌍) -> Corum 게시판 매핑 1건 */
public record BoardMigrationSpec(
        String legacyPostTable,
        String legacyCommentTable,
        Long targetBoardId
) {
}
