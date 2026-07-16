package top.sharpcaterpillar.campusecho.message.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteMessageRepository extends JpaRepository<SiteMessage, Long> {

    @Query("""
            select message from SiteMessage message
            where message.recipientId = :recipientId
              and (:unreadOnly = false or message.read = false)
              and (:messageType is null or message.messageType = :messageType)
            order by message.createdAt desc
            """)
    Page<SiteMessage> searchMine(
            @Param("recipientId") Long recipientId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("messageType") MessageType messageType,
            Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<SiteMessage> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SiteMessage message
            set message.read = true
            where message.recipientId = :recipientId and message.read = false
            """)
    int markAllRead(@Param("recipientId") Long recipientId);
}
