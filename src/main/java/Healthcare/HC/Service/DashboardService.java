package Healthcare.HC.Service;

import Healthcare.HC.Entity.*;
import Healthcare.HC.Repository.*;
import Healthcare.HC.dto.UserDashboardView;
import Healthcare.HC.dto.UserDashboardView.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DashboardService {

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineLogRepository medicineLogRepository;
    private final AppointmentRepository appointmentRepository;
    private final VitalReadingRepository vitalReadingRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final AuditService auditService;

    public DashboardService(UserRepository userRepository,
                            FamilyMemberRepository familyMemberRepository,
                            MedicineRepository medicineRepository,
                            MedicineLogRepository medicineLogRepository,
                            AppointmentRepository appointmentRepository,
                            VitalReadingRepository vitalReadingRepository,
                            EmergencyEventRepository emergencyEventRepository,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.medicineRepository = medicineRepository;
        this.medicineLogRepository = medicineLogRepository;
        this.appointmentRepository = appointmentRepository;
        this.vitalReadingRepository = vitalReadingRepository;
        this.emergencyEventRepository = emergencyEventRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserDashboardView getDashboardView(Long userId, Long requestedMemberId) {
        UserDashboardView view = new UserDashboardView();

        User user = userRepository.findById(userId).orElse(null);
        String firstName = "Family";
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            firstName = user.getFullName().trim().split("\\s+")[0];
        }
        view.setUserFirstName(firstName);

        int hour = java.time.LocalTime.now().getHour();
        String greeting = "Good morning";
        if (hour >= 12 && hour < 17) {
            greeting = "Good afternoon";
        } else if (hour >= 17) {
            greeting = "Good evening";
        }
        view.setGreeting(greeting);

        LocalDate today = LocalDate.now();
        view.setTodayDateFormatted(today.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));

        // Validate member ownership
        Long memberId = null;
        if (requestedMemberId != null) {
            Optional<FamilyMember> memberOpt = familyMemberRepository.findByIdAndUserId(requestedMemberId, userId);
            if (memberOpt.isPresent()) {
                memberId = requestedMemberId;
                view.setSelectedMemberId(memberId);
                view.setSelectedMemberName(memberOpt.get().getFullName());
            }
        }
        if (memberId == null) {
            view.setSelectedMemberName("All family");
        }

        // 1. Family Members
        List<FamilyMember> allMembers = familyMemberRepository.findByUserId(userId);
        view.setFamilyMembersCount(allMembers.size());

        List<MemberCardView> memberCardViews = new ArrayList<>();
        for (FamilyMember m : allMembers) {
            Integer age = null;
            if (m.getDateOfBirth() != null) {
                age = Period.between(m.getDateOfBirth(), today).getYears();
            }
            String initials = getInitials(m.getFullName());
            memberCardViews.add(new MemberCardView(
                    m.getId(),
                    m.getFullName(),
                    initials,
                    m.getRelationship() != null ? m.getRelationship() : "Family member",
                    age,
                    m.getBloodType() != null ? m.getBloodType() : "Unknown",
                    m.getAllergies(),
                    m.getChronicConditions()
            ));
        }
        view.setFamilyMembers(memberCardViews);

        // 2. Medicines & Adherence
        List<Medicine> medicines;
        if (memberId != null) {
            medicines = medicineRepository.findByUserIdAndFamilyMemberIdAndActiveTrue(userId, memberId);
        } else {
            medicines = medicineRepository.findByUserIdAndActiveTrue(userId);
        }

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        List<MedicineLog> todayLogs = medicineLogRepository.findByUserIdAndTakenAtBetween(userId, todayStart, todayEnd);
        Set<Long> takenMedicineIds = todayLogs.stream()
                .map(log -> log.getMedicine().getId())
                .collect(Collectors.toSet());

        List<MedicineItemView> medViews = new ArrayList<>();
        int takenTodayCount = 0;
        Medicine lowStockMed = null;

        for (Medicine m : medicines) {
            boolean taken = takenMedicineIds.contains(m.getId());
            if (taken) takenTodayCount++;

            boolean isLow = m.getStockCount() <= m.getLowStockThreshold();
            if (isLow && lowStockMed == null) {
                lowStockMed = m;
            }

            String memName = m.getFamilyMember() != null ? m.getFamilyMember().getFullName() : "Whole family";

            medViews.add(new MedicineItemView(
                    m.getId(),
                    m.getName(),
                    m.getDosage() != null ? m.getDosage() : "Standard dose",
                    m.getTimeOfDay() != null ? m.getTimeOfDay() : "Daily",
                    memName,
                    taken,
                    m.getStockCount(),
                    isLow
            ));
        }
        view.setTodayMedicines(medViews);
        view.setMedicinesTotalToday(medicines.size());
        view.setMedicinesTakenToday(takenTodayCount);

        int adherencePct = medicines.isEmpty() ? 100 : (int) Math.round((double) takenTodayCount / medicines.size() * 100);
        view.setTodayAdherencePercentage(adherencePct);
        if (medicines.isEmpty()) {
            view.setAdherenceSummary("No medications scheduled today.");
        } else if (takenTodayCount == medicines.size()) {
            view.setAdherenceSummary("All " + medicines.size() + " medications taken today. Excellent routine!");
        } else {
            view.setAdherenceSummary(takenTodayCount + " of " + medicines.size() + " medications taken today.");
        }

        // 7-day adherence sparkline
        List<DayAdherenceView> weekAdherence = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime dStart = d.atStartOfDay();
            LocalDateTime dEnd = d.plusDays(1).atStartOfDay();
            List<MedicineLog> dLogs = medicineLogRepository.findByUserIdAndTakenAtBetween(userId, dStart, dEnd);
            int logCount = (int) dLogs.stream().map(l -> l.getMedicine().getId()).distinct().count();
            int totalMeds = Math.max(medicines.size(), 1);
            int pct = Math.min(100, (int) Math.round((double) logCount / totalMeds * 100));
            String dayLabel = d.format(DateTimeFormatter.ofPattern("EEE"));
            weekAdherence.add(new DayAdherenceView(dayLabel, logCount, totalMeds, pct, i == 0));
        }
        view.setWeeklyAdherence(weekAdherence);

        // 3. Appointments
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcomingAppointments;
        if (memberId != null) {
            upcomingAppointments = appointmentRepository.findByUserIdAndFamilyMemberIdAndDateTimeAfterOrderByDateTimeAsc(userId, memberId, now);
        } else {
            upcomingAppointments = appointmentRepository.findByUserIdAndDateTimeAfterOrderByDateTimeAsc(userId, now);
        }

        List<AppointmentItemView> apptViews = new ArrayList<>();
        Appointment nextAppt = null;

        for (int i = 0; i < upcomingAppointments.size(); i++) {
            Appointment a = upcomingAppointments.get(i);
            if (i == 0) nextAppt = a;
            if (i < 3) {
                String mName = a.getFamilyMember() != null ? a.getFamilyMember().getFullName() : "Self";
                String month = a.getDateTime().format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
                String dayNum = a.getDateTime().format(DateTimeFormatter.ofPattern("d"));
                String time = a.getDateTime().format(DateTimeFormatter.ofPattern("h:mm a"));
                String relative = formatRelativeTime(a.getDateTime(), now);

                apptViews.add(new AppointmentItemView(
                        a.getId(),
                        a.getDoctorName(),
                        a.getSpecialty() != null ? a.getSpecialty() : "General medicine",
                        mName,
                        month,
                        dayNum,
                        time,
                        relative,
                        a.getNotes()
                ));
            }
        }
        view.setUpcomingAppointments(apptViews);

        if (nextAppt != null) {
            view.setNextAppointmentDate(nextAppt.getDateTime().format(DateTimeFormatter.ofPattern("MMM d, h:mm a")));
            view.setNextAppointmentDoctor(nextAppt.getDoctorName());
            view.setNextAppointmentRelative(formatRelativeTime(nextAppt.getDateTime(), now));
        } else {
            view.setNextAppointmentDate("None upcoming");
            view.setNextAppointmentDoctor("No doctor scheduled");
            view.setNextAppointmentRelative("No upcoming visits");
        }

        // 4. Vitals Overview
        String[] vitalTypes = {"BP", "GLUCOSE", "HEART_RATE", "SPO2"};
        Map<String, VitalSummaryView> vitalsMap = new HashMap<>();
        String overallVitalsStatus = "NORMAL";
        boolean hasAnyVitals = false;
        String latestVitalHeadline = "All vitals in normal range";

        for (String type : vitalTypes) {
            List<VitalReading> readings;
            if (memberId != null) {
                readings = vitalReadingRepository.findByUserIdAndFamilyMemberIdAndTypeOrderByRecordedAtDesc(userId, memberId, type);
            } else {
                readings = vitalReadingRepository.findByUserIdAndTypeOrderByRecordedAtDesc(userId, type);
            }

            if (!readings.isEmpty()) {
                hasAnyVitals = true;
                VitalReading latest = readings.get(0);

                String latestValStr;
                if ("BP".equalsIgnoreCase(type)) {
                    latestValStr = (latest.getValue1() != null ? String.valueOf(latest.getValue1().intValue()) : "--")
                            + "/" + (latest.getValue2() != null ? String.valueOf(latest.getValue2().intValue()) : "--");
                } else if ("WEIGHT".equalsIgnoreCase(type) || "GLUCOSE".equalsIgnoreCase(type)) {
                    latestValStr = latest.getValue1() != null ? String.format("%.1f", latest.getValue1()) : "--";
                } else {
                    latestValStr = latest.getValue1() != null ? String.valueOf(latest.getValue1().intValue()) : "--";
                }

                String status = latest.getStatus() != null ? latest.getStatus().toUpperCase() : "NORMAL";
                if ("HIGH".equals(status)) {
                    overallVitalsStatus = "HIGH";
                    latestVitalHeadline = type + " reading is high (" + latestValStr + " " + latest.getUnit() + ")";
                } else if ("ELEVATED".equals(status) && !"HIGH".equals(overallVitalsStatus)) {
                    overallVitalsStatus = "ELEVATED";
                    latestVitalHeadline = type + " reading is elevated (" + latestValStr + " " + latest.getUnit() + ")";
                }

                String recFormatted = latest.getRecordedAt().format(DateTimeFormatter.ofPattern("MMM d, h:mm a"));

                // Take up to 7 readings in chronological order for the chart
                List<VitalReading> chartReadings = new ArrayList<>(readings.subList(0, Math.min(readings.size(), 7)));
                Collections.reverse(chartReadings);

                List<VitalDataPoint> dataPoints = chartReadings.stream().map(r -> {
                    String lbl = r.getRecordedAt().format(DateTimeFormatter.ofPattern("M/d"));
                    return new VitalDataPoint(lbl, r.getValue1() != null ? r.getValue1() : 0.0, r.getValue2());
                }).collect(Collectors.toList());

                vitalsMap.put(type, new VitalSummaryView(type, latestValStr, latest.getUnit(), status, recFormatted, dataPoints));
            } else {
                // Empty fallback placeholder
                String unit = switch (type) {
                    case "BP" -> "mmHg";
                    case "GLUCOSE" -> "mg/dL";
                    case "HEART_RATE" -> "bpm";
                    case "SPO2" -> "%";
                    default -> "";
                };
                vitalsMap.put(type, new VitalSummaryView(type, "--", unit, "NO_DATA", "No readings", Collections.emptyList()));
            }
        }

        view.setVitals(vitalsMap);
        view.setLatestVitalsStatus(hasAnyVitals ? overallVitalsStatus : "NO_DATA");
        view.setLatestVitalsSummary(hasAnyVitals ? latestVitalHeadline : "No vitals recorded yet");

        // 5. Alert Banner (Low stock or appointment in <= 24 hours)
        LocalDateTime in24Hours = now.plusHours(24);
        List<Appointment> urgentAppts = appointmentRepository.findByUserIdAndDateTimeBetweenOrderByDateTimeAsc(userId, now, in24Hours);

        if (!urgentAppts.isEmpty()) {
            Appointment a = urgentAppts.get(0);
            view.setHasAlert(true);
            view.setAlertType("UPCOMING_APPOINTMENT");
            view.setAlertTitle("Appointment in less than 24 hours");
            view.setAlertMessage("Visit with " + a.getDoctorName() + " (" + (a.getSpecialty() != null ? a.getSpecialty() : "Consultation") + ") scheduled for " + a.getDateTime().format(DateTimeFormatter.ofPattern("h:mm a")) + ".");
            view.setAlertActionText("View appointments");
            view.setAlertActionLink("#appointments-section");
        } else if (lowStockMed != null) {
            view.setHasAlert(true);
            view.setAlertType("LOW_STOCK");
            view.setAlertTitle("Low medication stock");
            view.setAlertMessage(lowStockMed.getName() + " has only " + lowStockMed.getStockCount() + " doses remaining (threshold: " + lowStockMed.getLowStockThreshold() + ").");
            view.setAlertActionText("Manage medicines");
            view.setAlertActionLink("#medicines-section");
        }

        // 6. Recent Activity (last 8 events combined)
        List<ActivityItemView> activities = new ArrayList<>();

        List<MedicineLog> recentLogs = medicineLogRepository.findTop8ByUserIdOrderByTakenAtDesc(userId);
        for (MedicineLog l : recentLogs) {
            String medName = l.getMedicine().getName() + " (" + (l.getMedicine().getDosage() != null ? l.getMedicine().getDosage() : "dose") + ")";
            String mem = l.getMedicine().getFamilyMember() != null ? "for " + l.getMedicine().getFamilyMember().getFullName() : "for self";
            activities.add(new ActivityItemView("MEDICINE", medName + " taken", mem, formatRelativeTime(l.getTakenAt(), now), l.getTakenAt()));
        }

        List<VitalReading> recentVitals = vitalReadingRepository.findTop8ByUserIdOrderByRecordedAtDesc(userId);
        for (VitalReading v : recentVitals) {
            String val = v.getValue1() != null ? String.valueOf(v.getValue1().intValue()) : "";
            if (v.getValue2() != null) val += "/" + v.getValue2().intValue();
            String title = v.getType() + " logged: " + val + " " + (v.getUnit() != null ? v.getUnit() : "");
            String mem = v.getFamilyMember() != null ? "for " + v.getFamilyMember().getFullName() : "for self";
            activities.add(new ActivityItemView("VITAL", title, mem, formatRelativeTime(v.getRecordedAt(), now), v.getRecordedAt()));
        }

        List<Appointment> recentAppts = appointmentRepository.findTop8ByUserIdOrderByDateTimeDesc(userId);
        for (Appointment a : recentAppts) {
            String title = "Appointment scheduled with " + a.getDoctorName();
            String mem = a.getFamilyMember() != null ? "for " + a.getFamilyMember().getFullName() : "for self";
            LocalDateTime actTime = a.getCreatedAt() != null ? a.getCreatedAt() : a.getDateTime();
            activities.add(new ActivityItemView("APPOINTMENT", title, mem, formatRelativeTime(actTime, now), actTime));
        }

        activities.sort((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()));
        if (activities.size() > 8) {
            activities = activities.subList(0, 8);
        }
        view.setRecentActivities(activities);

        return view;
    }

    public Map<String, Object> recordMedicineTaken(Long medicineId, Long userId, String ip) {
        Medicine medicine = medicineRepository.findByIdAndUserId(medicineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found or access denied"));

        MedicineLog log = new MedicineLog(medicine, LocalDateTime.now());
        medicineLogRepository.save(log);

        if (medicine.getStockCount() != null && medicine.getStockCount() > 0) {
            medicine.setStockCount(medicine.getStockCount() - 1);
            medicineRepository.save(medicine);
        }

        User user = medicine.getUser();
        String userEmail = user != null ? user.getEmail() : "user";
        auditService.log(userId, userEmail, "MEDICINE_TAKEN", "MEDICINE", medicineId,
                "Marked medicine as taken: " + medicine.getName(), ip);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("medicineId", medicineId);
        response.put("stockCount", medicine.getStockCount());
        response.put("takenToday", true);
        response.put("message", medicine.getName() + " marked as taken.");
        return response;
    }

    public Map<String, Object> recordEmergencySos(Long userId, Double latitude, Double longitude, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        EmergencyEvent event = new EmergencyEvent(user, latitude, longitude, "TRIGGERED");
        emergencyEventRepository.save(event);

        auditService.log(userId, user.getEmail(), "EMERGENCY_SOS", "USER", userId,
                "Emergency SOS alert triggered at lat: " + latitude + ", lng: " + longitude, ip);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("eventId", event.getId());
        response.put("message", "Emergency alert recorded successfully. Help coordinator notified.");
        return response;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "FC";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String formatRelativeTime(LocalDateTime target, LocalDateTime now) {
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), target.toLocalDate());
        if (days == 0) {
            return "Today";
        } else if (days == 1) {
            return "Tomorrow";
        } else if (days == -1) {
            return "Yesterday";
        } else if (days > 1 && days < 7) {
            return "in " + days + " days";
        } else if (days < 0 && days > -7) {
            return Math.abs(days) + " days ago";
        } else {
            return target.format(DateTimeFormatter.ofPattern("MMM d"));
        }
    }
}
