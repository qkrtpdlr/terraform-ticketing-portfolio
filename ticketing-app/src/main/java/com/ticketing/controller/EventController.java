package com.ticketing.controller;

import com.ticketing.dto.ApiResponse;
import com.ticketing.dto.EventRequest;
import com.ticketing.dto.EventResponse;
import com.ticketing.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * 모든 이벤트 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 이벤트 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        try {
            EventResponse event = eventService.getEvent(id);
            return ResponseEntity.ok(ApiResponse.success(event));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 예정된 이벤트 조회
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents() {
        List<EventResponse> events = eventService.getUpcomingEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 예매 가능한 이벤트 조회
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAvailableEvents() {
        List<EventResponse> events = eventService.getAvailableEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 이벤트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request) {
        EventResponse event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이벤트가 생성되었습니다", event));
    }

    /**
     * 이벤트 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        try {
            EventResponse event = eventService.updateEvent(id, request);
            return ResponseEntity.ok(ApiResponse.success("이벤트가 수정되었습니다", event));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
