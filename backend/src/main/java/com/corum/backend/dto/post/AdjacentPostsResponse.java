package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import lombok.Getter;

@Getter
public class AdjacentPostsResponse {

    private final PostRef prev;
    private final PostRef next;

    public AdjacentPostsResponse(Post prev, Post next) {
        this.prev = prev != null ? new PostRef(prev.getId(), prev.getTitle()) : null;
        this.next = next != null ? new PostRef(next.getId(), next.getTitle()) : null;
    }

    @Getter
    public static class PostRef {
        private final Long id;
        private final String title;

        public PostRef(Long id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}
