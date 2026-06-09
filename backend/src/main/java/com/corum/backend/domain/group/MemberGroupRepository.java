package com.corum.backend.domain.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {

    List<MemberGroup> findByMemberId(Long memberId);

    List<MemberGroup> findByMemberIdIn(List<Long> memberIds);

    List<MemberGroup> findByGroupId(Long groupId);

    List<MemberGroup> findByGroupIdIn(List<Long> groupIds);

    @Query("SELECT DISTINCT mg.memberId FROM MemberGroup mg JOIN Group g ON g.id = mg.groupId WHERE g.type = 'ADMIN'")
    List<Long> findAdminMemberIds();

    boolean existsByMemberIdAndGroupId(Long memberId, Long groupId);

    void deleteByMemberIdAndGroupId(Long memberId, Long groupId);

    // 회원의 그룹 ID 목록
    @Query("SELECT mg.groupId FROM MemberGroup mg WHERE mg.memberId = :memberId")
    List<Long> findGroupIdsByMemberId(Long memberId);

    // 회원이 운영 그룹 계열인지 확인
    @Query("""
        SELECT COUNT(mg) > 0 FROM MemberGroup mg
        JOIN Group g ON g.id = mg.groupId
        WHERE mg.memberId = :memberId AND g.type = 'ADMIN'
        """)
    boolean existsAdminGroupByMemberId(Long memberId);
}
