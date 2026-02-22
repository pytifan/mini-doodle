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

    @Query("SELECT m FROM Meeting m JOIN m.timeSlot ts JOIN ts.calendar c " +
            "WHERE c.user.id = :userId ORDER BY ts.startTime DESC")
    Page<Meeting> findByOrganizerId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM Meeting m JOIN FETCH m.timeSlot ts JOIN FETCH m.organizer " +
            "LEFT JOIN FETCH m.participants WHERE m.id = :meetingId AND m.organizer.id = :userId")
    Optional<Meeting> findByIdAndOrganizerId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId);

    @Query("SELECT m FROM Meeting m WHERE m.timeSlot.id = :slotId")
    Optional<Meeting> findByTimeSlotId(@Param("slotId") Long slotId);
}
