CREATE SEQUENCE customer_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    NO CYCLE;

CREATE TABLE customers
(
    id              UUID                        NOT NULL,
    version         BIGINT                      NOT NULL,

    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(255)                NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by      VARCHAR(255)                NOT NULL,

    customer_type   VARCHAR(20)                 NOT NULL,
    customer_number VARCHAR(20)                 NOT NULL,
    email           VARCHAR(254)                NOT NULL,
    phone           VARCHAR(30)                 NOT NULL,
    status          VARCHAR(20)                 NOT NULL,

    CONSTRAINT pk_customers
        PRIMARY KEY (id),

    CONSTRAINT uc_customers_customer_number
        UNIQUE (customer_number),

    CONSTRAINT ck_customers_customer_type
        CHECK (
            customer_type IN (
                              'INDIVIDUAL',
                              'LEGAL_ENTITY'
                )
            ),

    CONSTRAINT ck_customers_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'CLOSED'
                )
            )
);

CREATE TABLE individual_customers
(
    id             UUID         NOT NULL,

    first_name     VARCHAR(100) NOT NULL,
    middle_name    VARCHAR(100),
    last_name      VARCHAR(100) NOT NULL,
    egn            VARCHAR(10)  NOT NULL,

    country_code   VARCHAR(2)   NOT NULL,
    city           VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),

    CONSTRAINT pk_individual_customers
        PRIMARY KEY (id),

    CONSTRAINT fk_individual_customers_customer
        FOREIGN KEY (id) REFERENCES customers (id),

    CONSTRAINT uc_individual_customers_egn
        UNIQUE (egn)
);

CREATE TABLE legal_entity_customers
(
    id                            UUID         NOT NULL,

    company_name                  VARCHAR(200) NOT NULL,
    eik                           VARCHAR(9)   NOT NULL,

    registered_country_code       VARCHAR(2)   NOT NULL,
    registered_city               VARCHAR(100) NOT NULL,
    registered_postal_code        VARCHAR(20)  NOT NULL,
    registered_address_line_1     VARCHAR(200) NOT NULL,
    registered_address_line_2     VARCHAR(200),

    correspondence_country_code   VARCHAR(2)   NOT NULL,
    correspondence_city           VARCHAR(100) NOT NULL,
    correspondence_postal_code    VARCHAR(20)  NOT NULL,
    correspondence_address_line_1 VARCHAR(200) NOT NULL,
    correspondence_address_line_2 VARCHAR(200),

    CONSTRAINT pk_legal_entity_customers
        PRIMARY KEY (id),

    CONSTRAINT fk_legal_entity_customers_customer
        FOREIGN KEY (id) REFERENCES customers (id),

    CONSTRAINT uc_legal_entity_customers_eik
        UNIQUE (eik)
);

CREATE TABLE representatives
(
    id                       UUID                        NOT NULL,
    version                  BIGINT                      NOT NULL,

    created_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by               VARCHAR(255)                NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by               VARCHAR(255)                NOT NULL,

    legal_entity_customer_id UUID                        NOT NULL,
    individual_customer_id   UUID,

    first_name               VARCHAR(100)                NOT NULL,
    middle_name              VARCHAR(100),
    last_name                VARCHAR(100)                NOT NULL,
    role                     VARCHAR(40)                 NOT NULL,
    valid_from               DATE                        NOT NULL,
    valid_to                 DATE,

    CONSTRAINT pk_representatives
        PRIMARY KEY (id),

    CONSTRAINT fk_representatives_legal_entity_customer
        FOREIGN KEY (legal_entity_customer_id) REFERENCES legal_entity_customers (id),

    CONSTRAINT fk_representatives_individual_customer
        FOREIGN KEY (individual_customer_id) REFERENCES individual_customers (id),

    CONSTRAINT ck_representatives_role
        CHECK (
            role IN (
                     'LEGAL_REPRESENTATIVE',
                     'AUTHORIZED_REPRESENTATIVE'
                )
            ),

    CONSTRAINT ck_representatives_period
        CHECK (
            valid_to IS NULL
                OR valid_to >= valid_from
            )
);

CREATE INDEX ix_representatives_legal_entity_customer_id
    ON representatives (legal_entity_customer_id);

CREATE INDEX ix_representatives_individual_customer_id
    ON representatives (individual_customer_id);