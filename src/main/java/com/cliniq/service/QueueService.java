package com.cliniq.service;

import com.cliniq.dto.QueueDisplayResponse;
import com.cliniq.dto.VisitResponse;
import com.cliniq.entity.Visit;
import com.cliniq.enums.VisitStatus;
import com.cliniq.enums.VisitType;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final VisitRepository visitRepository;

    @Transactional
    public VisitResponse updateVisitStatus(Long visitId, String newStatusStr) {
        log.info("Updating visit {} status to: {}", visitId, newStatusStr);

        VisitStatus newStatus = VisitStatus.valueOf(newStatusStr);
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found with id: " + visitId));

        validateStatusTransition(visit.getStatus(), newStatus);

        visit.setStatus(newStatus);

        if (newStatus == VisitStatus.IN_PROGRESS) {
            visit.setCalledAt(LocalDateTime.now());
            log.info("Token {} called at {}", visit.getTokenNumber(), visit.getCalledAt());
        } else if (newStatus == VisitStatus.COMPLETED) {
            visit.setCompletedAt(LocalDateTime.now());
            log.info("Token {} completed at {}", visit.getTokenNumber(), visit.getCompletedAt());
        } else if (newStatus == VisitStatus.NO_SHOW) {
            visit.setCompletedAt(LocalDateTime.now());
            log.info("Token {} marked as NO_SHOW at {}", visit.getTokenNumber(), visit.getCompletedAt());
        }

        visit = visitRepository.save(visit);
        log.info("Visit {} status updated to: {}", visitId, newStatus);

        return mapToVisitResponse(visit);
    }

    /**
     * Queue merge logic (US8):
     *
     * Priority order when calling next patient:
     * 1. APPOINTMENT visits whose appointmentTime <= current time (slot is due or overdue),
     *    ordered by appointmentTime ASC (earliest slot first).
     *    These get TOP priority because their booked time has arrived.
     *
     * 2. ALL remaining WAITING visits (walk-ins AND early check-in appointments
     *    whose slot time hasn't arrived yet), ordered by tokenNumber ASC.
     *    Early check-in appointments are treated fairly — they queue alongside
     *    walk-ins by arrival order instead of sitting idle.
     *
     * Scenarios:
     * - 10:00 appointment checked in at 10:05 → Step 1 picks them (slot overdue)
     * - 10:30 appointment checked in at 9:00 → Step 2 treats them as token #N
     *   alongside walk-ins. If queue moves fast, they may be seen before 10:30.
     *   If still waiting at 10:30, Step 1 picks them with top priority.
     * - Walk-ins always go by token order in Step 2
     */
    @Transactional
    public VisitResponse callNextPatient() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        log.info("Calling next patient for date: {}, time: {}", today, now);

        // Check if there's already a patient IN_PROGRESS
        List<Visit> inProgress = visitRepository
                .findByVisitDateAndStatusOrderByTokenNumberAsc(today, VisitStatus.IN_PROGRESS);
        if (!inProgress.isEmpty()) {
            log.warn("There is already a patient in progress: Token {}", inProgress.get(0).getTokenNumber());
            throw new IllegalStateException(
                    "There is already a patient in progress (Token " + inProgress.get(0).getTokenNumber()
                            + "). Complete or mark as no-show before calling next.");
        }

        // Step 1: Look for WAITING appointment visits whose slot time is due (<= now)
        Optional<Visit> nextAppointment = visitRepository
                .findFirstEligibleAppointment(today, VisitStatus.WAITING, now);
        log.info("Checking eligible appointments at time {}", now);

        if (nextAppointment.isPresent()) {
            Visit next = nextAppointment.get();
            next.setStatus(VisitStatus.IN_PROGRESS);
            next.setCalledAt(LocalDateTime.now());
            next = visitRepository.save(next);
            log.info("Appointment patient called (slot due): Token {} (slot {}), Patient: {}",
                    next.getTokenNumber(), next.getAppointmentTime(), next.getPatient().getName());
            return mapToVisitResponse(next);
        }

        // Step 2: No due appointments — call next WAITING patient by token order.
        // This includes BOTH walk-ins AND early check-in appointments (whose
        // slot time hasn't arrived yet). Fair first-come-first-served ordering.
        Visit nextPatient = visitRepository
                .findFirstByVisitDateAndStatusOrderByTokenNumberAsc(today, VisitStatus.WAITING)
                .orElseThrow(() -> {
                    log.info("No waiting patients in queue for today");
                    return new ResourceNotFoundException("No waiting patients in the queue");
                });

        nextPatient.setStatus(VisitStatus.IN_PROGRESS);
        nextPatient.setCalledAt(LocalDateTime.now());
        nextPatient = visitRepository.save(nextPatient);

        log.info("Patient called (token order): Token {} [{}] - {}",
                nextPatient.getTokenNumber(), nextPatient.getVisitType(), nextPatient.getPatient().getName());

        return mapToVisitResponse(nextPatient);
    }

    public List<VisitResponse> getTodayQueue() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching today's queue for date: {}", today);

        List<Visit> visits = visitRepository.findByVisitDateOrderByTokenNumberAsc(today);
        log.debug("Found {} visits for today", visits.size());

        return visits.stream()
                .map(this::mapToVisitResponse)
                .collect(Collectors.toList());
    }

    public QueueDisplayResponse getQueueDisplay() {
        LocalDate today = LocalDate.now();
        log.debug("Fetching queue display for date: {}", today);

        List<Visit> allVisits = visitRepository.findByVisitDateOrderByTokenNumberAsc(today);

        // Find current token (IN_PROGRESS)
        Visit currentVisit = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
        Integer currentToken = currentVisit != null ? currentVisit.getTokenNumber() : null;
        String currentPatientName = currentVisit != null ? currentVisit.getPatient().getName() : null;
        String currentVisitType = currentVisit != null ? currentVisit.getVisitType().name() : null;
        String currentAppointmentTime = (currentVisit != null && currentVisit.getAppointmentTime() != null)
                ? currentVisit.getAppointmentTime().toString() : null;

        // Waiting tokens
        List<QueueDisplayResponse.TokenDisplay> waitingTokens = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.WAITING)
                .map(v -> QueueDisplayResponse.TokenDisplay.builder()
                        .tokenNumber(v.getTokenNumber())
                        .status(v.getStatus().name())
                        .visitType(v.getVisitType().name())
                        .patientName(v.getPatient().getName())
                        .appointmentTime(v.getAppointmentTime() != null ? v.getAppointmentTime().toString() : null)
                        .build())
                .collect(Collectors.toList());

        // Completed tokens
        List<QueueDisplayResponse.TokenDisplay> completedTokens = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED || v.getStatus() == VisitStatus.NO_SHOW)
                .map(v -> QueueDisplayResponse.TokenDisplay.builder()
                        .tokenNumber(v.getTokenNumber())
                        .status(v.getStatus().name())
                        .visitType(v.getVisitType().name())
                        .patientName(v.getPatient().getName())
                        .appointmentTime(v.getAppointmentTime() != null ? v.getAppointmentTime().toString() : null)
                        .build())
                .collect(Collectors.toList());

        log.debug("Queue display - Current: {}, Waiting: {}, Completed: {}",
                currentToken, waitingTokens.size(), completedTokens.size());

        return QueueDisplayResponse.builder()
                .date(today)
                .currentToken(currentToken)
                .currentPatientName(currentPatientName)
                .currentVisitType(currentVisitType)
                .currentAppointmentTime(currentAppointmentTime)
                .totalTokens(allVisits.size())
                .waitingTokens(waitingTokens)
                .completedTokens(completedTokens)
                .build();
    }

    private void validateStatusTransition(VisitStatus current, VisitStatus next) {
        boolean valid = switch (current) {
            case WAITING -> next == VisitStatus.IN_PROGRESS || next == VisitStatus.NO_SHOW;
            case IN_PROGRESS -> next == VisitStatus.COMPLETED || next == VisitStatus.NO_SHOW;
            case COMPLETED, NO_SHOW -> false;
        };

        if (!valid) {
            log.warn("Invalid status transition: {} -> {}", current, next);
            throw new IllegalStateException(
                    "Invalid status transition from " + current + " to " + next);
        }
    }

    private VisitResponse mapToVisitResponse(Visit visit) {
        return VisitResponse.builder()
                .id(visit.getId())
                .tokenNumber(visit.getTokenNumber())
                .patientName(visit.getPatient().getName())
                .phone(visit.getPatient().getPhone())
                .visitDate(visit.getVisitDate())
                .status(visit.getStatus().name())
                .visitType(visit.getVisitType().name())
                .appointmentTime(visit.getAppointmentTime() != null ? visit.getAppointmentTime().toString() : null)
                .createdAt(visit.getCreatedAt())
                .calledAt(visit.getCalledAt())
                .completedAt(visit.getCompletedAt())
                .build();
    }
}
