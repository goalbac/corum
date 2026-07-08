package com.corum.backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardNotificationSubscriptionRepository extends JpaRepository<BoardNotificationSubscription, Long> {

    Optional<BoardNotificationSubscription> findByMemberIdAndBoardId(Long memberId, Long boardId);

    List<BoardNotificationSubscription> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    void deleteByMemberIdAndBoardId(Long memberId, Long boardId);

    @Query("SELECT s.memberId FROM BoardNotificationSubscription s WHERE s.boardId = :boardId AND s.notifyNewPost = true")
    List<Long> findMemberIdsByBoardIdAndNotifyNewPostTrue(Long boardId);

    @Query("SELECT s.memberId FROM BoardNotificationSubscription s WHERE s.boardId = :boardId AND s.notifyNewComment = true")
    List<Long> findMemberIdsByBoardIdAndNotifyNewCommentTrue(Long boardId);
}
