package com.minidoodle.repository;

import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId")
    Page<TimeSlot> findByCalendarId(@Param("calendarId") Long calendarId, Pageable pageable);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId AND ts.status = :status")
    Page<TimeSlot> findByCalendarIdAndStatus(
            @Param("calendarId") Long calendarId,
            @Param("status") SlotStatus status,
            Pageable pageable);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
            "AND ts.startTime >= :from AND ts.endTime <= :to")
    Page<TimeSlot> findByCalendarIdAndTimeRange(
            @Param("calendarId") Long calendarId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
            "AND ts.status = :status AND ts.startTime >= :from AND ts.endTime <= :to")
    Page<TimeSlot> findByCalendarIdAndStatusAndTimeRange(
            @Param("calendarId") Long calendarId,
            @Param("status") SlotStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT ts FROM TimeSlot ts LEFT JOIN FETCH ts.meeting WHERE ts.calendar.id = :calendarId " +
            "AND ts.startTime >= :from AND ts.endTime <= :to ORDER BY ts.startTime")
    List<TimeSlot> findAllByCalendarIdAndTimeRange(
            @Param("calendarId") Long calendarId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.calendar.id = :calendarId AND ts.id = :slotId")
    Optional<TimeSlot> findByIdAndCalendarId(
            @Param("slotId") Long slotId,
            @Param("calendarId") Long calendarId);

    /**
     * Check for overlapping slots in the same calendar.
     * Two slots overlap if one starts before the other ends and ends after the other starts.
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
            "AND ts.startTime < :endTime AND ts.endTime > :startTime AND ts.id <> :excludeId")
    boolean existsOverlappingSlot(
            @Param("calendarId") Long calendarId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(ts) > 0 FROM TimeSlot ts WHERE ts.calendar.id = :calendarId " +
            "AND ts.startTime < :endTime AND ts.endTime > :startTime")
    boolean existsOverlappingSlot(
            @Param("calendarId") Long calendarId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
