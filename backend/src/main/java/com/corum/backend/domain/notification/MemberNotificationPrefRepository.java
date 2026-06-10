package com.corum.backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberNotificationPrefRepository extends JpaRepository<MemberNotificationPref, Long> {

    List<MemberNotificationPref> findByMemberId(Long memberId);

    Optional<MemberNotificationPref> findByMemberIdAndNotifType(Long memberId, String notifType);

    @Query("SELECT p FROM MemberNotificationPref p WHERE p.memberId = :memberId AND p.notifType = :notifType")
    Optional<MemberNotificationPref> findPref(Long memberId, String notifType);
}
