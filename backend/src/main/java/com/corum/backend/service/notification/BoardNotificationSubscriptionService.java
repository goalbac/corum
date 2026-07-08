package com.corum.backend.service.notification;

import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.notification.BoardNotificationSubscription;
import com.corum.backend.domain.notification.BoardNotificationSubscriptionRepository;
import com.corum.backend.dto.notification.BoardNotificationSubscriptionResponse;
import com.corum.backend.dto.notification.MyBoardSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardNotificationSubscriptionService {

    private final BoardNotificationSubscriptionRepository subscriptionRepository;
    private final BoardRepository boardRepository;

    @Transactional(readOnly = true)
    public BoardNotificationSubscriptionResponse getSubscription(Long memberId, Long boardId) {
        return subscriptionRepository.findByMemberIdAndBoardId(memberId, boardId)
                .map(s -> new BoardNotificationSubscriptionResponse(boardId, s.getNotifyNewPost(), s.getNotifyNewComment()))
                .orElseGet(() -> new BoardNotificationSubscriptionResponse(boardId, false, false));
    }

    @Transactional
    public BoardNotificationSubscriptionResponse updateSubscription(Long memberId, Long boardId,
                                                                      Boolean notifyNewPost, Boolean notifyNewComment) {
        boolean post = Boolean.TRUE.equals(notifyNewPost);
        boolean comment = Boolean.TRUE.equals(notifyNewComment);

        if (!post && !comment) {
            subscriptionRepository.deleteByMemberIdAndBoardId(memberId, boardId);
            return new BoardNotificationSubscriptionResponse(boardId, false, false);
        }

        BoardNotificationSubscription subscription = subscriptionRepository.findByMemberIdAndBoardId(memberId, boardId)
                .orElseGet(() -> BoardNotificationSubscription.builder().memberId(memberId).boardId(boardId).build());
        subscription.update(post, comment);
        subscriptionRepository.save(subscription);
        return new BoardNotificationSubscriptionResponse(boardId, post, comment);
    }

    @Transactional(readOnly = true)
    public List<MyBoardSubscriptionResponse> getMySubscriptions(Long memberId) {
        return subscriptionRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(s -> {
                    Board board = boardRepository.findById(s.getBoardId()).orElse(null);
                    return new MyBoardSubscriptionResponse(
                            s.getBoardId(),
                            board != null ? board.getName() : "(삭제된 게시판)",
                            board != null ? board.getMenuId() : null,
                            s.getNotifyNewPost(),
                            s.getNotifyNewComment()
                    );
                })
                .collect(Collectors.toList());
    }
}
