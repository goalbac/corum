package com.corum.backend.dto.board;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class BoardCreateRequest {

    private Long menuId;

    @NotBlank(message = "게시판 이름을 입력해주세요.")
    private String name;

    private String boardType = "POST";
    private Boolean useComment = true;
    private Boolean useLike = true;
    private Boolean useAnonymous = false;
    private Boolean useNotice = true;
    private Integer noticeCountLimit = 5;
    private Integer fileMaxSizeMb;
    private String fileAllowedExtensions;
    private Integer fileMaxCount = 5;
    private Boolean isActive = true;
    private Boolean useAllCategory = false;
    private Boolean useAliasWriter = false;
    private List<BoardPermissionRequest> permissions;
    private List<CategoryRequest> categories;
    private List<IdentityRequest> writerIdentities;

    @Getter
    public static class CategoryRequest {
        private Long id;
        private String name;
        private Integer sortOrder;
    }

    @Getter
    public static class IdentityRequest {
        private Long id;
        private String name;
        private Integer sortOrder;
    }

    @Getter
    public static class BoardPermissionRequest {
        private Long groupId;
        private Boolean canRead = true;
        private Boolean canWrite = false;
        private Boolean canComment = false;
        private Boolean canDownload = true;
        private Boolean canManage = false;
    }
}
