package com.ticketing.service;

import com.ticketing.dto.EventRequest;
import com.ticketing.dto.EventResponse;
import com.ticketing.model.Event;
import com.ticketing.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    /**
     * 모든 이벤트 조회 (캐시 적용)
     */
    @Cacheable(value = "events", key = "'all'")
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        log.info("Fetching all events from database (Cache Miss)");
        return eventRepository.findAll().stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 상세 조회 (캐시 적용)
     */
    @Cacheable(value = "events", key = "#eventId")
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        log.info("Fetching event from database: eventId={} (Cache Miss)", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));

        return EventResponse.from(event);
    }

    /**
     * 예정된 이벤트 조회
     */
    @Cacheable(value = "events", key = "'upcoming'")
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        log.info("Fetching upcoming events from database (Cache Miss)");
        return eventRepository.findUpcomingEvents(LocalDateTime.now()).stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 예매 가능한 이벤트 조회
     */
    @Cacheable(value = "events", key = "'available'")
    @Transactional(readOnly = true)
    public List<EventResponse> getAvailableEvents() {
        log.info("Fetching available events from database (Cache Miss)");
        return eventRepository.findAvailableEvents().stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 생성 (캐시 전체 무효화)
     */
    @Caching(evict = {
            @CacheEvict(value = "events", key = "'all'"),
            @CacheEvict(value = "events", key = "'upcoming'"),
            @CacheEvict(value = "events", key = "'available'")
    })
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = Event.builder()
                .eventName(request.getEventName())
                .totalSeats(request.getTotalSeats())
                .eventDate(request.getEventDate())
                .build();

        Event savedEvent = eventRepository.save(event);

        log.info("Event created: id={}, name={}", savedEvent.getId(), savedEvent.getEventName());

        return EventResponse.from(savedEvent);
    }

    /**
     * 이벤트 수정 (해당 이벤트 캐시 무효화)
     */
    @Caching(evict = {
            @CacheEvict(value = "events", key = "#eventId"),
            @CacheEvict(value = "events", key = "'all'"),
            @CacheEvict(value = "events", key = "'upcoming'"),
            @CacheEvict(value = "events", key = "'available'")
    })
    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));

        event.setEventName(request.getEventName());
        event.setTotalSeats(request.getTotalSeats());
        event.setEventDate(request.getEventDate());

        Event updatedEvent = eventRepository.save(event);

        log.info("Event updated: id={}, name={}", updatedEvent.getId(), updatedEvent.getEventName());

        return EventResponse.from(updatedEvent);
    }

    /**
     * 전체 캐시 무효화
     */
    @CacheEvict(value = "events", allEntries = true)
    public void clearAllCache() {
        log.info("All event cache cleared");
    }
}
