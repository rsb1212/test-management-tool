package com.testmgmt.repository;

import com.testmgmt.entity.Defect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ─── DefectRepository ────────────────────────────────────────
@Repository
public interface DefectRepository extends JpaRepository<Defect, UUID> {
    Optional<Defect> findByCode(String code);

    @Query("""
                SELECT d FROM Defect d
                WHERE d.project.id = :projectId
                  AND (:severity    IS NULL OR d.severity = :severity)
                  AND (:status      IS NULL OR d.status IN :statuses)
                  AND (:buildVersion IS NULL OR d.buildVersion = :buildVersion)
            """)
    Page<Defect> findByFilters(
            @Param("projectId") UUID projectId,
            @Param("severity") Defect.Severity severity,
            @Param("statuses") List<Defect.DefectStatus> statuses,
            @Param("buildVersion") String buildVersion,
            Pageable pageable
    );

    @Query("""
                SELECT d.severity AS severity, COUNT(d) AS count
                FROM Defect d WHERE d.project.id = :projectId
                GROUP BY d.severity
            """)
    List<Object[]> countBySeverity(@Param("projectId") UUID projectId);

    @Query("""
                SELECT d.module AS module, COUNT(d) AS count
                FROM Defect d WHERE d.project.id = :projectId
                GROUP BY d.module
            """)
    List<Object[]> countByModule(@Param("projectId") UUID projectId);

    @Query("""
                SELECT FUNCTION('date_trunc', :groupBy, d.createdAt) AS period,
                       COUNT(d) AS raised,
                       SUM(CASE WHEN d.status = 'CLOSED' THEN 1 ELSE 0 END) AS closed
                FROM Defect d
                WHERE d.project.id = :projectId
                  AND d.createdAt BETWEEN :from AND :to
                GROUP BY FUNCTION('date_trunc', :groupBy, d.createdAt)
                ORDER BY period
            """)
    List<Object[]> defectTrends(
            @Param("projectId") UUID projectId,
            @Param("groupBy") String groupBy,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
