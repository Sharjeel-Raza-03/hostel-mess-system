-- Hostel Mess, Leave & Medicine Management System
-- PostgreSQL 15+  |  Run on database: hostel_mess

DROP TABLE IF EXISTS bills             CASCADE;
DROP TABLE IF EXISTS leave_requests    CASCADE;
DROP TABLE IF EXISTS meal_records      CASCADE;
DROP TABLE IF EXISTS daily_mess_count  CASCADE;
DROP TABLE IF EXISTS medicines         CASCADE;
DROP TABLE IF EXISTS settings          CASCADE;
DROP TABLE IF EXISTS users             CASCADE;

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    room_no       VARCHAR(20),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
);

-- ---------------------------------------------------------------
-- meal_records
-- ---------------------------------------------------------------
CREATE TABLE meal_records (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT    NOT NULL,
    meal_date DATE      NOT NULL,
    breakfast BOOLEAN   NOT NULL DEFAULT FALSE,
    lunch     BOOLEAN   NOT NULL DEFAULT FALSE,
    dinner    BOOLEAN   NOT NULL DEFAULT FALSE,
    on_leave  BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_meal_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_meal_user_date UNIQUE (user_id, meal_date)
);

-- ---------------------------------------------------------------
-- leave_requests
-- ---------------------------------------------------------------
CREATE TABLE leave_requests (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    from_date    DATE         NOT NULL,
    to_date      DATE         NOT NULL,
    reason       VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by  BIGINT,
    admin_remark VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    reviewed_at  TIMESTAMP,
    CONSTRAINT fk_leave_user     FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_dates   CHECK (to_date >= from_date),
    CONSTRAINT chk_leave_status  CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- ---------------------------------------------------------------
-- daily_mess_count
-- ---------------------------------------------------------------
CREATE TABLE daily_mess_count (
    id              BIGSERIAL PRIMARY KEY,
    count_date      DATE      NOT NULL,
    breakfast_count INT       NOT NULL DEFAULT 0,
    lunch_count     INT       NOT NULL DEFAULT 0,
    dinner_count    INT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_mess_count_date UNIQUE (count_date),
    CONSTRAINT chk_mess_counts_nonneg CHECK (
        breakfast_count >= 0 AND lunch_count >= 0 AND dinner_count >= 0)
);

-- ---------------------------------------------------------------
-- bills
-- ---------------------------------------------------------------
CREATE TABLE bills (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    bill_month   INT           NOT NULL,
    bill_year    INT           NOT NULL,
    total_meals  INT           NOT NULL DEFAULT 0,
    leave_days   INT           NOT NULL DEFAULT 0,
    meal_rate    DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    generated_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bill_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_bill_month   CHECK (bill_month BETWEEN 1 AND 12),
    CONSTRAINT chk_bill_amounts CHECK (
        total_meals >= 0 AND leave_days >= 0 AND meal_rate >= 0 AND total_amount >= 0),
    CONSTRAINT uq_bill_user_month_year UNIQUE (user_id, bill_month, bill_year)
);

-- ---------------------------------------------------------------
-- medicines
-- ---------------------------------------------------------------
CREATE TABLE medicines (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    quantity    INT          NOT NULL DEFAULT 0,
    unit        VARCHAR(20)  NOT NULL,
    expiry_date DATE         NOT NULL,
    updated_by  BIGINT,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_medicine_updated_by FOREIGN KEY (updated_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_medicine_quantity CHECK (quantity >= 0),
    CONSTRAINT uq_medicine_name_expiry UNIQUE (name, expiry_date)
);

-- ---------------------------------------------------------------
-- settings
-- ---------------------------------------------------------------
CREATE TABLE settings (
    key   VARCHAR(50) PRIMARY KEY,
    value VARCHAR(50) NOT NULL
);

-- ---------------------------------------------------------------
-- Indexes (columns used in WHERE / JOIN / ORDER BY)
-- uq_* constraints already create indexes on:
--   users(email), meal_records(user_id, meal_date), daily_mess_count(count_date),
--   bills(user_id, bill_month, bill_year), medicines(name, expiry_date)
-- ---------------------------------------------------------------
CREATE INDEX idx_meal_date            ON meal_records (meal_date);
CREATE INDEX idx_meal_on_leave        ON meal_records (meal_date) WHERE on_leave = TRUE;

CREATE INDEX idx_leave_user           ON leave_requests (user_id);
CREATE INDEX idx_leave_status         ON leave_requests (status);
CREATE INDEX idx_leave_user_dates     ON leave_requests (user_id, from_date, to_date);

CREATE INDEX idx_bills_month_year     ON bills (bill_year, bill_month);

CREATE INDEX idx_medicines_name       ON medicines (LOWER(name));
CREATE INDEX idx_medicines_category   ON medicines (category);
CREATE INDEX idx_medicines_expiry     ON medicines (expiry_date);
CREATE INDEX idx_medicines_quantity   ON medicines (quantity);
