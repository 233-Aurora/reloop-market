package top.sharpcaterpillar.campusecho.user.domain;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UserAccount> findByUsernameOrEmail(String username, String email);

    Optional<UserAccount> findByIdAndStatus(Long id, UserStatus status);

    boolean existsByAvatarUrl(String avatarUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user where user.id = :id and user.status = :status")
    Optional<UserAccount> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user where user.id = :id")
    Optional<UserAccount> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(UserStatus status);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from UserAccount user
            where user.role = :role and user.status = :status
            order by user.id
            """)
    List<UserAccount> findByRoleAndStatusForUpdate(
            @Param("role") UserRole role,
            @Param("status") UserStatus status);

    @Query("""
            select user from UserAccount user
            where (:keyword = ''
                   or lower(user.username) like lower(concat('%', :keyword, '%'))
                   or lower(user.email) like lower(concat('%', :keyword, '%'))
                   or lower(user.displayName) like lower(concat('%', :keyword, '%')))
              and (:role is null or user.role = :role)
              and (:status is null or user.status = :status)
            """)
    Page<UserAccount> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable);
}
