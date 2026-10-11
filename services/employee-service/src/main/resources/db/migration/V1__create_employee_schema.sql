CREATE SEQUENCE employee_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    NO CYCLE;

CREATE TABLE employees
(
    id                    UUID                           NOT NULL,
    version               BIGINT                         NOT NULL,

    created_at            TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    created_by            VARCHAR(255)                   NOT NULL,
    updated_at            TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    updated_by            VARCHAR(255)                   NOT NULL,

    employee_number       VARCHAR(20)                    NOT NULL,
    identity_subject_id   VARCHAR(255)                   NOT NULL,
    first_name            VARCHAR(100)                   NOT NULL,
    last_name             VARCHAR(100)                   NOT NULL,
    work_email            VARCHAR(254)                   NOT NULL,
    position              VARCHAR(30)                    NOT NULL,
    status                VARCHAR(20)                    NOT NULL,
    employment_start_date DATE                           NOT NULL,
    employment_end_date   DATE,

    CONSTRAINT pk_employees
        PRIMARY KEY (id),

    CONSTRAINT uc_employees_employee_number
        UNIQUE (employee_number),

    CONSTRAINT uc_employees_identity_subject
        UNIQUE (identity_subject_id),

    CONSTRAINT uc_employees_work_email
        UNIQUE (work_email),

    CONSTRAINT ck_employees_position
        CHECK (
            position IN (
                         'CLERK',
                         'LOAN_OFFICER',
                         'ADMINISTRATOR'
                )
            ),

    CONSTRAINT ck_employees_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'INACTIVE'
                )
            ),

    CONSTRAINT ck_employees_employment_period
        CHECK (
            employment_end_date IS NULL
                OR employment_end_date >= employment_start_date
            ),

    CONSTRAINT ck_employees_lifecycle
        CHECK (
            (
                status = 'ACTIVE'
                    AND employment_end_date IS NULL
                )
                OR
            (
                status = 'INACTIVE'
                    AND employment_end_date IS NOT NULL
                )
            )
);