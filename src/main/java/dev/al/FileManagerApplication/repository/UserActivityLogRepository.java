package dev.al.FileManagerApplication.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import dev.al.FileManagerApplication.model.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {


    Page<UserActivityLog> findByEventTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<UserActivityLog> findByEventType(String eventType, Pageable pageable);

    Page<UserActivityLog> findByUserIdAndEventTimeBetween(String userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}