package com.cliniq.repository;

import com.cliniq.entity.Appointment;
import com.cliniq.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByAppointmentDateOrderByStartTimeAsc(LocalDate date);

    List<Appointment> findByAppointmentDateAndStatusOrderByStartTimeAsc(LocalDate date, AppointmentStatus status);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    Optional<Appointment> findByPatientIdAndAppointmentDate(Long patientId, LocalDate date);

    boolean existsByPatientIdAndAppointmentDateAndStatusNot(Long patientId, LocalDate date, AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.slot.id = :slotId AND a.appointmentDate = :date AND a.status <> 'CANCELLED'")
    long countActiveBookingsForSlot(@Param("slotId") Long slotId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.status = :status ORDER BY a.startTime ASC")
    List<Appointment> findByDateAndStatus(@Param("date") LocalDate date, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :startDate AND :endDate ORDER BY a.appointmentDate ASC, a.startTime ASC")
    List<Appointment> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
