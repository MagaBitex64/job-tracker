package com.jobtracker.application.repository;

import com.jobtracker.application.entity.Application;
import com.jobtracker.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT a FROM Application a
        WHERE a.user.id = :userId
          AND (:status IS NULL OR a.status = :status)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(a.company) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.position) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Application> findAllByUserAndFilters(
            @Param("userId") Long userId,
            @Param("status") ApplicationStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT a.status AS status, COUNT(a) AS count
        FROM Application a
        WHERE a.user.id = :userId
        GROUP BY a.status
    """)
    List<StatusCountProjection> countApplicationsByStatusForUser(@Param("userId") Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
