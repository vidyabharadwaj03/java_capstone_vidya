package assembly.general.api.repository;

import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUserIdAndStatusInOrderByReservedAtDesc(UUID userId, List<ReservationStatus> statuses);

    long countByUserIdAndStatusIn(UUID userId, List<ReservationStatus> statuses);

    long countByUserIdAndStatus(UUID userId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId ORDER BY COALESCE(r.returnedAt, r.reservedAt) DESC")
    Page<Reservation> findHistoryByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, java.time.Instant instant);
}
