UPDATE work_schedule
SET processed = 1
WHERE id IN (
    SELECT DISTINCT work_schedule_id
    FROM activity_employee
    WHERE created_at = ?
);