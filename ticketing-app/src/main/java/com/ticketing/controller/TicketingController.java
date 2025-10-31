package com.ticketing.controller;

import com.ticketing.dto.ApiResponse;
import com.ticketing.dto.ReservationRequest;
import com.ticketing.dto.ReservationResponse;
import com.ticketing.service.TicketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class TicketingController {

    private final TicketingService ticketingService;

    /**
     * 티켓 예매
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveTicket(
            @Valid @RequestBody ReservationRequest request) {
        try {
            ReservationResponse response = ticketingService.reserveTicket(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("예매가 완료되었습니다", response));
        } catch (Exception e) {
            log.error("Reservation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 사용자 예매 내역 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getUserReservations(
            @PathVariable String userId) {
        List<ReservationResponse> reservations = ticketingService.getUserReservations(userId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    /**
     * 예매 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(
            @PathVariable Long id) {
        try {
            ReservationResponse response = ticketingService.getReservation(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 예매 취소
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        try {
            ticketingService.cancelReservation(id);
            return ResponseEntity.ok(ApiResponse.success("예매가 취소되었습니다", null));
        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
