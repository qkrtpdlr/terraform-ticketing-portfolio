package com.ticketing.service;

import com.ticketing.dto.ReservationRequest;
import com.ticketing.dto.ReservationResponse;
import com.ticketing.model.Event;
import com.ticketing.model.Reservation;
import com.ticketing.repository.EventRepository;
import com.ticketing.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketingService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final CacheManager cacheManager;

    /**
     * 티켓 예매 (Redis 분산 락 사용)
     */
    @Transactional
    public ReservationResponse reserveTicket(ReservationRequest request) {
        String lockKey = "lock:event:" + request.getEventId();
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new RuntimeException("다른 사용자가 예매 진행 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            log.info("Lock acquired for event: {}", request.getEventId());

            // 1. 이벤트 조회
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));

            // 2. 잔여 좌석 확인
            if (event.getAvailableSeats() < request.getQuantity()) {
                throw new RuntimeException("좌석이 부족합니다");
            }

            // 3. 좌석 차감
            event.decreaseSeats(request.getQuantity());
            eventRepository.save(event);

            // 4. 예매 기록 저장
            Reservation reservation = Reservation.builder()
                    .eventId(request.getEventId())
                    .userId(request.getUserId())
                    .quantity(request.getQuantity())
                    .build();

            reservationRepository.save(reservation);

            // 5. 캐시 무효화
            if (cacheManager.getCache("events") != null) {
                cacheManager.getCache("events").evict(event.getId());
            }

            log.info("Reservation completed: eventId={}, userId={}, quantity={}",
                    request.getEventId(), request.getUserId(), request.getQuantity());

            return ReservationResponse.success(reservation);

        } finally {
            // 6. 락 해제 (항상 실행)
            redisTemplate.delete(lockKey);
            log.info("Lock released for event: {}", request.getEventId());
        }
    }

    /**
     * 예매 취소
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예매 내역을 찾을 수 없습니다"));

        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new RuntimeException("이미 취소된 예매입니다");
        }

        // 1. 예매 취소
        reservation.cancel();
        reservationRepository.save(reservation);

        // 2. 좌석 복구
        Event event = eventRepository.findById(reservation.getEventId())
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));

        event.increaseSeats(reservation.getQuantity());
        eventRepository.save(event);

        // 3. 캐시 무효화
        if (cacheManager.getCache("events") != null) {
            cacheManager.getCache("events").evict(event.getId());
        }

        log.info("Reservation cancelled: id={}, eventId={}, quantity={}",
                reservationId, reservation.getEventId(), reservation.getQuantity());
    }

    /**
     * 사용자 예매 내역 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(String userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 예매 상세 조회
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예매 내역을 찾을 수 없습니다"));

        return ReservationResponse.from(reservation);
    }
}
