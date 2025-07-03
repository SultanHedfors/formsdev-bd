INSERT INTO activity_employee (activity_id,
                               employee_id,
                               user_modified,
                               work_schedule_id)
SELECT a.zajecie_id,
       COALESCE(ws.substitute_employee_id, ws.employee_id) AS employee_id,
       0                                                   AS user_modified,
       ws.id                                               AS work_schedule_id
FROM work_schedule ws
         JOIN zajecie a
              ON CAST(a.zajecie_data AS DATE) = CAST(
                      ws.year_month || '-' || lpad(ws.day_of_month, 2, '0') AS DATE
                                                )
                  AND SUBSTRING(a.zajecie_godz FROM 12 FOR 8) >= ws.work_start_time || ':00'
                  AND SUBSTRING(a.zajecie_godz FROM 12 FOR 8) <= ws.work_end_time || ':00'
         JOIN zabieg p ON p.zabieg_id = a.zabieg_id
         JOIN stanowisko st ON st.stanowisko_id = a.stanowisko_id
         LEFT JOIN activity_employee ae
                   ON ae.activity_id = a.zajecie_id AND ae.user_modified = 1
WHERE (ws.processed IS NULL OR ws.processed = 0)
  AND ae.id IS NULL
  AND (
    (LOWER(ws.work_mode) IN ('uw', 'zl')
        AND LOWER(st.stanowisko_uwagi) = LOWER(ws.room_symbol)
        AND ws.substitute_employee_id IS NOT NULL)
        OR
    (LOWER(ws.work_mode) IN ('f', 'b', 'u')
        AND LOWER(p.zabieg_uwagi) = LOWER(ws.work_mode))
        OR
    (LOWER(ws.work_mode) = 's'
        AND LOWER(st.stanowisko_uwagi) = LOWER(ws.room_symbol))
    )
