package com.corum.backend.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    void deleteByPostIdAndMemberId(Long postId, Long memberId);
}
