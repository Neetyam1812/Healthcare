package Healthcare.HC.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardView {

    private String userFirstName;
    private String greeting = "Good morning";
    private String todayDateFormatted;
    private Long selectedMemberId;
    private String selectedMemberName;

    // 1. Alert Banner
    private boolean hasAlert;
    private String alertType; // LOW_STOCK, UPCOMING_APPOINTMENT
    private String alertTitle;
    private String alertMessage;
    private String alertActionText;
    private String alertActionLink;

    // 2. Stat Cards
    private int familyMembersCount;
    private int medicinesTakenToday;
    private int medicinesTotalToday;
    private String nextAppointmentDate;
    private String nextAppointmentDoctor;
    private String nextAppointmentRelative;
    private String latestVitalsStatus = "NO_DATA";
    private String latestVitalsSummary = "No vitals recorded yet";

    // 3. Today's Medicines
    private List<MedicineItemView> todayMedicines = new ArrayList<>();

    // 4. Adherence
    private int todayAdherencePercentage;
    private String adherenceSummary;
    private List<DayAdherenceView> weeklyAdherence = new ArrayList<>();

    // 5. Family Members
    private List<MemberCardView> familyMembers = new ArrayList<>();

    // 6. Upcoming Appointments
    private List<AppointmentItemView> upcomingAppointments = new ArrayList<>();

    // 7. Vitals Overview
    private Map<String, VitalSummaryView> vitals = new HashMap<>();

    // 10. Recent Activity
    private List<ActivityItemView> recentActivities = new ArrayList<>();

    // =========================================================================
    // Nested View Classes
    // =========================================================================

    public static class MedicineItemView {
        private Long id;
        private String name;
        private String dosage;
        private String timeOfDay;
        private String memberName;
        private boolean takenToday;
        private int stockCount;
        private boolean lowStock;

        public MedicineItemView(Long id, String name, String dosage, String timeOfDay, String memberName, boolean takenToday, int stockCount, boolean lowStock) {
            this.id = id;
            this.name = name;
            this.dosage = dosage;
            this.timeOfDay = timeOfDay;
            this.memberName = memberName;
            this.takenToday = takenToday;
            this.stockCount = stockCount;
            this.lowStock = lowStock;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDosage() { return dosage; }
        public String getTimeOfDay() { return timeOfDay; }
        public String getMemberName() { return memberName; }
        public boolean isTakenToday() { return takenToday; }
        public int getStockCount() { return stockCount; }
        public boolean isLowStock() { return lowStock; }
    }

    public static class DayAdherenceView {
        private String dayLabel;
        private int taken;
        private int total;
        private int percentage;
        private boolean isToday;

        public DayAdherenceView(String dayLabel, int taken, int total, int percentage, boolean isToday) {
            this.dayLabel = dayLabel;
            this.taken = taken;
            this.total = total;
            this.percentage = percentage;
            this.isToday = isToday;
        }

        public String getDayLabel() { return dayLabel; }
        public int getTaken() { return taken; }
        public int getTotal() { return total; }
        public int getPercentage() { return percentage; }
        public boolean isToday() { return isToday; }
    }

    public static class MemberCardView {
        private Long id;
        private String fullName;
        private String initials;
        private String relationship;
        private Integer age;
        private String bloodType;
        private String allergies;
        private String chronicConditions;

        public MemberCardView(Long id, String fullName, String initials, String relationship, Integer age, String bloodType, String allergies, String chronicConditions) {
            this.id = id;
            this.fullName = fullName;
            this.initials = initials;
            this.relationship = relationship;
            this.age = age;
            this.bloodType = bloodType;
            this.allergies = allergies;
            this.chronicConditions = chronicConditions;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getInitials() { return initials; }
        public String getRelationship() { return relationship; }
        public Integer getAge() { return age; }
        public String getBloodType() { return bloodType; }
        public String getAllergies() { return allergies; }
        public String getChronicConditions() { return chronicConditions; }
    }

    public static class AppointmentItemView {
        private Long id;
        private String doctorName;
        private String specialty;
        private String memberName;
        private String monthShort;
        private String dayNumber;
        private String timeFormatted;
        private String relativeTime;
        private String notes;

        public AppointmentItemView(Long id, String doctorName, String specialty, String memberName, String monthShort, String dayNumber, String timeFormatted, String relativeTime, String notes) {
            this.id = id;
            this.doctorName = doctorName;
            this.specialty = specialty;
            this.memberName = memberName;
            this.monthShort = monthShort;
            this.dayNumber = dayNumber;
            this.timeFormatted = timeFormatted;
            this.relativeTime = relativeTime;
            this.notes = notes;
        }

        public Long getId() { return id; }
        public String getDoctorName() { return doctorName; }
        public String getSpecialty() { return specialty; }
        public String getMemberName() { return memberName; }
        public String getMonthShort() { return monthShort; }
        public String getDayNumber() { return dayNumber; }
        public String getTimeFormatted() { return timeFormatted; }
        public String getRelativeTime() { return relativeTime; }
        public String getNotes() { return notes; }
    }

    public static class VitalDataPoint {
        private String label;
        private double val1;
        private Double val2;

        public VitalDataPoint(String label, double val1, Double val2) {
            this.label = label;
            this.val1 = val1;
            this.val2 = val2;
        }

        public String getLabel() { return label; }
        public double getVal1() { return val1; }
        public Double getVal2() { return val2; }
    }

    public static class VitalSummaryView {
        private String type;
        private String latestValue;
        private String unit;
        private String status;
        private String recordedAtFormatted;
        private List<VitalDataPoint> readings = new ArrayList<>();

        public VitalSummaryView(String type, String latestValue, String unit, String status, String recordedAtFormatted, List<VitalDataPoint> readings) {
            this.type = type;
            this.latestValue = latestValue;
            this.unit = unit;
            this.status = status;
            this.recordedAtFormatted = recordedAtFormatted;
            this.readings = readings != null ? readings : new ArrayList<>();
        }

        public String getType() { return type; }
        public String getLatestValue() { return latestValue; }
        public String getUnit() { return unit; }
        public String getStatus() { return status; }
        public String getRecordedAtFormatted() { return recordedAtFormatted; }
        public List<VitalDataPoint> getReadings() { return readings; }
    }

    public static class ActivityItemView {
        private String type; // MEDICINE, VITAL, MEMBER, APPOINTMENT, SOS
        private String title;
        private String subtitle;
        private String timeAgo;
        private LocalDateTime timestamp;

        public ActivityItemView(String type, String title, String subtitle, String timeAgo, LocalDateTime timestamp) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.timeAgo = timeAgo;
            this.timestamp = timestamp;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getTimeAgo() { return timeAgo; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public String getUserFirstName() { return userFirstName; }
    public void setUserFirstName(String userFirstName) { this.userFirstName = userFirstName; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    public String getTodayDateFormatted() { return todayDateFormatted; }
    public void setTodayDateFormatted(String todayDateFormatted) { this.todayDateFormatted = todayDateFormatted; }

    public Long getSelectedMemberId() { return selectedMemberId; }
    public void setSelectedMemberId(Long selectedMemberId) { this.selectedMemberId = selectedMemberId; }

    public String getSelectedMemberName() { return selectedMemberName; }
    public void setSelectedMemberName(String selectedMemberName) { this.selectedMemberName = selectedMemberName; }

    public boolean isHasAlert() { return hasAlert; }
    public void setHasAlert(boolean hasAlert) { this.hasAlert = hasAlert; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getAlertTitle() { return alertTitle; }
    public void setAlertTitle(String alertTitle) { this.alertTitle = alertTitle; }

    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    public String getAlertActionText() { return alertActionText; }
    public void setAlertActionText(String alertActionText) { this.alertActionText = alertActionText; }

    public String getAlertActionLink() { return alertActionLink; }
    public void setAlertActionLink(String alertActionLink) { this.alertActionLink = alertActionLink; }

    public int getFamilyMembersCount() { return familyMembersCount; }
    public void setFamilyMembersCount(int familyMembersCount) { this.familyMembersCount = familyMembersCount; }

    public int getMedicinesTakenToday() { return medicinesTakenToday; }
    public void setMedicinesTakenToday(int medicinesTakenToday) { this.medicinesTakenToday = medicinesTakenToday; }

    public int getMedicinesTotalToday() { return medicinesTotalToday; }
    public void setMedicinesTotalToday(int medicinesTotalToday) { this.medicinesTotalToday = medicinesTotalToday; }

    public String getNextAppointmentDate() { return nextAppointmentDate; }
    public void setNextAppointmentDate(String nextAppointmentDate) { this.nextAppointmentDate = nextAppointmentDate; }

    public String getNextAppointmentDoctor() { return nextAppointmentDoctor; }
    public void setNextAppointmentDoctor(String nextAppointmentDoctor) { this.nextAppointmentDoctor = nextAppointmentDoctor; }

    public String getNextAppointmentRelative() { return nextAppointmentRelative; }
    public void setNextAppointmentRelative(String nextAppointmentRelative) { this.nextAppointmentRelative = nextAppointmentRelative; }

    public String getLatestVitalsStatus() { return latestVitalsStatus; }
    public void setLatestVitalsStatus(String latestVitalsStatus) { this.latestVitalsStatus = latestVitalsStatus; }

    public String getLatestVitalsSummary() { return latestVitalsSummary; }
    public void setLatestVitalsSummary(String latestVitalsSummary) { this.latestVitalsSummary = latestVitalsSummary; }

    public List<MedicineItemView> getTodayMedicines() { return todayMedicines; }
    public void setTodayMedicines(List<MedicineItemView> todayMedicines) { this.todayMedicines = todayMedicines; }

    public int getTodayAdherencePercentage() { return todayAdherencePercentage; }
    public void setTodayAdherencePercentage(int todayAdherencePercentage) { this.todayAdherencePercentage = todayAdherencePercentage; }

    public String getAdherenceSummary() { return adherenceSummary; }
    public void setAdherenceSummary(String adherenceSummary) { this.adherenceSummary = adherenceSummary; }

    public List<DayAdherenceView> getWeeklyAdherence() { return weeklyAdherence; }
    public void setWeeklyAdherence(List<DayAdherenceView> weeklyAdherence) { this.weeklyAdherence = weeklyAdherence; }

    public List<MemberCardView> getFamilyMembers() { return familyMembers; }
    public void setFamilyMembers(List<MemberCardView> familyMembers) { this.familyMembers = familyMembers; }

    public List<AppointmentItemView> getUpcomingAppointments() { return upcomingAppointments; }
    public void setUpcomingAppointments(List<AppointmentItemView> upcomingAppointments) { this.upcomingAppointments = upcomingAppointments; }

    public Map<String, VitalSummaryView> getVitals() { return vitals; }
    public void setVitals(Map<String, VitalSummaryView> vitals) { this.vitals = vitals; }

    public List<ActivityItemView> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<ActivityItemView> recentActivities) { this.recentActivities = recentActivities; }
}
