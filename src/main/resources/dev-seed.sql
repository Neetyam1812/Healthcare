-- =============================================================================
-- FamilyCare - Dev Seed Script (dev-seed.sql)
-- Development & demo data for testing the Family User Dashboard.
-- NOTE: Not loaded automatically. Run manually in psql or pgAdmin against HealthCare DB.
-- =============================================================================

DO $$
DECLARE
    target_user_id BIGINT;
    member_mother_id BIGINT;
    member_father_id BIGINT;
    member_child_id BIGINT;
    med_metformin_id BIGINT;
    med_lisinopril_id BIGINT;
    med_atorvastatin_id BIGINT;
    med_vitamind_id BIGINT;
BEGIN
    -- 1. Identify Target User (defaults to first user in database)
    SELECT id INTO target_user_id FROM users ORDER BY id ASC LIMIT 1;

    IF target_user_id IS NULL THEN
        RAISE NOTICE 'No user found in users table. Please register an account first.';
        RETURN;
    END IF;

    RAISE NOTICE 'Seeding dashboard demo data for user ID: %', target_user_id;

    -- 2. Insert Family Members
    INSERT INTO family_members (user_id, full_name, relationship, date_of_birth, blood_type, allergies, chronic_conditions, created_at, updated_at)
    VALUES (target_user_id, 'Eleanor Vance', 'Mother', '1958-04-12', 'O+', 'Penicillin', 'Type 2 Diabetes, Hypertension', NOW(), NOW())
    RETURNING id INTO member_mother_id;

    INSERT INTO family_members (user_id, full_name, relationship, date_of_birth, blood_type, allergies, chronic_conditions, created_at, updated_at)
    VALUES (target_user_id, 'Arthur Vance', 'Father', '1955-09-24', 'A+', 'None known', 'Mild Arthritis', NOW(), NOW())
    RETURNING id INTO member_father_id;

    INSERT INTO family_members (user_id, full_name, relationship, date_of_birth, blood_type, allergies, chronic_conditions, created_at, updated_at)
    VALUES (target_user_id, 'Leo Vance', 'Son', '2015-06-18', 'O+', 'Peanuts', 'Asthma (mild)', NOW(), NOW())
    RETURNING id INTO member_child_id;

    -- 3. Insert Active Medicines
    INSERT INTO medicines (user_id, family_member_id, name, dosage, time_of_day, stock_count, low_stock_threshold, active, created_at, updated_at)
    VALUES (target_user_id, member_mother_id, 'Metformin', '500mg', '08:00 AM', 4, 5, true, NOW(), NOW())
    RETURNING id INTO med_metformin_id;

    INSERT INTO medicines (user_id, family_member_id, name, dosage, time_of_day, stock_count, low_stock_threshold, active, created_at, updated_at)
    VALUES (target_user_id, member_mother_id, 'Lisinopril', '10mg', '09:00 AM', 18, 5, true, NOW(), NOW())
    RETURNING id INTO med_lisinopril_id;

    INSERT INTO medicines (user_id, family_member_id, name, dosage, time_of_day, stock_count, low_stock_threshold, active, created_at, updated_at)
    VALUES (target_user_id, member_father_id, 'Atorvastatin', '20mg', '09:00 PM', 22, 5, true, NOW(), NOW())
    RETURNING id INTO med_atorvastatin_id;

    INSERT INTO medicines (user_id, family_member_id, name, dosage, time_of_day, stock_count, low_stock_threshold, active, created_at, updated_at)
    VALUES (target_user_id, member_child_id, 'Vitamin D3', '1000 IU', '12:00 PM', 30, 7, true, NOW(), NOW())
    RETURNING id INTO med_vitamind_id;

    -- 4. Insert Medicine Logs (for today and previous 6 days to show adherence routine)
    -- Today: 2 taken
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '2 hours');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_lisinopril_id, NOW() - INTERVAL '1 hour');

    -- Past 6 days
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '1 day');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_lisinopril_id, NOW() - INTERVAL '1 day');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_atorvastatin_id, NOW() - INTERVAL '1 day');

    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '2 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_lisinopril_id, NOW() - INTERVAL '2 days');

    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '3 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_lisinopril_id, NOW() - INTERVAL '3 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_atorvastatin_id, NOW() - INTERVAL '3 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_vitamind_id, NOW() - INTERVAL '3 days');

    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '4 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_atorvastatin_id, NOW() - INTERVAL '4 days');

    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '5 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_lisinopril_id, NOW() - INTERVAL '5 days');
    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_atorvastatin_id, NOW() - INTERVAL '5 days');

    INSERT INTO medicine_logs (medicine_id, taken_at) VALUES (med_metformin_id, NOW() - INTERVAL '6 days');

    -- 5. Insert Upcoming Appointments
    INSERT INTO appointments (user_id, family_member_id, doctor_name, specialty, date_time, notes, status, created_at)
    VALUES (target_user_id, member_mother_id, 'Dr. Sarah Jenkins', 'Cardiology', NOW() + INTERVAL '2 days' + INTERVAL '3 hours', 'Quarterly blood pressure review and medication check.', 'SCHEDULED', NOW());

    INSERT INTO appointments (user_id, family_member_id, doctor_name, specialty, date_time, notes, status, created_at)
    VALUES (target_user_id, member_father_id, 'Dr. Marcus Brody', 'Rheumatology', NOW() + INTERVAL '5 days' + INTERVAL '1 hour', 'Arthritis joint mobility assessment.', 'SCHEDULED', NOW());

    INSERT INTO appointments (user_id, family_member_id, doctor_name, specialty, date_time, notes, status, created_at)
    VALUES (target_user_id, member_child_id, 'Dr. Lisa Patel', 'Pediatrics', NOW() + INTERVAL '12 days', 'Annual developmental wellness checkup.', 'SCHEDULED', NOW());

    -- 6. Insert Historical Vital Readings (7 readings for each category)
    -- Blood Pressure (BP)
    INSERT INTO vital_readings (user_id, family_member_id, type, value_1, value_2, unit, status, recorded_at) VALUES
    (target_user_id, member_mother_id, 'BP', 126, 84, 'mmHg', 'NORMAL', NOW() - INTERVAL '6 days'),
    (target_user_id, member_mother_id, 'BP', 128, 86, 'mmHg', 'NORMAL', NOW() - INTERVAL '5 days'),
    (target_user_id, member_mother_id, 'BP', 132, 88, 'mmHg', 'ELEVATED', NOW() - INTERVAL '4 days'),
    (target_user_id, member_mother_id, 'BP', 125, 82, 'mmHg', 'NORMAL', NOW() - INTERVAL '3 days'),
    (target_user_id, member_mother_id, 'BP', 122, 80, 'mmHg', 'NORMAL', NOW() - INTERVAL '2 days'),
    (target_user_id, member_mother_id, 'BP', 124, 82, 'mmHg', 'NORMAL', NOW() - INTERVAL '1 day'),
    (target_user_id, member_mother_id, 'BP', 120, 80, 'mmHg', 'NORMAL', NOW() - INTERVAL '2 hours');

    -- Blood Glucose
    INSERT INTO vital_readings (user_id, family_member_id, type, value_1, value_2, unit, status, recorded_at) VALUES
    (target_user_id, member_mother_id, 'GLUCOSE', 104, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '6 days'),
    (target_user_id, member_mother_id, 'GLUCOSE', 108, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '5 days'),
    (target_user_id, member_mother_id, 'GLUCOSE', 112, NULL, 'mg/dL', 'ELEVATED', NOW() - INTERVAL '4 days'),
    (target_user_id, member_mother_id, 'GLUCOSE', 102, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '3 days'),
    (target_user_id, member_mother_id, 'GLUCOSE', 99, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '2 days'),
    (target_user_id, member_mother_id, 'GLUCOSE', 97, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '1 day'),
    (target_user_id, member_mother_id, 'GLUCOSE', 98, NULL, 'mg/dL', 'NORMAL', NOW() - INTERVAL '4 hours');

    -- Heart Rate
    INSERT INTO vital_readings (user_id, family_member_id, type, value_1, value_2, unit, status, recorded_at) VALUES
    (target_user_id, member_mother_id, 'HEART_RATE', 76, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '6 days'),
    (target_user_id, member_mother_id, 'HEART_RATE', 74, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '5 days'),
    (target_user_id, member_mother_id, 'HEART_RATE', 78, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '4 days'),
    (target_user_id, member_mother_id, 'HEART_RATE', 72, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '3 days'),
    (target_user_id, member_mother_id, 'HEART_RATE', 70, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '2 days'),
    (target_user_id, member_mother_id, 'HEART_RATE', 73, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '1 day'),
    (target_user_id, member_mother_id, 'HEART_RATE', 72, NULL, 'bpm', 'NORMAL', NOW() - INTERVAL '30 minutes');

    -- SpO2 (Oxygen Saturation)
    INSERT INTO vital_readings (user_id, family_member_id, type, value_1, value_2, unit, status, recorded_at) VALUES
    (target_user_id, member_mother_id, 'SPO2', 98, NULL, '%', 'NORMAL', NOW() - INTERVAL '6 days'),
    (target_user_id, member_mother_id, 'SPO2', 97, NULL, '%', 'NORMAL', NOW() - INTERVAL '5 days'),
    (target_user_id, member_mother_id, 'SPO2', 98, NULL, '%', 'NORMAL', NOW() - INTERVAL '4 days'),
    (target_user_id, member_mother_id, 'SPO2', 99, NULL, '%', 'NORMAL', NOW() - INTERVAL '3 days'),
    (target_user_id, member_mother_id, 'SPO2', 99, NULL, '%', 'NORMAL', NOW() - INTERVAL '2 days'),
    (target_user_id, member_mother_id, 'SPO2', 98, NULL, '%', 'NORMAL', NOW() - INTERVAL '1 day'),
    (target_user_id, member_mother_id, 'SPO2', 99, NULL, '%', 'NORMAL', NOW() - INTERVAL '1 hour');

    RAISE NOTICE 'Demo seed data inserted successfully for user ID: %', target_user_id;
END $$;
