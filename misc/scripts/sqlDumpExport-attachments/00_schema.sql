PRAGMA foreign_keys = ON;
PRAGMA encoding = "UTF-8";

-- ProjectForge SQL-Export - Schema
-- Exportiert am: {{EXPORT_DATE}}

CREATE TABLE IF NOT EXISTS T_PF_USER (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    username TEXT NOT NULL,
    firstname TEXT,
    lastname TEXT,
    email TEXT,
    description TEXT,
    deactivated INTEGER DEFAULT 0,
    local_user INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS T_GROUP (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    name TEXT NOT NULL,
    description TEXT,
    organization TEXT,
    local_group INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS T_TASK (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    parent_task_id INTEGER,
    title TEXT NOT NULL,
    status TEXT,
    priority TEXT,
    short_description TEXT,
    description TEXT,
    responsible_user_id INTEGER,
    reference TEXT,
    FOREIGN KEY (parent_task_id) REFERENCES T_TASK(pk),
    FOREIGN KEY (responsible_user_id) REFERENCES T_PF_USER(pk)
);

CREATE TABLE IF NOT EXISTS T_TIMESHEET (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    user_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    start_time TEXT NOT NULL,
    stop_time TEXT NOT NULL,
    location TEXT,
    description TEXT,
    reference TEXT,
    tag TEXT,
    time_zone TEXT,
    FOREIGN KEY (user_id) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (task_id) REFERENCES T_TASK(pk)
);

-- === NEUE TABELLEN (16 zusätzliche) ===

-- FINANZBUCHHALTUNG (8 Tabellen)

CREATE TABLE IF NOT EXISTS T_FIBU_KONTO (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummer INTEGER UNIQUE,
    bezeichnung TEXT NOT NULL,
    description TEXT,
    status TEXT
);

CREATE TABLE IF NOT EXISTS T_FIBU_KUNDE (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    name TEXT NOT NULL,
    identifier TEXT,
    division TEXT,
    status TEXT,
    konto_id INTEGER,
    description TEXT,
    FOREIGN KEY (konto_id) REFERENCES T_FIBU_KONTO(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_PROJEKT (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummer INTEGER,
    name TEXT NOT NULL,
    identifier TEXT,
    status TEXT,
    kunde_id INTEGER,
    konto_id INTEGER,
    task_fk INTEGER,
    intern_kost2_4 INTEGER,
    projektmanager_group_fk INTEGER,
    projectmanager_fk INTEGER,
    headofbusinessmanager_fk INTEGER,
    salesmanager_fk INTEGER,
    description TEXT,
    FOREIGN KEY (kunde_id) REFERENCES T_FIBU_KUNDE(pk),
    FOREIGN KEY (konto_id) REFERENCES T_FIBU_KONTO(pk),
    FOREIGN KEY (task_fk) REFERENCES T_TASK(pk),
    FOREIGN KEY (projektmanager_group_fk) REFERENCES T_GROUP(pk),
    FOREIGN KEY (projectmanager_fk) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (headofbusinessmanager_fk) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (salesmanager_fk) REFERENCES T_PF_USER(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_AUFTRAG (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummer INTEGER,
    status TEXT,
    kunde_fk INTEGER,
    projekt_fk INTEGER,
    contact_person_fk INTEGER,
    projectmanager_fk INTEGER,
    headofbusinessmanager_fk INTEGER,
    salesmanager_fk INTEGER,
    titel TEXT,
    bemerkung TEXT,
    referenz TEXT,
    kunde_text TEXT,
    angebots_datum TEXT,
    erfassungs_datum TEXT,
    entscheidungs_datum TEXT,
    bindungs_frist TEXT,
    beauftragungs_datum TEXT,
    FOREIGN KEY (kunde_fk) REFERENCES T_FIBU_KUNDE(pk),
    FOREIGN KEY (projekt_fk) REFERENCES T_FIBU_PROJEKT(pk),
    FOREIGN KEY (contact_person_fk) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (projectmanager_fk) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (headofbusinessmanager_fk) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (salesmanager_fk) REFERENCES T_PF_USER(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_AUFTRAG_POSITION (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    auftrag_fk INTEGER,
    number INTEGER,
    art TEXT,
    titel TEXT,
    bemerkung TEXT,
    nettoSumme REAL,
    status TEXT,
    vollstaendigFakturiert INTEGER DEFAULT 0,
    task_id INTEGER,
    FOREIGN KEY (auftrag_fk) REFERENCES T_FIBU_AUFTRAG(pk),
    FOREIGN KEY (task_id) REFERENCES T_TASK(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_PAYMENT_SCHEDULE (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    auftrag_fk INTEGER,
    number INTEGER,
    amount REAL,
    schedule_date TEXT,
    reached INTEGER DEFAULT 0,
    vollstaendigFakturiert INTEGER DEFAULT 0,
    comment TEXT,
    FOREIGN KEY (auftrag_fk) REFERENCES T_FIBU_AUFTRAG(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_RECHNUNG (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummer INTEGER,
    kunde_id INTEGER,
    projekt_id INTEGER,
    datum TEXT,
    faelligkeit TEXT,
    bezahl_datum TEXT,
    zahlBetrag REAL,
    status TEXT,
    typ TEXT,
    bemerkung TEXT,
    besonderheiten TEXT,
    konto_id INTEGER,
    discountPercent REAL,
    discountMaturity TEXT,
    FOREIGN KEY (kunde_id) REFERENCES T_FIBU_KUNDE(pk),
    FOREIGN KEY (projekt_id) REFERENCES T_FIBU_PROJEKT(pk),
    FOREIGN KEY (konto_id) REFERENCES T_FIBU_KONTO(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_RECHNUNG_POSITION (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    rechnung_fk INTEGER,
    number INTEGER,
    text TEXT,
    menge REAL,
    einzelNetto REAL,
    vat REAL,
    auftragsPosition_fk INTEGER,
    FOREIGN KEY (rechnung_fk) REFERENCES T_FIBU_RECHNUNG(pk),
    FOREIGN KEY (auftragsPosition_fk) REFERENCES T_FIBU_AUFTRAG_POSITION(pk)
);

-- KOSTENRECHNUNG (4 Tabellen)

CREATE TABLE IF NOT EXISTS T_FIBU_KOST1 (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummernkreis INTEGER,
    bereich INTEGER,
    teilbereich INTEGER,
    endziffer INTEGER,
    kostentraeger_status TEXT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS T_FIBU_KOST2ART (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    id INTEGER,
    name TEXT,
    description TEXT,
    fakturiert INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS T_FIBU_KOST2 (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    nummernkreis INTEGER,
    bereich INTEGER,
    teilbereich INTEGER,
    kost2_art_id INTEGER,
    work_fraction REAL,
    description TEXT,
    comment TEXT,
    kostentraeger_status TEXT,
    projekt_id INTEGER,
    FOREIGN KEY (kost2_art_id) REFERENCES T_FIBU_KOST2ART(pk),
    FOREIGN KEY (projekt_id) REFERENCES T_FIBU_PROJEKT(pk)
);

CREATE TABLE IF NOT EXISTS T_FIBU_BUCHUNGSSATZ (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    year INTEGER,
    month INTEGER,
    satznr INTEGER,
    betrag REAL,
    sh TEXT,
    datum TEXT,
    konto_id INTEGER,
    gegen_konto_id INTEGER,
    kost1_id INTEGER,
    kost2_id INTEGER,
    menge REAL,
    beleg TEXT,
    text TEXT,
    comment TEXT,
    FOREIGN KEY (konto_id) REFERENCES T_FIBU_KONTO(pk),
    FOREIGN KEY (gegen_konto_id) REFERENCES T_FIBU_KONTO(pk),
    FOREIGN KEY (kost1_id) REFERENCES T_FIBU_KOST1(pk),
    FOREIGN KEY (kost2_id) REFERENCES T_FIBU_KOST2(pk)
);

-- PERSONAL & URLAUB (4 Tabellen)

CREATE TABLE IF NOT EXISTS T_FIBU_EMPLOYEE (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    user_id INTEGER NOT NULL,
    kost1_id INTEGER,
    position_text TEXT,
    eintritt TEXT,
    austritt TEXT,
    abteilung TEXT,
    staffNumber TEXT,
    FOREIGN KEY (user_id) REFERENCES T_PF_USER(pk),
    FOREIGN KEY (kost1_id) REFERENCES T_FIBU_KOST1(pk)
);

CREATE TABLE IF NOT EXISTS T_EMPLOYEE_VACATION (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    employee_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    replacement_id INTEGER NOT NULL,
    manager_id INTEGER NOT NULL,
    vacation_status TEXT NOT NULL,
    is_special INTEGER DEFAULT 0,
    is_half_day_begin INTEGER DEFAULT 0,
    is_half_day_end INTEGER DEFAULT 0,
    comment TEXT,
    FOREIGN KEY (employee_id) REFERENCES T_FIBU_EMPLOYEE(pk),
    FOREIGN KEY (replacement_id) REFERENCES T_FIBU_EMPLOYEE(pk),
    FOREIGN KEY (manager_id) REFERENCES T_FIBU_EMPLOYEE(pk)
);

CREATE TABLE IF NOT EXISTS T_EMPLOYEE_VACATION_OTHER_REPLACEMENTS (
    vacation_id INTEGER NOT NULL,
    employee_id INTEGER NOT NULL,
    PRIMARY KEY (vacation_id, employee_id),
    FOREIGN KEY (vacation_id) REFERENCES T_EMPLOYEE_VACATION(pk),
    FOREIGN KEY (employee_id) REFERENCES T_FIBU_EMPLOYEE(pk)
);

CREATE TABLE IF NOT EXISTS T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY (
    pk INTEGER PRIMARY KEY,
    created TEXT,
    last_update TEXT,
    deleted INTEGER DEFAULT 0,
    employee_id INTEGER NOT NULL,
    date TEXT,
    amount REAL,
    description TEXT,
    FOREIGN KEY (employee_id) REFERENCES T_FIBU_EMPLOYEE(pk)
);

-- Indizes für bessere Performance (bestehende)
CREATE INDEX IF NOT EXISTS idx_timesheet_user ON T_TIMESHEET(user_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_task ON T_TIMESHEET(task_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_start ON T_TIMESHEET(start_time);
CREATE INDEX IF NOT EXISTS idx_task_parent ON T_TASK(parent_task_id);
CREATE INDEX IF NOT EXISTS idx_task_responsible ON T_TASK(responsible_user_id);

-- Indizes für neue Tabellen
CREATE INDEX IF NOT EXISTS idx_kunde_konto ON T_FIBU_KUNDE(konto_id);
CREATE INDEX IF NOT EXISTS idx_projekt_kunde ON T_FIBU_PROJEKT(kunde_id);
CREATE INDEX IF NOT EXISTS idx_projekt_konto ON T_FIBU_PROJEKT(konto_id);
CREATE INDEX IF NOT EXISTS idx_projekt_task ON T_FIBU_PROJEKT(task_fk);
CREATE INDEX IF NOT EXISTS idx_auftrag_kunde ON T_FIBU_AUFTRAG(kunde_fk);
CREATE INDEX IF NOT EXISTS idx_auftrag_projekt ON T_FIBU_AUFTRAG(projekt_fk);
CREATE INDEX IF NOT EXISTS idx_auftragposition_auftrag ON T_FIBU_AUFTRAG_POSITION(auftrag_fk);
CREATE INDEX IF NOT EXISTS idx_paymentschedule_auftrag ON T_FIBU_PAYMENT_SCHEDULE(auftrag_fk);
CREATE INDEX IF NOT EXISTS idx_rechnung_kunde ON T_FIBU_RECHNUNG(kunde_id);
CREATE INDEX IF NOT EXISTS idx_rechnung_projekt ON T_FIBU_RECHNUNG(projekt_id);
CREATE INDEX IF NOT EXISTS idx_rechnung_datum ON T_FIBU_RECHNUNG(datum);
CREATE INDEX IF NOT EXISTS idx_rechnungposition_rechnung ON T_FIBU_RECHNUNG_POSITION(rechnung_fk);
CREATE INDEX IF NOT EXISTS idx_kost2_kost2art ON T_FIBU_KOST2(kost2_art_id);
CREATE INDEX IF NOT EXISTS idx_kost2_projekt ON T_FIBU_KOST2(projekt_id);
CREATE INDEX IF NOT EXISTS idx_buchungssatz_konto ON T_FIBU_BUCHUNGSSATZ(konto_id);
CREATE INDEX IF NOT EXISTS idx_buchungssatz_gegen_konto ON T_FIBU_BUCHUNGSSATZ(gegen_konto_id);
CREATE INDEX IF NOT EXISTS idx_buchungssatz_kost1 ON T_FIBU_BUCHUNGSSATZ(kost1_id);
CREATE INDEX IF NOT EXISTS idx_buchungssatz_kost2 ON T_FIBU_BUCHUNGSSATZ(kost2_id);
CREATE INDEX IF NOT EXISTS idx_buchungssatz_datum ON T_FIBU_BUCHUNGSSATZ(datum);
CREATE INDEX IF NOT EXISTS idx_employee_user ON T_FIBU_EMPLOYEE(user_id);
CREATE INDEX IF NOT EXISTS idx_employee_kost1 ON T_FIBU_EMPLOYEE(kost1_id);
CREATE INDEX IF NOT EXISTS idx_vacation_employee ON T_EMPLOYEE_VACATION(employee_id);
CREATE INDEX IF NOT EXISTS idx_vacation_dates ON T_EMPLOYEE_VACATION(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_vacation_replacement ON T_EMPLOYEE_VACATION(replacement_id);
CREATE INDEX IF NOT EXISTS idx_vacation_manager ON T_EMPLOYEE_VACATION(manager_id);
CREATE INDEX IF NOT EXISTS idx_leave_entry_employee ON T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY(employee_id);
CREATE INDEX IF NOT EXISTS idx_leave_entry_date ON T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY(date);
