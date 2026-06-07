package com.corum.backend.dto.board;

import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardGroupPermission;
import lombok.Getter;

import java.util.List;

@Getter
public class BoardResponse {

    private final Long id;
    private final Long menuId;
    private final String name;
    private final String boardType;
    private final Boolean useComment;
    private final Boolean useLike;
    private final Boolean useAnonymous;
    private final Boolean useNotice;
    private final Integer noticeCountLimit;
    private final Integer fileMaxSizeMb;
    private final String fileAllowedExtensions;
    private final Integer fileMaxCount;
    private final Boolean isActive;
    private final List<BoardGroupPermission> permissions;

    public BoardResponse(Board board, List<BoardGroupPermission> permissions) {
        this.id = board.getId();
        this.menuId = board.getMenuId();
        this.name = board.getName();
        this.boardType = board.getBoardType();
        this.useComment = board.getUseComment();
        this.useLike = board.getUseLike();
        this.useAnonymous = board.getUseAnonymous();
        this.useNotice = board.getUseNotice();
        this.noticeCountLimit = board.getNoticeCountLimit();
        this.fileMaxSizeMb = board.getFileMaxSizeMb();
        this.fileAllowedExtensions = board.getFileAllowedExtensions();
        this.fileMaxCount = board.getFileMaxCount();
        this.isActive = board.getIsActive();
        this.permissions = permissions;
    }
}
