package com.cliniq.repository;

import com.cliniq.entity.Visit;
import com.cliniq.enums.VisitStatus;
import com.cliniq.enums.VisitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT MAX(v.tokenNumber) FROM Visit v WHERE v.visitDate = :date")
    Optional<Integer> findMaxTokenNumberByDate(@Param("date") LocalDate date);

    boolean existsByPatientIdAndVisitDate(Long patientId, LocalDate visitDate);

    List<Visit> findByVisitDateOrderByTokenNumberAsc(LocalDate visitDate);

    List<Visit> findByVisitDateAndStatusOrderByTokenNumberAsc(LocalDate visitDate, VisitStatus status);

    Optional<Visit> findFirstByVisitDateAndStatusOrderByTokenNumberAsc(LocalDate visitDate, VisitStatus status);

    Optional<Visit> findByTokenNumberAndVisitDate(Integer tokenNumber, LocalDate visitDate);

    /**
     * Find the first eligible appointment visit: WAITING, APPOINTMENT type,
     * and appointment_time <= now (slot time is due or overdue).
     * Ordered by appointmentTime ASC so the earliest due slot gets called first.
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate = :date AND v.status = :status " +
           "AND v.visitType = :visitType AND v.appointmentTime <= :now " +
           "ORDER BY v.appointmentTime ASC, v.tokenNumber ASC")
    List<Visit> findEligibleAppointments(
            @Param("date") LocalDate date,
            @Param("status") VisitStatus status,
            @Param("visitType") VisitType visitType,
            @Param("now") LocalTime now);

    /**
     * Convenience: get just the first eligible appointment.
     */
    default Optional<Visit> findFirstEligibleAppointment(LocalDate date, VisitStatus status, LocalTime now) {
        List<Visit> eligible = findEligibleAppointments(date, status, VisitType.APPOINTMENT, now);
        return eligible.isEmpty() ? Optional.empty() : Optional.of(eligible.get(0));
    }

    /**
     * Find the first WAITING walk-in visit (ordered by token number).
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate = :date AND v.status = :status " +
           "AND v.visitType = :visitType ORDER BY v.tokenNumber ASC")
    List<Visit> findWalkInWaiting(
            @Param("date") LocalDate date,
            @Param("status") VisitStatus status,
            @Param("visitType") VisitType visitType);

    default Optional<Visit> findFirstWalkInWaiting(LocalDate date, VisitStatus status, VisitType visitType) {
        List<Visit> walkIns = findWalkInWaiting(date, status, visitType);
        return walkIns.isEmpty() ? Optional.empty() : Optional.of(walkIns.get(0));
    }
}
