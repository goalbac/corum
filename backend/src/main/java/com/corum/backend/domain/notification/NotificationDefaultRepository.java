package com.corum.backend.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDefaultRepository extends JpaRepository<NotificationDefault, String> {

    List<NotificationDefault> findAllByOrderByNotifTypeAsc();
}
