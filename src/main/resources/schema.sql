-- Rent-A-Car schema.
--
-- Replaces four files of raw Java object serialization. That format stored a whole
-- object graph per file, so Booking.ser carried its own frozen copy of the Customer
-- and the Car (which carried its own copy of the CarOwner). Those copies drifted from
-- the real records the moment anything was edited, and writing one back could
-- overwrite a balance earned since the booking was made. Referencing rows by id
-- removes that entire class of bug rather than working around it.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS car_owner (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    cnic        TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    contact_no  TEXT    NOT NULL,
    balance     INTEGER NOT NULL DEFAULT 0,
    -- Records are retired, never erased. Deleting an owner used to cascade their cars
    -- away and every booking those cars had ever had, which threw away the rental
    -- history the business is accountable for.
    deleted     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    cnic        TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    contact_no  TEXT    NOT NULL,
    bill        INTEGER NOT NULL DEFAULT 0,
    deleted     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS car (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    maker             TEXT    NOT NULL,
    name              TEXT    NOT NULL,
    colour            TEXT,
    type              TEXT,
    seating_capacity  INTEGER NOT NULL,
    model             TEXT,
    condition         TEXT,
    reg_no            TEXT    NOT NULL UNIQUE,
    rent_per_hour     INTEGER NOT NULL,
    -- Removing an owner removes their cars, which is what CarOwner_Remove did by
    -- hand in a loop. The database now does it in one atomic step.
    owner_id          INTEGER NOT NULL REFERENCES car_owner(id) ON DELETE CASCADE,
    deleted           INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS booking (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id  INTEGER NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    car_id       INTEGER NOT NULL REFERENCES car(id)      ON DELETE CASCADE,
    rent_time    INTEGER NOT NULL,
    -- NULL while the car is still out. The old format used 0 as that sentinel, which
    -- is also a valid epoch millisecond and made "not returned" indistinguishable
    -- from "returned in 1970" in any query.
    return_time  INTEGER,
    -- What the customer was actually charged, recorded at the moment of return.
    -- Revenue reports must not recompute this from the car's rent_per_hour: that
    -- rate is editable, so raising a price would silently rewrite the takings of
    -- every rental that car ever had. NULL until the car comes back.
    amount_charged INTEGER
);

-- Who may sign in. The password is a BCrypt hash, never the password itself, and the
-- role decides whether the destructive actions are offered at all.
CREATE TABLE IF NOT EXISTS app_user (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash  TEXT NOT NULL,
    role           TEXT NOT NULL CHECK (role IN ('ADMIN', 'STAFF'))
);

-- Every list filters on this, so it is worth indexing alongside what it is joined to.
CREATE INDEX IF NOT EXISTS idx_car_live          ON car(deleted);
CREATE INDEX IF NOT EXISTS idx_customer_live     ON customer(deleted);
CREATE INDEX IF NOT EXISTS idx_car_owner_live    ON car_owner(deleted);
CREATE INDEX IF NOT EXISTS idx_car_owner_id      ON car(owner_id);
CREATE INDEX IF NOT EXISTS idx_booking_customer  ON booking(customer_id);
CREATE INDEX IF NOT EXISTS idx_booking_car       ON booking(car_id);
-- Every "is this car currently out?" check filters on this.
CREATE INDEX IF NOT EXISTS idx_booking_open      ON booking(car_id) WHERE return_time IS NULL;
