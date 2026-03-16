package com.cliniq.service;

import com.cliniq.dto.QueueDisplayResponse;
import com.cliniq.dto.VisitResponse;
import com.cliniq.entity.Visit;
import com.cliniq.enums.VisitStatus;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Transactional
    public VisitResponse callNextPatient() {
        LocalDate today = LocalDate.now();
        log.info("Calling next patient for date: {}", today);

        // Check if there's already a patient IN_PROGRESS
        List<Visit> inProgress = visitRepository
                .findByVisitDateAndStatusOrderByTokenNumberAsc(today, VisitStatus.IN_PROGRESS);
        if (!inProgress.isEmpty()) {
            log.warn("There is already a patient in progress: Token {}", inProgress.get(0).getTokenNumber());
            throw new IllegalStateException(
                    "There is already a patient in progress (Token " + inProgress.get(0).getTokenNumber()
                            + "). Complete or mark as no-show before calling next.");
        }

        // Find next WAITING patient
        Visit nextVisit = visitRepository
                .findFirstByVisitDateAndStatusOrderByTokenNumberAsc(today, VisitStatus.WAITING)
                .orElseThrow(() -> {
                    log.info("No waiting patients in queue for today");
                    return new ResourceNotFoundException("No waiting patients in the queue");
                });

        nextVisit.setStatus(VisitStatus.IN_PROGRESS);
        nextVisit.setCalledAt(LocalDateTime.now());
        nextVisit = visitRepository.save(nextVisit);

        log.info("Next patient called: Token {} - {}", nextVisit.getTokenNumber(),
                nextVisit.getPatient().getName());

        return mapToVisitResponse(nextVisit);
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
        Integer currentToken = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.IN_PROGRESS)
                .map(Visit::getTokenNumber)
                .findFirst()
                .orElse(null);

        // Waiting tokens
        List<QueueDisplayResponse.TokenDisplay> waitingTokens = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.WAITING)
                .map(v -> QueueDisplayResponse.TokenDisplay.builder()
                        .tokenNumber(v.getTokenNumber())
                        .status(v.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        // Completed tokens
        List<QueueDisplayResponse.TokenDisplay> completedTokens = allVisits.stream()
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED || v.getStatus() == VisitStatus.NO_SHOW)
                .map(v -> QueueDisplayResponse.TokenDisplay.builder()
                        .tokenNumber(v.getTokenNumber())
                        .status(v.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        log.debug("Queue display - Current: {}, Waiting: {}, Completed: {}",
                currentToken, waitingTokens.size(), completedTokens.size());

        return QueueDisplayResponse.builder()
                .date(today)
                .currentToken(currentToken)
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
                .createdAt(visit.getCreatedAt())
                .calledAt(visit.getCalledAt())
                .completedAt(visit.getCompletedAt())
                .build();
    }
}
