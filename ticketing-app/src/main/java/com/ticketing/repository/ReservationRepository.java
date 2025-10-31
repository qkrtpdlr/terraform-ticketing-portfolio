package com.ticketing.repository;

import com.ticketing.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(String userId);

    List<Reservation> findByEventId(Long eventId);

    List<Reservation> findByUserIdAndStatus(String userId, Reservation.ReservationStatus status);
}
