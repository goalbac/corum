package com.corum.backend.domain.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    List<CommentReaction> findByCommentId(Long commentId);

    List<CommentReaction> findByCommentIdIn(Collection<Long> commentIds);

    boolean existsByCommentIdAndMemberIdAndEmojiType(Long commentId, Long memberId, String emojiType);

    void deleteByCommentIdAndMemberIdAndEmojiType(Long commentId, Long memberId, String emojiType);

    List<CommentReaction> findByMemberIdAndCommentId(Long memberId, Long commentId);
}
