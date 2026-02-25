package com.minidoodle.repository;

import com.minidoodle.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Query(value = "SELECT DISTINCT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.organizer.id = :userId OR p.id = :userId",
            countQuery = "SELECT COUNT(DISTINCT m) FROM Meeting m LEFT JOIN m.participants p " +
                    "WHERE m.organizer.id = :userId OR p.id = :userId")
    Page<Meeting> findByOrganizerOrParticipant(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM Meeting m JOIN FETCH m.timeSlot ts JOIN FETCH m.organizer " +
            "LEFT JOIN FETCH m.participants WHERE m.id = :meetingId AND m.organizer.id = :userId")
    Optional<Meeting> findByIdAndOrganizerId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId);

    @Query("SELECT m FROM Meeting m JOIN FETCH m.timeSlot ts JOIN FETCH m.organizer " +
            "LEFT JOIN FETCH m.participants " +
            "WHERE m.id = :meetingId AND (m.organizer.id = :userId OR EXISTS (" +
            "SELECT p FROM m.participants p WHERE p.id = :userId))")
    Optional<Meeting> findByIdForUser(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId);

    @Query("SELECT m FROM Meeting m WHERE m.timeSlot.id = :slotId")
    Optional<Meeting> findByTimeSlotId(@Param("slotId") Long slotId);
}
