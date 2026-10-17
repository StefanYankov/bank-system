-- Development-only Employee fixtures.
--
-- This migration is loaded only when the Spring "dev" profile is active.
-- Keycloak subject identifiers must match the committed local realm export.

INSERT INTO employees (
    id,
    version,
    created_at,
    created_by,
    updated_at,
    updated_by,
    employee_number,
    identity_subject_id,
    first_name,
    last_name,
    work_email,
    position,
    status,
    employment_start_date,
    employment_end_date
)
VALUES (
           '0199e65a-35b1-7000-8000-000000000001',
           0,
           CURRENT_TIMESTAMP,
           'SYSTEM',
           CURRENT_TIMESTAMP,
           'SYSTEM',
           'EMP-000001',
           'd05d99d3-dc70-4f12-9bad-c15aa5699688',
           'Serafim',
           'Gerasimoff',
           'serafim.gerasimoff@bank.test',
           'CLERK',
           'ACTIVE',
           DATE '2026-10-01',
           NULL
       )
ON CONFLICT (identity_subject_id) DO UPDATE
    SET
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        work_email = EXCLUDED.work_email,
        position = EXCLUDED.position,
        status = EXCLUDED.status,
        employment_start_date = EXCLUDED.employment_start_date,
        employment_end_date = EXCLUDED.employment_end_date,
        updated_at = CURRENT_TIMESTAMP,
        updated_by = 'SYSTEM';

-- EMP-000001 is assigned by the fixture. Ensure the next generated Employee
-- number is EMP-000002 without ever moving an already-used sequence backwards.
SELECT setval(
               'employee_number_seq',
               GREATEST(
                       (SELECT last_value FROM employee_number_seq),
                       1
               ),
               true
       );