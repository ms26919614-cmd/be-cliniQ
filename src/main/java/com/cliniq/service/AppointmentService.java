package com.cliniq.service;

import com.cliniq.dto.AppointmentBookingRequest;
import com.cliniq.dto.AppointmentRescheduleRequest;
import com.cliniq.dto.AppointmentResponse;
import com.cliniq.entity.Appointment;
import com.cliniq.entity.AppointmentSlot;
import com.cliniq.entity.Patient;
import com.cliniq.entity.Visit;
import com.cliniq.enums.AppointmentStatus;
import com.cliniq.enums.DayOfWeekEnum;
import com.cliniq.enums.VisitStatus;
import com.cliniq.enums.VisitType;
import com.cliniq.exception.ResourceNotFoundException;
import com.cliniq.repository.AppointmentRepository;
import com.cliniq.repository.AppointmentSlotRepository;
import com.cliniq.repository.PatientRepository;
import com.cliniq.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository appointmentSlotRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentBookingRequest request) {
        LocalDate date = LocalDate.parse(request.getAppointmentDate(), DATE_FMT);

        // Validate date is not in the past
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book appointments for past dates");
        }

        // Find or create patient
        Patient patient = patientRepository.findByPhoneAndName(request.getPhone(), request.getPatientName())
                .orElseGet(() -> {
                    log.info("Creating new patient: {} ({})", request.getPatientName(), request.getPhone());
                    Patient newPatient = Patient.builder()
                            .name(request.getPatientName())
                            .phone(request.getPhone())
                            .build();
                    return patientRepository.save(newPatient);
                });

        // Check if patient already has an active appointment on this date
        if (appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                patient.getId(), date, AppointmentStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Patient " + patient.getName() + " already has an appointment on " + request.getAppointmentDate());
        }

        // Validate slot exists and is active
        AppointmentSlot slot = appointmentSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + request.getSlotId()));

        if (!slot.isActive()) {
            throw new IllegalStateException("This appointment slot is currently inactive");
        }

        // Validate the date's day-of-week matches the slot's day
        DayOfWeekEnum requestDay = DayOfWeekEnum.valueOf(date.getDayOfWeek().name());
        if (slot.getDayOfWeek() != requestDay) {
            throw new IllegalArgumentException(
                    "Date " + request.getAppointmentDate() + " is a " + requestDay +
                    " but the selected slot is for " + slot.getDayOfWeek());
        }

        // Check slot capacity
        long currentBookings = appointmentRepository.countActiveBookingsForSlot(slot.getId(), date);
        if (currentBookings >= slot.getMaxPatients()) {
            throw new IllegalStateException(
                    "This slot is fully booked for " + request.getAppointmentDate() +
                    " (" + currentBookings + "/" + slot.getMaxPatients() + " patients)");
        }

        // Create appointment
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .slot(slot)
                .appointmentDate(date)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(AppointmentStatus.BOOKED)
                .notes(request.getNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment booked: ID={}, Patient={}, Date={}, Slot={}-{}",
                saved.getId(), patient.getName(), date, slot.getStartTime(), slot.getEndTime());

        return mapToResponse(saved);
    }

    public List<AppointmentResponse> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByAppointmentDateOrderByStartTimeAsc(date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByDateAndStatus(LocalDate date, String status) {
        AppointmentStatus appointmentStatus = parseStatus(status);
        return appointmentRepository.findByAppointmentDateAndStatusOrderByStartTimeAsc(date, appointmentStatus)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AppointmentResponse getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
        return mapToResponse(appointment);
    }

    public List<AppointmentResponse> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment cancelled: ID={}", appointmentId);

        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRescheduleRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reschedule a cancelled appointment");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reschedule a completed appointment");
        }

        LocalDate newDate = LocalDate.parse(request.getNewAppointmentDate(), DATE_FMT);

        if (newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot reschedule to a past date");
        }

        // Validate new slot
        AppointmentSlot newSlot = appointmentSlotRepository.findById(request.getNewSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with id: " + request.getNewSlotId()));

        if (!newSlot.isActive()) {
            throw new IllegalStateException("The new appointment slot is currently inactive");
        }

        // Validate day-of-week matches
        DayOfWeekEnum requestDay = DayOfWeekEnum.valueOf(newDate.getDayOfWeek().name());
        if (newSlot.getDayOfWeek() != requestDay) {
            throw new IllegalArgumentException(
                    "Date " + request.getNewAppointmentDate() + " is a " + requestDay +
                    " but the selected slot is for " + newSlot.getDayOfWeek());
        }

        // Check new slot capacity
        long currentBookings = appointmentRepository.countActiveBookingsForSlot(newSlot.getId(), newDate);
        if (currentBookings >= newSlot.getMaxPatients()) {
            throw new IllegalStateException(
                    "The new slot is fully booked for " + request.getNewAppointmentDate());
        }

        // Update appointment
        appointment.setSlot(newSlot);
        appointment.setAppointmentDate(newDate);
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());
        appointment.setTokenNumber(null); // Reset token on reschedule

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment rescheduled: ID={}, NewDate={}, NewSlot={}-{}",
                appointmentId, newDate, newSlot.getStartTime(), newSlot.getEndTime());

        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId, String newStatus) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        AppointmentStatus status = parseStatus(newStatus);
        validateStatusTransition(appointment.getStatus(), status);

        appointment.setStatus(status);

        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(LocalDateTime.now());
        }

        // When checking in, create a Visit entry to join the unified queue
        if (status == AppointmentStatus.CHECKED_IN) {
            // Use today's date (not appointmentDate) because the patient is
            // physically present NOW and joining today's live queue.
            LocalDate today = LocalDate.now();

            // Generate next token number for today
            Integer maxToken = visitRepository.findMaxTokenNumberByDate(today).orElse(0);
            int nextToken = maxToken + 1;

            Visit visit = Visit.builder()
                    .tokenNumber(nextToken)
                    .visitDate(today)
                    .patient(appointment.getPatient())
                    .status(VisitStatus.WAITING)
                    .visitType(VisitType.APPOINTMENT)
                    .appointment(appointment)
                    .appointmentTime(appointment.getStartTime())
                    .build();
            visitRepository.save(visit);

            appointment.setTokenNumber(nextToken);
            log.info("Appointment checked in: ID={}, Token={}, SlotTime={}",
                    appointmentId, nextToken, appointment.getStartTime());
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment status updated: ID={}, NewStatus={}", appointmentId, status);

        return mapToResponse(saved);
    }

    private void validateStatusTransition(AppointmentStatus current, AppointmentStatus target) {
        boolean valid = switch (current) {
            case BOOKED -> target == AppointmentStatus.CHECKED_IN ||
                           target == AppointmentStatus.CANCELLED ||
                           target == AppointmentStatus.NO_SHOW;
            case CHECKED_IN -> target == AppointmentStatus.COMPLETED ||
                               target == AppointmentStatus.NO_SHOW;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Cannot transition from " + current + " to " + target);
        }
    }

    private AppointmentStatus parseStatus(String status) {
        try {
            return AppointmentStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: '" + status + "'. Must be one of: BOOKED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW");
        }
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getName())
                .phone(appointment.getPatient().getPhone())
                .slotId(appointment.getSlot().getId())
                .appointmentDate(appointment.getAppointmentDate().format(DATE_FMT))
                .startTime(appointment.getStartTime().format(TIME_FMT))
                .endTime(appointment.getEndTime().format(TIME_FMT))
                .tokenNumber(appointment.getTokenNumber())
                .status(appointment.getStatus().name())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt() != null ? appointment.getCreatedAt().format(DATETIME_FMT) : null)
                .updatedAt(appointment.getUpdatedAt() != null ? appointment.getUpdatedAt().format(DATETIME_FMT) : null)
                .cancelledAt(appointment.getCancelledAt() != null ? appointment.getCancelledAt().format(DATETIME_FMT) : null)
                .build();
    }
}
