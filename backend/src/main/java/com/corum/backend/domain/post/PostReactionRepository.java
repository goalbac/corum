package com.corum.backend.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    List<PostReaction> findByPostId(Long postId);

    boolean existsByPostIdAndMemberIdAndEmojiType(Long postId, Long memberId, String emojiType);

    void deleteByPostIdAndMemberIdAndEmojiType(Long postId, Long memberId, String emojiType);

    @Query("SELECT COUNT(r) FROM PostReaction r WHERE r.postId = :postId")
    int countByPostId(@Param("postId") Long postId);

    List<PostReaction> findByMemberIdAndPostId(Long memberId, Long postId);
}
