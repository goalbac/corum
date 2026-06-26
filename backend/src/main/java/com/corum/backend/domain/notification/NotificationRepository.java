package com.corum.backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByMemberIdAndIsReadFalse(Long memberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.memberId = :memberId AND n.isRead = false")
    int markAllReadByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    int deleteReadOlderThan(@Param("cutoff") java.time.LocalDateTime cutoff);
}
