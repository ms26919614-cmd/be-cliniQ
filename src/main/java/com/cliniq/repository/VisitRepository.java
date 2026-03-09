package com.cliniq.repository;

import com.cliniq.entity.Visit;
import com.cliniq.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
