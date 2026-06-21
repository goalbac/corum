package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostSummaryResponse {

    private final Long id;
    private final Long boardId;
    private final String title;
    private final String writerName;
    private final String excerpt;
    private final Boolean isNotice;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final int commentCount;
    private final boolean hasFile;
    private final String thumbnailUrl;
    private final List<String> imageUrls;
    private final Long rowNum;
    private final Long categoryId;
    private final String categoryName;
    private final String writerProfileImageUrl;
    // 자료실(DOCUMENT) 전용 필드
    private final String primaryFileName;
    private final Long primaryFileSize;
    private final Integer primaryFileDownloadCount;
    private final int fileCount;

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile) {
        this(post, commentCount, hasFile, null, List.of(), null, null, null, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile, String thumbnailUrl) {
        this(post, commentCount, hasFile, thumbnailUrl, List.of(), null, null, null, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile, String thumbnailUrl, Long rowNum) {
        this(post, commentCount, hasFile, thumbnailUrl, List.of(), rowNum, null, null, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile,
                               String thumbnailUrl, List<String> imageUrls, Long rowNum) {
        this(post, commentCount, hasFile, thumbnailUrl, imageUrls, rowNum, null, null, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile,
                               String thumbnailUrl, List<String> imageUrls, Long rowNum, String categoryName) {
        this(post, commentCount, hasFile, thumbnailUrl, imageUrls, rowNum, categoryName, null, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile,
                               String thumbnailUrl, List<String> imageUrls, Long rowNum,
                               String categoryName, String writerProfileImageUrl) {
        this(post, commentCount, hasFile, thumbnailUrl, imageUrls, rowNum, categoryName, writerProfileImageUrl, null, null, null, 0);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile,
                               String thumbnailUrl, List<String> imageUrls, Long rowNum,
                               String categoryName, String writerProfileImageUrl,
                               String primaryFileName, Long primaryFileSize,
                               Integer primaryFileDownloadCount, int fileCount) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.title = post.getTitle();
        this.writerName = post.getWriterName();
        this.excerpt = createExcerpt(post.getContent());
        this.isNotice = post.getIsNotice();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.createdAt = post.getCreatedAt();
        this.commentCount = commentCount;
        this.hasFile = hasFile;
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrls = imageUrls != null ? imageUrls : List.of();
        this.rowNum = rowNum;
        this.categoryId = post.getCategoryId();
        this.categoryName = categoryName;
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.primaryFileName = primaryFileName;
        this.primaryFileSize = primaryFileSize;
        this.primaryFileDownloadCount = primaryFileDownloadCount;
        this.fileCount = fileCount;
    }

    private static String createExcerpt(String content) {
        if (content == null || content.isBlank()) return "";
        String text = content
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= 160) return text;
        return text.substring(0, 160).trim() + "...";
    }
}
