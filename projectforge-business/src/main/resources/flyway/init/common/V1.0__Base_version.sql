CREATE SEQUENCE hibernate_sequence
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE;

CREATE SEQUENCE sq_base_ghistory_attr_data_pk
START WITH 1
INCREMENT BY 50
NO MINVALUE
NO MAXVALUE;

CREATE SEQUENCE sq_base_ghistory_attr_pk
START WITH 1
INCREMENT BY 50
NO MINVALUE
NO MAXVALUE;

CREATE SEQUENCE sq_base_ghistory_pk
START WITH 1
INCREMENT BY 50
NO MINVALUE
NO MAXVALUE;

CREATE TABLE t_address_attr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL
);

CREATE TABLE t_address_attrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_addressbook (
  pk                        INTEGER NOT NULL,
  created                   TIMESTAMP WITHOUT TIME ZONE,
  deleted                   BOOLEAN NOT NULL,
  last_update               TIMESTAMP WITHOUT TIME ZONE,
  description               CHARACTER VARYING(4000),
  title                     CHARACTER VARYING(1000),
  tenant_id                 INTEGER,
  owner_fk                  INTEGER,
  full_access_group_ids     CHARACTER VARYING(255),
  full_access_user_ids      CHARACTER VARYING(255),
  minimal_access_group_ids  CHARACTER VARYING(255),
  minimal_access_user_ids   CHARACTER VARYING(255),
  readonly_access_group_ids CHARACTER VARYING(255),
  readonly_access_user_ids  CHARACTER VARYING(255)
);

CREATE TABLE t_addressbook_address (
  address_id     INTEGER NOT NULL,
  addressbook_id INTEGER NOT NULL
);

CREATE TABLE t_book (
  pk                 INTEGER               NOT NULL,
  created            TIMESTAMP WITHOUT TIME ZONE,
  deleted            BOOLEAN               NOT NULL,
  last_update        TIMESTAMP WITHOUT TIME ZONE,
  abstract_text      CHARACTER VARYING(4000),
  authors            CHARACTER VARYING(1000),
  comment            CHARACTER VARYING(1000),
  editor             CHARACTER VARYING(255),
  isbn               CHARACTER VARYING(255),
  keywords           CHARACTER VARYING(1024),
  lend_out_comment   CHARACTER VARYING(1024),
  lend_out_date      TIMESTAMP WITHOUT TIME ZONE,
  publisher          CHARACTER VARYING(255),
  signature          CHARACTER VARYING(255),
  status             CHARACTER VARYING(20) NOT NULL,
  title              CHARACTER VARYING(255),
  book_type          CHARACTER VARYING(20),
  year_of_publishing CHARACTER VARYING(4),
  tenant_id          INTEGER,
  lend_out_by        INTEGER,
  task_id            INTEGER               NOT NULL
);

CREATE TABLE t_configuration (
  pk                INTEGER                NOT NULL,
  created           TIMESTAMP WITHOUT TIME ZONE,
  deleted           BOOLEAN                NOT NULL,
  last_update       TIMESTAMP WITHOUT TIME ZONE,
  configurationtype CHARACTER VARYING(20)  NOT NULL,
  floatvalue        NUMERIC(18, 5),
  is_global         BOOLEAN DEFAULT FALSE,
  intvalue          INTEGER,
  parameter         CHARACTER VARYING(255) NOT NULL,
  stringvalue       CHARACTER VARYING(4000),
  tenant_id         INTEGER
);

CREATE TABLE t_contact (
  pk                     INTEGER                NOT NULL,
  created                TIMESTAMP WITHOUT TIME ZONE,
  deleted                BOOLEAN                NOT NULL,
  last_update            TIMESTAMP WITHOUT TIME ZONE,
  address_status         CHARACTER VARYING(20)  NOT NULL,
  birthday               DATE,
  comment                CHARACTER VARYING(5000),
  communication_language CHARACTER VARYING(255),
  contact_status         CHARACTER VARYING(20)  NOT NULL,
  division               CHARACTER VARYING(255),
  emailvalues            CHARACTER VARYING(255),
  fingerprint            CHARACTER VARYING(255),
  first_name             CHARACTER VARYING(255),
  form                   CHARACTER VARYING(10),
  name                   CHARACTER VARYING(255) NOT NULL,
  organization           CHARACTER VARYING(255),
  phonevalues            CHARACTER VARYING(255),
  positiontext           CHARACTER VARYING(255),
  public_key             CHARACTER VARYING(7000),
  socialmediavalues      CHARACTER VARYING(255),
  title                  CHARACTER VARYING(255),
  website                CHARACTER VARYING(255),
  tenant_id              INTEGER,
  task_id                INTEGER
);

CREATE TABLE t_contactentry (
  pk           INTEGER               NOT NULL,
  created      TIMESTAMP WITHOUT TIME ZONE,
  deleted      BOOLEAN               NOT NULL,
  last_update  TIMESTAMP WITHOUT TIME ZONE,
  city         CHARACTER VARYING(255),
  contact_type CHARACTER VARYING(15) NOT NULL,
  country      CHARACTER VARYING(255),
  number       SMALLINT,
  state        CHARACTER VARYING(255),
  street       CHARACTER VARYING(255),
  zipcode      CHARACTER VARYING(255),
  tenant_id    INTEGER,
  contact_id   INTEGER               NOT NULL
);

CREATE TABLE t_contract (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  co_contractor_a      CHARACTER VARYING(1000),
  co_contractor_b      CHARACTER VARYING(1000),
  contract_person_a    CHARACTER VARYING(1000),
  contract_person_b    CHARACTER VARYING(1000),
  c_date               DATE,
  due_date             DATE,
  filing               CHARACTER VARYING(1000),
  number               INTEGER,
  reference            CHARACTER VARYING(1000),
  resubmission_on_date DATE,
  signer_a             CHARACTER VARYING(1000),
  signer_b             CHARACTER VARYING(1000),
  signing_date         DATE,
  status               CHARACTER VARYING(100),
  text                 CHARACTER VARYING(4000),
  title                CHARACTER VARYING(1000),
  type                 CHARACTER VARYING(100),
  valid_from           DATE,
  valid_until          DATE,
  tenant_id            INTEGER
);

CREATE TABLE t_database_update (
  update_date         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  description         CHARACTER VARYING(4000),
  execution_result    CHARACTER VARYING(1000),
  region_id           CHARACTER VARYING(1000),
  version             CHARACTER VARYING(15),
  executed_by_user_fk INTEGER                     NOT NULL
);

CREATE TABLE t_employee_vacation (
  pk              INTEGER               NOT NULL,
  created         TIMESTAMP WITHOUT TIME ZONE,
  deleted         BOOLEAN               NOT NULL,
  last_update     TIMESTAMP WITHOUT TIME ZONE,
  end_date        DATE                  NOT NULL,
  is_special      BOOLEAN               NOT NULL,
  start_date      DATE                  NOT NULL,
  vacation_status CHARACTER VARYING(30) NOT NULL,
  tenant_id       INTEGER,
  employee_id     INTEGER               NOT NULL,
  manager_id      INTEGER               NOT NULL,
  is_half_day     BOOLEAN
);


CREATE TABLE t_employee_vacation_calendar (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  tenant_id   INTEGER,
  calendar_id INTEGER NOT NULL,
  event_id    INTEGER,
  vacation_id INTEGER NOT NULL
);

CREATE TABLE t_employee_vacation_substitution (
  vacation_id     INTEGER NOT NULL,
  substitution_id INTEGER NOT NULL
);

CREATE TABLE t_fibu_auftrag (
  pk                          INTEGER NOT NULL,
  created                     TIMESTAMP WITHOUT TIME ZONE,
  deleted                     BOOLEAN NOT NULL,
  last_update                 TIMESTAMP WITHOUT TIME ZONE,
  angebots_datum              DATE,
  status                      CHARACTER VARYING(30),
  beauftragungs_beschreibung  CHARACTER VARYING(4000),
  beauftragungs_datum         DATE,
  bemerkung                   CHARACTER VARYING(4000),
  bindungs_frist              DATE,
  kunde_text                  CHARACTER VARYING(1000),
  nummer                      INTEGER NOT NULL,
  period_of_performance_begin DATE,
  period_of_performance_end   DATE,
  referenz                    CHARACTER VARYING(255),
  status_beschreibung         CHARACTER VARYING(4000),
  titel                       CHARACTER VARYING(1000),
  ui_status_as_xml            CHARACTER VARYING(10000),
  tenant_id                   INTEGER,
  contact_person_fk           INTEGER,
  kunde_fk                    INTEGER,
  projekt_fk                  INTEGER,
  entscheidungs_datum         DATE,
  erfassungs_datum            DATE,
  probability_of_occurrence   INTEGER,
  headofbusinessmanager_fk    INTEGER,
  salesmanager_fk             INTEGER,
  projectmanager_fk           INTEGER
);

CREATE TABLE t_fibu_auftrag_position (
  pk                          INTEGER NOT NULL,
  created                     TIMESTAMP WITHOUT TIME ZONE,
  deleted                     BOOLEAN NOT NULL,
  last_update                 TIMESTAMP WITHOUT TIME ZONE,
  art                         CHARACTER VARYING(30),
  bemerkung                   CHARACTER VARYING(4000),
  mode_of_payment_type        CHARACTER VARYING(13),
  netto_summe                 NUMERIC(12, 2),
  number                      SMALLINT,
  period_of_performance_begin DATE,
  period_of_performance_end   DATE,
  period_of_performance_type  CHARACTER VARYING(10),
  person_days                 NUMERIC(12, 2),
  status                      CHARACTER VARYING(30),
  titel                       CHARACTER VARYING(255),
  vollstaendig_fakturiert     BOOLEAN NOT NULL,
  tenant_id                   INTEGER,
  auftrag_fk                  INTEGER NOT NULL,
  task_fk                     INTEGER,
  paymenttype                 CHARACTER VARYING(30)
);

CREATE TABLE t_fibu_buchungssatz (
  pk            INTEGER                     NOT NULL,
  created       TIMESTAMP WITHOUT TIME ZONE,
  deleted       BOOLEAN                     NOT NULL,
  last_update   TIMESTAMP WITHOUT TIME ZONE,
  beleg         CHARACTER VARYING(255),
  betrag        NUMERIC(18, 2)              NOT NULL,
  comment       CHARACTER VARYING(4000),
  datum         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  menge         CHARACTER VARYING(255),
  month         INTEGER                     NOT NULL,
  satznr        INTEGER                     NOT NULL,
  sh            CHARACTER VARYING(7)        NOT NULL,
  buchungstext  CHARACTER VARYING(255),
  year          INTEGER                     NOT NULL,
  tenant_id     INTEGER,
  gegenkonto_id INTEGER                     NOT NULL,
  konto_id      INTEGER                     NOT NULL,
  kost1_id      INTEGER                     NOT NULL,
  kost2_id      INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_eingangsrechnung (
  pk               INTEGER NOT NULL,
  created          TIMESTAMP WITHOUT TIME ZONE,
  deleted          BOOLEAN NOT NULL,
  last_update      TIMESTAMP WITHOUT TIME ZONE,
  bemerkung        CHARACTER VARYING(4000),
  besonderheiten   CHARACTER VARYING(4000),
  betreff          CHARACTER VARYING(4000),
  bezahl_datum     DATE,
  datum            DATE    NOT NULL,
  faelligkeit      DATE,
  ui_status_as_xml CHARACTER VARYING(10000),
  zahl_betrag      NUMERIC(12, 2),
  kreditor         CHARACTER VARYING(255),
  payment_type     CHARACTER VARYING(20),
  referenz         CHARACTER VARYING(1000),
  tenant_id        INTEGER,
  konto_id         INTEGER,
  bic              CHARACTER VARYING(11),
  iban             CHARACTER VARYING(50),
  receiver         CHARACTER VARYING(255),
  customernr       CHARACTER VARYING(255),
  discountmaturity DATE,
  discountpercent  NUMERIC(19, 2)
);

CREATE TABLE t_fibu_eingangsrechnung_position (
  pk                  INTEGER NOT NULL,
  created             TIMESTAMP WITHOUT TIME ZONE,
  deleted             BOOLEAN NOT NULL,
  last_update         TIMESTAMP WITHOUT TIME ZONE,
  einzel_netto        NUMERIC(18, 2),
  menge               NUMERIC(18, 5),
  number              SMALLINT,
  s_text              CHARACTER VARYING(1000),
  vat                 NUMERIC(10, 5),
  tenant_id           INTEGER,
  eingangsrechnung_fk INTEGER NOT NULL
);

CREATE TABLE t_fibu_employee (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  abteilung            CHARACTER VARYING(255),
  account_holder       CHARACTER VARYING(255),
  austritt             TIMESTAMP WITHOUT TIME ZONE,
  bic                  CHARACTER VARYING(11),
  birthday             TIMESTAMP WITHOUT TIME ZONE,
  city                 CHARACTER VARYING(255),
  comment              CHARACTER VARYING(4000),
  country              CHARACTER VARYING(255),
  eintritt             TIMESTAMP WITHOUT TIME ZONE,
  gender               INTEGER,
  iban                 CHARACTER VARYING(50),
  position_text        CHARACTER VARYING(244),
  staffnumber          CHARACTER VARYING(255),
  state                CHARACTER VARYING(255),
  employee_status      CHARACTER VARYING(30),
  street               CHARACTER VARYING(255),
  urlaubstage          INTEGER,
  weekly_working_hours NUMERIC(10, 5),
  zipcode              CHARACTER VARYING(255),
  tenant_id            INTEGER,
  kost1_id             INTEGER,
  user_id              INTEGER NOT NULL
);

CREATE TABLE t_fibu_employee_attr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_employee_attrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_employee_salary (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  brutto_mit_ag_anteil NUMERIC(12, 2),
  comment              CHARACTER VARYING(4000),
  month                INTEGER,
  type                 CHARACTER VARYING(20),
  year                 INTEGER,
  tenant_id            INTEGER,
  employee_id          INTEGER NOT NULL
);

CREATE TABLE t_fibu_employee_timed (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  group_name    CHARACTER VARYING(255)      NOT NULL,
  start_time    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  employee_id   INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_employee_timedattr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_employee_timedattrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_fibu_konto (
  pk          INTEGER                NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  bezeichnung CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(4000),
  nummer      INTEGER                NOT NULL,
  status      CHARACTER VARYING(10),
  tenant_id   INTEGER
);

CREATE TABLE t_fibu_kost1 (
  pk                  INTEGER NOT NULL,
  created             TIMESTAMP WITHOUT TIME ZONE,
  deleted             BOOLEAN NOT NULL,
  last_update         TIMESTAMP WITHOUT TIME ZONE,
  bereich             INTEGER,
  description         CHARACTER VARYING(4000),
  endziffer           INTEGER,
  kostentraegerstatus CHARACTER VARYING(30),
  nummernkreis        INTEGER,
  teilbereich         INTEGER,
  tenant_id           INTEGER
);

CREATE TABLE t_fibu_kost2 (
  pk                  INTEGER NOT NULL,
  created             TIMESTAMP WITHOUT TIME ZONE,
  deleted             BOOLEAN NOT NULL,
  last_update         TIMESTAMP WITHOUT TIME ZONE,
  bereich             INTEGER,
  comment             CHARACTER VARYING(4000),
  description         CHARACTER VARYING(4000),
  kostentraegerstatus CHARACTER VARYING(30),
  nummernkreis        INTEGER,
  teilbereich         INTEGER,
  work_fraction       NUMERIC(10, 5),
  tenant_id           INTEGER,
  kost2_art_id        INTEGER NOT NULL,
  projekt_id          INTEGER
);

CREATE TABLE t_fibu_kost2art (
  pk               INTEGER                NOT NULL,
  created          TIMESTAMP WITHOUT TIME ZONE,
  deleted          BOOLEAN                NOT NULL,
  last_update      TIMESTAMP WITHOUT TIME ZONE,
  description      CHARACTER VARYING(5000),
  fakturiert       BOOLEAN                NOT NULL,
  name             CHARACTER VARYING(255) NOT NULL,
  projekt_standard BOOLEAN,
  work_fraction    NUMERIC(10, 5),
  tenant_id        INTEGER
);

CREATE TABLE t_fibu_kost_zuweisung (
  pk                       INTEGER NOT NULL,
  created                  TIMESTAMP WITHOUT TIME ZONE,
  deleted                  BOOLEAN NOT NULL,
  last_update              TIMESTAMP WITHOUT TIME ZONE,
  comment                  CHARACTER VARYING(4000),
  index                    SMALLINT,
  netto                    NUMERIC(12, 2),
  tenant_id                INTEGER,
  eingangsrechnungs_pos_fk INTEGER,
  employee_salary_fk       INTEGER,
  kost1_fk                 INTEGER NOT NULL,
  kost2_fk                 INTEGER NOT NULL,
  rechnungs_pos_fk         INTEGER
);

CREATE TABLE t_fibu_kunde (
  pk          INTEGER                NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  description CHARACTER VARYING(4000),
  division    CHARACTER VARYING(255),
  identifier  CHARACTER VARYING(20),
  name        CHARACTER VARYING(255) NOT NULL,
  status      CHARACTER VARYING(30),
  tenant_id   INTEGER,
  konto_id    INTEGER
);

CREATE TABLE t_fibu_payment_schedule (
  pk                      INTEGER NOT NULL,
  created                 TIMESTAMP WITHOUT TIME ZONE,
  deleted                 BOOLEAN NOT NULL,
  last_update             TIMESTAMP WITHOUT TIME ZONE,
  amount                  NUMERIC(12, 2),
  comment                 CHARACTER VARYING(255),
  number                  SMALLINT,
  reached                 BOOLEAN,
  schedule_date           DATE,
  vollstaendig_fakturiert BOOLEAN NOT NULL,
  tenant_id               INTEGER,
  auftrag_id              INTEGER NOT NULL,
  position_number         SMALLINT
);

CREATE TABLE t_fibu_projekt (
  pk                       INTEGER                NOT NULL,
  created                  TIMESTAMP WITHOUT TIME ZONE,
  deleted                  BOOLEAN                NOT NULL,
  last_update              TIMESTAMP WITHOUT TIME ZONE,
  description              CHARACTER VARYING(4000),
  identifier               CHARACTER VARYING(20),
  intern_kost2_4           INTEGER,
  name                     CHARACTER VARYING(255) NOT NULL,
  nummer                   INTEGER                NOT NULL,
  status                   CHARACTER VARYING(30),
  tenant_id                INTEGER,
  konto_id                 INTEGER,
  kunde_id                 INTEGER,
  projektmanager_group_fk  INTEGER,
  task_fk                  INTEGER,
  headofbusinessmanager_fk INTEGER,
  salesmanager_fk          INTEGER,
  projectmanager_fk        INTEGER
);

CREATE TABLE t_fibu_rechnung (
  pk                          INTEGER NOT NULL,
  created                     TIMESTAMP WITHOUT TIME ZONE,
  deleted                     BOOLEAN NOT NULL,
  last_update                 TIMESTAMP WITHOUT TIME ZONE,
  bemerkung                   CHARACTER VARYING(4000),
  besonderheiten              CHARACTER VARYING(4000),
  betreff                     CHARACTER VARYING(4000),
  bezahl_datum                DATE,
  datum                       DATE    NOT NULL,
  faelligkeit                 DATE,
  ui_status_as_xml            CHARACTER VARYING(10000),
  zahl_betrag                 NUMERIC(12, 2),
  kunde_text                  CHARACTER VARYING(255),
  nummer                      INTEGER,
  status                      CHARACTER VARYING(30),
  typ                         CHARACTER VARYING(40),
  tenant_id                   INTEGER,
  konto_id                    INTEGER,
  kunde_id                    INTEGER,
  projekt_id                  INTEGER,
  bic                         CHARACTER VARYING(11),
  iban                        CHARACTER VARYING(50),
  receiver                    CHARACTER VARYING(255),
  discountmaturity            DATE,
  discountpercent             NUMERIC(19, 2),
  period_of_performance_begin DATE,
  period_of_performance_end   DATE,
  customerref1                CHARACTER VARYING(255),
  customeraddress             CHARACTER VARYING(255),
  attachment                  CHARACTER VARYING(255)
);

CREATE TABLE t_fibu_rechnung_position (
  pk                          INTEGER NOT NULL,
  created                     TIMESTAMP WITHOUT TIME ZONE,
  deleted                     BOOLEAN NOT NULL,
  last_update                 TIMESTAMP WITHOUT TIME ZONE,
  einzel_netto                NUMERIC(18, 2),
  menge                       NUMERIC(18, 5),
  number                      SMALLINT,
  s_text                      CHARACTER VARYING(1000),
  vat                         NUMERIC(10, 5),
  tenant_id                   INTEGER,
  auftrags_position_fk        INTEGER,
  rechnung_fk                 INTEGER NOT NULL,
  period_of_performance_begin DATE,
  period_of_performance_end   DATE,
  period_of_performance_type  CHARACTER VARYING(10)
);

CREATE TABLE t_gantt_chart (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  gantt_objects_as_xml CHARACTER VARYING(10000),
  name                 CHARACTER VARYING(1000),
  read_access          CHARACTER VARYING(16),
  settings_as_xml      CHARACTER VARYING(10000),
  style_as_xml         CHARACTER VARYING(10000),
  write_access         CHARACTER VARYING(16),
  tenant_id            INTEGER,
  owner_fk             INTEGER,
  task_fk              INTEGER NOT NULL
);

CREATE TABLE t_group (
  pk           INTEGER NOT NULL,
  created      TIMESTAMP WITHOUT TIME ZONE,
  deleted      BOOLEAN NOT NULL,
  last_update  TIMESTAMP WITHOUT TIME ZONE,
  description  CHARACTER VARYING(1000),
  ldap_values  CHARACTER VARYING(4000),
  local_group  BOOLEAN NOT NULL,
  name         CHARACTER VARYING(100),
  organization CHARACTER VARYING(100),
  tenant_id    INTEGER
);

CREATE TABLE t_group_task_access (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  description CHARACTER VARYING(4000),
  recursive   BOOLEAN,
  tenant_id   INTEGER,
  group_id    INTEGER,
  task_id     INTEGER
);

CREATE TABLE t_group_task_access_entry (
  pk                   INTEGER NOT NULL,
  access_delete        BOOLEAN,
  access_insert        BOOLEAN,
  access_select        BOOLEAN,
  access_type          CHARACTER VARYING(255),
  access_update        BOOLEAN,
  tenant_id            INTEGER,
  group_task_access_fk INTEGER
);

CREATE TABLE t_group_user (
  group_id INTEGER NOT NULL,
  user_id  INTEGER NOT NULL
);

CREATE TABLE t_hr_planning (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  week        DATE    NOT NULL,
  tenant_id   INTEGER,
  user_fk     INTEGER NOT NULL
);

CREATE TABLE t_hr_planning_entry (
  pk              INTEGER NOT NULL,
  created         TIMESTAMP WITHOUT TIME ZONE,
  deleted         BOOLEAN NOT NULL,
  last_update     TIMESTAMP WITHOUT TIME ZONE,
  description     CHARACTER VARYING(4000),
  fridayhours     NUMERIC(5, 2),
  mondayhours     NUMERIC(5, 2),
  priority        CHARACTER VARYING(20),
  probability     INTEGER,
  status          CHARACTER VARYING(20),
  thursdayhours   NUMERIC(5, 2),
  tuesdayhours    NUMERIC(5, 2),
  unassignedhours NUMERIC(5, 2),
  wednesdayhours  NUMERIC(5, 2),
  weekendhours    NUMERIC(5, 2),
  tenant_id       INTEGER,
  planning_fk     INTEGER NOT NULL,
  projekt_fk      INTEGER
);

CREATE TABLE t_imported_meb_entry (
  pk          INTEGER                     NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                     NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  check_sum   CHARACTER VARYING(255)      NOT NULL,
  date        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  sender      CHARACTER VARYING(255)      NOT NULL,
  source      CHARACTER VARYING(10),
  tenant_id   INTEGER
);

CREATE TABLE t_meb_entry (
  pk          INTEGER                     NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                     NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  date        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  message     CHARACTER VARYING(4000),
  sender      CHARACTER VARYING(255)      NOT NULL,
  status      CHARACTER VARYING(20)       NOT NULL,
  tenant_id   INTEGER,
  owner_fk    INTEGER
);

CREATE TABLE t_orga_postausgang (
  pk          INTEGER                 NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                 NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  bemerkung   CHARACTER VARYING(4000),
  datum       DATE                    NOT NULL,
  empfaenger  CHARACTER VARYING(1000) NOT NULL,
  inhalt      CHARACTER VARYING(1000),
  person      CHARACTER VARYING(1000),
  post_type   CHARACTER VARYING(100)  NOT NULL,
  tenant_id   INTEGER
);

CREATE TABLE t_orga_posteingang (
  pk          INTEGER                 NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                 NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  absender    CHARACTER VARYING(1000) NOT NULL,
  bemerkung   CHARACTER VARYING(4000),
  datum       DATE                    NOT NULL,
  inhalt      CHARACTER VARYING(1000),
  person      CHARACTER VARYING(1000),
  post_type   CHARACTER VARYING(20)   NOT NULL,
  tenant_id   INTEGER
);

CREATE TABLE t_orga_visitorbook (
  pk           INTEGER                NOT NULL,
  created      TIMESTAMP WITHOUT TIME ZONE,
  deleted      BOOLEAN                NOT NULL,
  last_update  TIMESTAMP WITHOUT TIME ZONE,
  company      CHARACTER VARYING(255),
  firstname    CHARACTER VARYING(30)  NOT NULL,
  lastname     CHARACTER VARYING(30)  NOT NULL,
  visitor_type CHARACTER VARYING(255) NOT NULL,
  tenant_id    INTEGER
);

CREATE TABLE t_orga_visitorbook_employee (
  visitorbook_id INTEGER NOT NULL,
  employee_id    INTEGER NOT NULL
);

CREATE TABLE t_orga_visitorbook_timed (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  group_name    CHARACTER VARYING(255)      NOT NULL,
  start_time    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  visitor_id    INTEGER                     NOT NULL
);

CREATE TABLE t_orga_visitorbook_timedattr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL
);

CREATE TABLE t_orga_visitorbook_timedattrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_personal_address (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  business_phone       BOOLEAN NOT NULL,
  favorite_card        BOOLEAN NOT NULL,
  fax                  BOOLEAN NOT NULL,
  mobile_phone         BOOLEAN NOT NULL,
  private_mobile_phone BOOLEAN NOT NULL,
  private_phone        BOOLEAN NOT NULL,
  tenant_id            INTEGER,
  address_id           INTEGER NOT NULL,
  owner_id             INTEGER NOT NULL
);

CREATE TABLE t_personal_contact (
  pk                   INTEGER NOT NULL,
  created              TIMESTAMP WITHOUT TIME ZONE,
  deleted              BOOLEAN NOT NULL,
  last_update          TIMESTAMP WITHOUT TIME ZONE,
  business_phone       BOOLEAN NOT NULL,
  favorite_card        BOOLEAN NOT NULL,
  fax                  BOOLEAN NOT NULL,
  mobile_phone         BOOLEAN NOT NULL,
  private_mobile_phone BOOLEAN NOT NULL,
  private_phone        BOOLEAN NOT NULL,
  tenant_id            INTEGER,
  contact_id           INTEGER NOT NULL,
  owner_id             INTEGER NOT NULL
);

CREATE TABLE t_pf_history (
  pk             BIGINT                      NOT NULL,
  createdat      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby      CHARACTER VARYING(60)       NOT NULL,
  modifiedat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby     CHARACTER VARYING(60)       NOT NULL,
  updatecounter  INTEGER                     NOT NULL,
  entity_id      BIGINT                      NOT NULL,
  entity_name    CHARACTER VARYING(255)      NOT NULL,
  entity_optype  CHARACTER VARYING(32),
  transaction_id CHARACTER VARYING(64),
  user_comment   CHARACTER VARYING(2000)
);

CREATE TABLE t_pf_history_attr (
  withdata            CHARACTER(1)                NOT NULL,
  pk                  BIGINT                      NOT NULL,
  createdat           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby           CHARACTER VARYING(60)       NOT NULL,
  modifiedat          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby          CHARACTER VARYING(60)       NOT NULL,
  updatecounter       INTEGER                     NOT NULL,
  value               CHARACTER VARYING(3000),
  propertyname        CHARACTER VARYING(255)      NOT NULL,
  type                CHARACTER(1)                NOT NULL,
  property_type_class CHARACTER VARYING(128),
  master_fk           BIGINT                      NOT NULL
);

CREATE TABLE t_pf_history_attr_data (
  pk            BIGINT                      NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_pk     BIGINT                      NOT NULL
);

CREATE TABLE t_pf_user (
  pk                         INTEGER                NOT NULL,
  created                    TIMESTAMP WITHOUT TIME ZONE,
  deleted                    BOOLEAN                NOT NULL,
  last_update                TIMESTAMP WITHOUT TIME ZONE,
  authentication_token       CHARACTER VARYING(100),
  date_format                CHARACTER VARYING(20),
  deactivated                BOOLEAN                NOT NULL,
  description                CHARACTER VARYING(255),
  email                      CHARACTER VARYING(255),
  excel_date_format          CHARACTER VARYING(20),
  first_day_of_week          INTEGER,
  firstname                  CHARACTER VARYING(255),
  hr_planning                BOOLEAN                NOT NULL,
  jira_username              CHARACTER VARYING(100),
  lastlogin                  TIMESTAMP WITHOUT TIME ZONE,
  last_password_change       TIMESTAMP WITHOUT TIME ZONE,
  lastname                   CHARACTER VARYING(255),
  ldap_values                CHARACTER VARYING(4000),
  local_user                 BOOLEAN                NOT NULL,
  locale                     CHARACTER VARYING(255),
  loginfailures              INTEGER,
  organization               CHARACTER VARYING(255),
  password                   CHARACTER VARYING(50),
  password_salt              CHARACTER VARYING(40),
  personal_meb_identifiers   CHARACTER VARYING(255),
  personal_phone_identifiers CHARACTER VARYING(255),
  restricted_user            BOOLEAN                NOT NULL,
  ssh_public_key             CHARACTER VARYING(4096),
  stay_logged_in_key         CHARACTER VARYING(255),
  super_admin                BOOLEAN DEFAULT FALSE  NOT NULL,
  time_notation              CHARACTER VARYING(6),
  time_zone                  CHARACTER VARYING(255),
  username                   CHARACTER VARYING(255) NOT NULL,
  tenant_id                  INTEGER,
  last_wlan_password_change  TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE t_plugin_bank_account (
  pk                       INTEGER                NOT NULL,
  created                  TIMESTAMP WITHOUT TIME ZONE,
  deleted                  BOOLEAN                NOT NULL,
  last_update              TIMESTAMP WITHOUT TIME ZONE,
  account_number           CHARACTER VARYING(255) NOT NULL,
  bank                     CHARACTER VARYING(255),
  bank_identification_code CHARACTER VARYING(100),
  description              CHARACTER VARYING(4000),
  name                     CHARACTER VARYING(255),
  tenant_id                INTEGER
);

CREATE TABLE t_plugin_bank_account_balance (
  pk          INTEGER        NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN        NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  amount      NUMERIC(18, 5) NOT NULL,
  date_col    DATE           NOT NULL,
  description CHARACTER VARYING(4000),
  tenant_id   INTEGER,
  account_fk  INTEGER        NOT NULL
);

CREATE TABLE t_plugin_bank_account_record (
  pk          INTEGER        NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN        NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  amount      NUMERIC(18, 5) NOT NULL,
  date_col    DATE           NOT NULL,
  text        CHARACTER VARYING(255),
  tenant_id   INTEGER,
  account_fk  INTEGER        NOT NULL
);

CREATE TABLE t_plugin_calendar_event (
  pk                          INTEGER                NOT NULL,
  created                     TIMESTAMP WITHOUT TIME ZONE,
  deleted                     BOOLEAN                NOT NULL,
  last_update                 TIMESTAMP WITHOUT TIME ZONE,
  all_day                     BOOLEAN,
  end_date                    TIMESTAMP WITHOUT TIME ZONE,
  external_uid                CHARACTER VARYING(255),
  last_email                  TIMESTAMP WITHOUT TIME ZONE,
  location                    CHARACTER VARYING(1000),
  note                        CHARACTER VARYING(4000),
  organizer                   CHARACTER VARYING(1000),
  recurrence_date             CHARACTER VARYING(255),
  recurrence_ex_date          CHARACTER VARYING(4000),
  recurrence_reference_id     CHARACTER VARYING(255),
  recurrence_rule             CHARACTER VARYING(4000),
  recurrence_until            TIMESTAMP WITHOUT TIME ZONE,
  reminder_action_type        CHARACTER VARYING(255),
  reminder_duration           INTEGER,
  reminder_duration_unit      CHARACTER VARYING(255),
  sequence                    INTEGER,
  start_date                  TIMESTAMP WITHOUT TIME ZONE,
  subject                     CHARACTER VARYING(1000),
  tenant_id                   INTEGER,
  calendar_fk                 INTEGER                NOT NULL,
  uid                         CHARACTER VARYING(255) NOT NULL,
  team_event_fk_creator       INTEGER,
  dt_stamp                    TIMESTAMP WITHOUT TIME ZONE,
  organizer_additional_params CHARACTER VARYING(1000),
  ownership                   BOOLEAN
);

CREATE TABLE t_plugin_calendar_event_attendee (
  pk                  INTEGER NOT NULL,
  created             TIMESTAMP WITHOUT TIME ZONE,
  deleted             BOOLEAN NOT NULL,
  last_update         TIMESTAMP WITHOUT TIME ZONE,
  comment             CHARACTER VARYING(4000),
  comment_of_attendee CHARACTER VARYING(4000),
  login_token         CHARACTER VARYING(255),
  number              SMALLINT,
  status              CHARACTER VARYING(100),
  url                 CHARACTER VARYING(255),
  tenant_id           INTEGER,
  address_id          INTEGER,
  user_id             INTEGER,
  team_event_fk       INTEGER,
  additional_params   CHARACTER VARYING(1000),
  common_name         CHARACTER VARYING(256),
  cu_type             CHARACTER VARYING(20),
  role                CHARACTER VARYING(255),
  rsvp                BOOLEAN
);

CREATE TABLE t_plugin_employee_configuration (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  tenant_id   INTEGER
);

CREATE TABLE t_plugin_employee_configuration_attr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL
);

CREATE TABLE t_plugin_employee_configuration_attrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_plugin_employee_configuration_timed (
  pk                        INTEGER                     NOT NULL,
  createdat                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby                 CHARACTER VARYING(60)       NOT NULL,
  modifiedat                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby                CHARACTER VARYING(60)       NOT NULL,
  updatecounter             INTEGER                     NOT NULL,
  group_name                CHARACTER VARYING(255)      NOT NULL,
  start_time                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  employee_configuration_id INTEGER                     NOT NULL
);

CREATE TABLE t_plugin_employee_configuration_timedattr (
  withdata      CHARACTER(1)                NOT NULL,
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  value         CHARACTER VARYING(3000),
  propertyname  CHARACTER VARYING(255)      NOT NULL,
  type          CHARACTER(1)                NOT NULL,
  parent        INTEGER                     NOT NULL,
  datarow       INTEGER
);

CREATE TABLE t_plugin_employee_configuration_timedattrdata (
  pk            INTEGER                     NOT NULL,
  createdat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby     CHARACTER VARYING(60)       NOT NULL,
  modifiedat    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby    CHARACTER VARYING(60)       NOT NULL,
  updatecounter INTEGER                     NOT NULL,
  datacol       CHARACTER VARYING(2990),
  datarow       INTEGER                     NOT NULL,
  parent_id     INTEGER                     NOT NULL
);

CREATE TABLE t_plugin_financialfairplay_accounting (
  pk               INTEGER        NOT NULL,
  created          TIMESTAMP WITHOUT TIME ZONE,
  deleted          BOOLEAN        NOT NULL,
  last_update      TIMESTAMP WITHOUT TIME ZONE,
  value            NUMERIC(19, 2) NOT NULL,
  weighting        NUMERIC(19, 2) NOT NULL,
  tenant_id        INTEGER,
  event_id         INTEGER,
  comment          CHARACTER VARYING(255),
  attendee_user_id INTEGER
);

CREATE TABLE t_plugin_financialfairplay_debt (
  pk                    INTEGER        NOT NULL,
  created               TIMESTAMP WITHOUT TIME ZONE,
  deleted               BOOLEAN        NOT NULL,
  last_update           TIMESTAMP WITHOUT TIME ZONE,
  approvedbyfrom        BOOLEAN        NOT NULL,
  approvedbyto          BOOLEAN        NOT NULL,
  value                 NUMERIC(19, 2) NOT NULL,
  tenant_id             INTEGER,
  event_id              INTEGER,
  attendee_user_id_from INTEGER,
  attendee_user_id_to   INTEGER
);

CREATE TABLE t_plugin_financialfairplay_event (
  pk                INTEGER                 NOT NULL,
  created           TIMESTAMP WITHOUT TIME ZONE,
  deleted           BOOLEAN                 NOT NULL,
  last_update       TIMESTAMP WITHOUT TIME ZONE,
  eventdate         DATE                    NOT NULL,
  finished          BOOLEAN,
  title             CHARACTER VARYING(1000) NOT NULL,
  tenant_id         INTEGER,
  commondebtvalue   NUMERIC(19, 2),
  organizer_user_id INTEGER
);

CREATE TABLE t_plugin_financialfairplay_event_attendee (
  event_pk         INTEGER NOT NULL,
  attendee_user_pk INTEGER
);

CREATE TABLE t_plugin_liqui_entry (
  pk              INTEGER NOT NULL,
  created         TIMESTAMP WITHOUT TIME ZONE,
  deleted         BOOLEAN NOT NULL,
  last_update     TIMESTAMP WITHOUT TIME ZONE,
  amount          NUMERIC(12, 2),
  comment         CHARACTER VARYING(4000),
  date_of_payment DATE,
  paid            BOOLEAN,
  subject         CHARACTER VARYING(1000),
  tenant_id       INTEGER
);

CREATE TABLE t_plugin_marketing_address_campaign (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  comment     CHARACTER VARYING(4000),
  title       CHARACTER VARYING(1000),
  s_values    CHARACTER VARYING(1000),
  tenant_id   INTEGER
);

CREATE TABLE t_plugin_marketing_address_campaign_value (
  pk                  INTEGER NOT NULL,
  created             TIMESTAMP WITHOUT TIME ZONE,
  deleted             BOOLEAN NOT NULL,
  last_update         TIMESTAMP WITHOUT TIME ZONE,
  comment             CHARACTER VARYING(4000),
  value               CHARACTER VARYING(100),
  tenant_id           INTEGER,
  address_fk          INTEGER NOT NULL,
  address_campaign_fk INTEGER NOT NULL
);

CREATE TABLE t_plugin_memo (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  memo        CHARACTER VARYING(4000),
  subject     CHARACTER VARYING(1000),
  tenant_id   INTEGER,
  owner_fk    INTEGER
);

CREATE TABLE t_plugin_plugintemplate (
  pk          INTEGER                NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  key         CHARACTER VARYING(255) NOT NULL,
  value       CHARACTER VARYING(255),
  tenant_id   INTEGER
);

CREATE TABLE t_plugin_poll (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  active      BOOLEAN,
  description CHARACTER VARYING(255),
  location    CHARACTER VARYING(255),
  title       CHARACTER VARYING(255),
  tenant_id   INTEGER,
  owner_fk    INTEGER
);

CREATE TABLE t_plugin_poll_attendee (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  email       CHARACTER VARYING(255),
  securekey   CHARACTER VARYING(255),
  tenant_id   INTEGER,
  poll_fk     INTEGER,
  user_fk     INTEGER
);

CREATE TABLE t_plugin_poll_event (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  enddate     TIMESTAMP WITHOUT TIME ZONE,
  startdate   TIMESTAMP WITHOUT TIME ZONE,
  tenant_id   INTEGER,
  poll_fk     INTEGER
);

CREATE TABLE t_plugin_poll_result (
  pk               INTEGER NOT NULL,
  created          TIMESTAMP WITHOUT TIME ZONE,
  deleted          BOOLEAN NOT NULL,
  last_update      TIMESTAMP WITHOUT TIME ZONE,
  result           BOOLEAN,
  tenant_id        INTEGER,
  poll_attendee_fk INTEGER,
  poll_event_fk    INTEGER
);

CREATE TABLE t_plugin_skill (
  pk                        INTEGER NOT NULL,
  created                   TIMESTAMP WITHOUT TIME ZONE,
  deleted                   BOOLEAN NOT NULL,
  last_update               TIMESTAMP WITHOUT TIME ZONE,
  comment                   CHARACTER VARYING(4000),
  description               CHARACTER VARYING(4000),
  full_access_group_ids     CHARACTER VARYING(4000),
  rateable                  BOOLEAN,
  readonly_access_group_ids CHARACTER VARYING(4000),
  title                     CHARACTER VARYING(255),
  training_access_group_ids CHARACTER VARYING(4000),
  tenant_id                 INTEGER,
  parent_fk                 INTEGER
);

CREATE TABLE t_plugin_skill_rating (
  pk              INTEGER NOT NULL,
  created         TIMESTAMP WITHOUT TIME ZONE,
  deleted         BOOLEAN NOT NULL,
  last_update     TIMESTAMP WITHOUT TIME ZONE,
  certificates    CHARACTER VARYING(4000),
  comment         CHARACTER VARYING(4000),
  description     CHARACTER VARYING(4000),
  since_year      INTEGER,
  skill_rating    CHARACTER VARYING(15),
  trainingcourses CHARACTER VARYING(4000),
  tenant_id       INTEGER,
  skill_fk        INTEGER,
  user_fk         INTEGER
);

CREATE TABLE t_plugin_skill_training (
  pk                        INTEGER NOT NULL,
  created                   TIMESTAMP WITHOUT TIME ZONE,
  deleted                   BOOLEAN NOT NULL,
  last_update               TIMESTAMP WITHOUT TIME ZONE,
  certificate               CHARACTER VARYING(4000),
  description               CHARACTER VARYING(4000),
  end_date                  DATE,
  full_access_group_ids     CHARACTER VARYING(4000),
  rating                    CHARACTER VARYING(255),
  readonly_access_group_ids CHARACTER VARYING(4000),
  start_date                DATE,
  title                     CHARACTER VARYING(255),
  tenant_id                 INTEGER,
  skill_fk                  INTEGER
);

CREATE TABLE t_plugin_skill_training_attendee (
  pk          INTEGER NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  certificate CHARACTER VARYING(4000),
  description CHARACTER VARYING(4000),
  end_date    DATE,
  rating      CHARACTER VARYING(1000),
  start_date  DATE,
  tenant_id   INTEGER,
  attendee_fk INTEGER,
  training_fk INTEGER
);

CREATE TABLE t_plugin_todo (
  pk           INTEGER NOT NULL,
  created      TIMESTAMP WITHOUT TIME ZONE,
  deleted      BOOLEAN NOT NULL,
  last_update  TIMESTAMP WITHOUT TIME ZONE,
  comment      CHARACTER VARYING(4000),
  description  CHARACTER VARYING(4000),
  due_date     DATE,
  priority     CHARACTER VARYING(20),
  recent       BOOLEAN,
  resubmission DATE,
  status       CHARACTER VARYING(20),
  subject      CHARACTER VARYING(1000),
  type         CHARACTER VARYING(20),
  tenant_id    INTEGER,
  assignee_fk  INTEGER,
  group_id     INTEGER,
  reporter_fk  INTEGER,
  task_id      INTEGER
);

CREATE TABLE t_task (
  pk                       INTEGER               NOT NULL,
  created                  TIMESTAMP WITHOUT TIME ZONE,
  deleted                  BOOLEAN               NOT NULL,
  last_update              TIMESTAMP WITHOUT TIME ZONE,
  description              CHARACTER VARYING(4000),
  duration                 NUMERIC(10, 2),
  end_date                 TIMESTAMP WITHOUT TIME ZONE,
  gantt_type               CHARACTER VARYING(10),
  gantt_predecessor_offset INTEGER,
  gantt_rel_type           CHARACTER VARYING(15),
  kost2_black_white_list   CHARACTER VARYING(1024),
  kost2_is_black_list      BOOLEAN               NOT NULL,
  max_hours                INTEGER,
  old_kost2_id             INTEGER,
  priority                 CHARACTER VARYING(7),
  progress                 INTEGER,
  protect_timesheets_until TIMESTAMP WITHOUT TIME ZONE,
  protectionofprivacy      BOOLEAN DEFAULT FALSE NOT NULL,
  reference                CHARACTER VARYING(1000),
  short_description        CHARACTER VARYING(255),
  start_date               TIMESTAMP WITHOUT TIME ZONE,
  status                   CHARACTER VARYING(1),
  timesheet_booking_status CHARACTER VARYING(20) NOT NULL,
  title                    CHARACTER VARYING(40) NOT NULL,
  workpackage_code         CHARACTER VARYING(100),
  tenant_id                INTEGER,
  gantt_predecessor_fk     INTEGER,
  parent_task_id           INTEGER,
  responsible_user_id      INTEGER
);

CREATE TABLE t_tenant (
  pk             INTEGER NOT NULL,
  created        TIMESTAMP WITHOUT TIME ZONE,
  deleted        BOOLEAN NOT NULL,
  last_update    TIMESTAMP WITHOUT TIME ZONE,
  default_tenant BOOLEAN,
  description    CHARACTER VARYING(4000),
  name           CHARACTER VARYING(255),
  shortname      CHARACTER VARYING(100),
  tenant_id      INTEGER
);

CREATE TABLE t_tenant_user (
  tenant_id INTEGER NOT NULL,
  user_id   INTEGER NOT NULL
);

CREATE TABLE t_timesheet (
  pk          INTEGER                     NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                     NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  description CHARACTER VARYING(4000),
  location    CHARACTER VARYING(100),
  start_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  stop_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  time_zone   CHARACTER VARYING(100),
  tenant_id   INTEGER,
  kost2_id    INTEGER,
  task_id     INTEGER                     NOT NULL,
  user_id     INTEGER                     NOT NULL
);

CREATE TABLE t_user_pref (
  pk          INTEGER                NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN                NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  area        CHARACTER VARYING(20)  NOT NULL,
  name        CHARACTER VARYING(255) NOT NULL,
  tenant_id   INTEGER,
  user_fk     INTEGER                NOT NULL
);

CREATE TABLE t_user_pref_entry (
  pk           INTEGER NOT NULL,
  parameter    CHARACTER VARYING(255),
  s_value      CHARACTER VARYING(10000),
  tenant_id    INTEGER,
  user_pref_fk INTEGER
);

CREATE TABLE t_user_right (
  pk          INTEGER               NOT NULL,
  created     TIMESTAMP WITHOUT TIME ZONE,
  deleted     BOOLEAN               NOT NULL,
  last_update TIMESTAMP WITHOUT TIME ZONE,
  right_id    CHARACTER VARYING(40) NOT NULL,
  value       CHARACTER VARYING(40),
  tenant_id   INTEGER,
  user_fk     INTEGER               NOT NULL
);

CREATE TABLE t_user_xml_prefs (
  pk                 INTEGER NOT NULL,
  created            TIMESTAMP WITHOUT TIME ZONE,
  key                CHARACTER VARYING(1000),
  last_update        TIMESTAMP WITHOUT TIME ZONE,
  serializedsettings CHARACTER VARYING(10000),
  version            INTEGER,
  tenant_id          INTEGER,
  user_id            INTEGER NOT NULL
);

CREATE TABLE tb_base_ghistory (
  base_ghistory  BIGINT                      NOT NULL,
  createdat      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby      CHARACTER VARYING(60)       NOT NULL,
  modifiedat     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby     CHARACTER VARYING(60)       NOT NULL,
  updatecounter  INTEGER                     NOT NULL,
  entity_id      BIGINT                      NOT NULL,
  entity_name    CHARACTER VARYING(255)      NOT NULL,
  entity_optype  CHARACTER VARYING(32),
  transaction_id CHARACTER VARYING(64),
  user_comment   CHARACTER VARYING(2000)
);

CREATE TABLE tb_base_ghistory_attr (
  withdata            CHARACTER(1)                NOT NULL,
  base_ghistory_attr  BIGINT                      NOT NULL,
  createdat           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby           CHARACTER VARYING(60)       NOT NULL,
  modifiedat          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby          CHARACTER VARYING(60)       NOT NULL,
  updatecounter       INTEGER                     NOT NULL,
  value               CHARACTER VARYING(3000),
  propertyname        CHARACTER VARYING(255)      NOT NULL,
  type                CHARACTER(1)                NOT NULL,
  property_type_class CHARACTER VARYING(128),
  master_fk           BIGINT                      NOT NULL
);

CREATE TABLE tb_base_ghistory_attr_data (
  base_ghistory_attr_data BIGINT                      NOT NULL,
  createdat               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  createdby               CHARACTER VARYING(60)       NOT NULL,
  modifiedat              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  modifiedby              CHARACTER VARYING(60)       NOT NULL,
  updatecounter           INTEGER                     NOT NULL,
  datacol                 CHARACTER VARYING(2990),
  datarow                 INTEGER                     NOT NULL,
  parent_pk               BIGINT                      NOT NULL
);

ALTER TABLE t_address_attr
  ADD CONSTRAINT t_address_attr_pkey PRIMARY KEY (pk);

ALTER TABLE t_address_attrdata
  ADD CONSTRAINT t_address_attrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_addressbook_address
  ADD CONSTRAINT t_addressbook_address_pkey PRIMARY KEY (address_id, addressbook_id);

ALTER TABLE t_addressbook
  ADD CONSTRAINT t_addressbook_pkey PRIMARY KEY (pk);

ALTER TABLE t_book
  ADD CONSTRAINT t_book_pkey PRIMARY KEY (pk);

ALTER TABLE t_configuration
  ADD CONSTRAINT t_configuration_pkey PRIMARY KEY (pk);

ALTER TABLE t_contact
  ADD CONSTRAINT t_contact_pkey PRIMARY KEY (pk);

ALTER TABLE t_contactentry
  ADD CONSTRAINT t_contactentry_pkey PRIMARY KEY (pk);

ALTER TABLE t_contract
  ADD CONSTRAINT t_contract_pkey PRIMARY KEY (pk);

ALTER TABLE t_database_update
  ADD CONSTRAINT t_database_update_pkey PRIMARY KEY (update_date);

ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT t_employee_vacation_calendar_pkey PRIMARY KEY (pk);

ALTER TABLE t_employee_vacation
  ADD CONSTRAINT t_employee_vacation_pkey PRIMARY KEY (pk);

ALTER TABLE t_employee_vacation_substitution
  ADD CONSTRAINT t_employee_vacation_substitution_pkey PRIMARY KEY (vacation_id, substitution_id);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT t_fibu_auftrag_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_auftrag_position
  ADD CONSTRAINT t_fibu_auftrag_position_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT t_fibu_buchungssatz_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_eingangsrechnung
  ADD CONSTRAINT t_fibu_eingangsrechnung_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_eingangsrechnung_position
  ADD CONSTRAINT t_fibu_eingangsrechnung_position_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_attr
  ADD CONSTRAINT t_fibu_employee_attr_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_attrdata
  ADD CONSTRAINT t_fibu_employee_attrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee
  ADD CONSTRAINT t_fibu_employee_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_salary
  ADD CONSTRAINT t_fibu_employee_salary_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_timed
  ADD CONSTRAINT t_fibu_employee_timed_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_timedattr
  ADD CONSTRAINT t_fibu_employee_timedattr_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_employee_timedattrdata
  ADD CONSTRAINT t_fibu_employee_timedattrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_konto
  ADD CONSTRAINT t_fibu_konto_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_kost1
  ADD CONSTRAINT t_fibu_kost1_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_kost2
  ADD CONSTRAINT t_fibu_kost2_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_kost2art
  ADD CONSTRAINT t_fibu_kost2art_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT t_fibu_kost_zuweisung_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_kunde
  ADD CONSTRAINT t_fibu_kunde_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_payment_schedule
  ADD CONSTRAINT t_fibu_payment_schedule_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT t_fibu_projekt_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT t_fibu_rechnung_pkey PRIMARY KEY (pk);

ALTER TABLE t_fibu_rechnung_position
  ADD CONSTRAINT t_fibu_rechnung_position_pkey PRIMARY KEY (pk);

ALTER TABLE t_gantt_chart
  ADD CONSTRAINT t_gantt_chart_pkey PRIMARY KEY (pk);

ALTER TABLE t_group
  ADD CONSTRAINT t_group_pkey PRIMARY KEY (pk);

ALTER TABLE t_group_task_access_entry
  ADD CONSTRAINT t_group_task_access_entry_pkey PRIMARY KEY (pk);

ALTER TABLE t_group_task_access
  ADD CONSTRAINT t_group_task_access_pkey PRIMARY KEY (pk);

ALTER TABLE t_group_user
  ADD CONSTRAINT t_group_user_pkey PRIMARY KEY (group_id, user_id);

ALTER TABLE t_hr_planning_entry
  ADD CONSTRAINT t_hr_planning_entry_pkey PRIMARY KEY (pk);

ALTER TABLE t_hr_planning
  ADD CONSTRAINT t_hr_planning_pkey PRIMARY KEY (pk);

ALTER TABLE t_imported_meb_entry
  ADD CONSTRAINT t_imported_meb_entry_pkey PRIMARY KEY (pk);

ALTER TABLE t_meb_entry
  ADD CONSTRAINT t_meb_entry_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_postausgang
  ADD CONSTRAINT t_orga_postausgang_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_posteingang
  ADD CONSTRAINT t_orga_posteingang_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_visitorbook_employee
  ADD CONSTRAINT t_orga_visitorbook_employee_pkey PRIMARY KEY (visitorbook_id, employee_id);

ALTER TABLE t_orga_visitorbook
  ADD CONSTRAINT t_orga_visitorbook_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_visitorbook_timed
  ADD CONSTRAINT t_orga_visitorbook_timed_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_visitorbook_timedattr
  ADD CONSTRAINT t_orga_visitorbook_timedattr_pkey PRIMARY KEY (pk);

ALTER TABLE t_orga_visitorbook_timedattrdata
  ADD CONSTRAINT t_orga_visitorbook_timedattrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_personal_address
  ADD CONSTRAINT t_personal_address_pkey PRIMARY KEY (pk);

ALTER TABLE t_personal_contact
  ADD CONSTRAINT t_personal_contact_pkey PRIMARY KEY (pk);

ALTER TABLE t_pf_history_attr_data
  ADD CONSTRAINT t_pf_history_attr_data_pkey PRIMARY KEY (pk);

ALTER TABLE t_pf_history_attr
  ADD CONSTRAINT t_pf_history_attr_pkey PRIMARY KEY (pk);

ALTER TABLE t_pf_history
  ADD CONSTRAINT t_pf_history_pkey PRIMARY KEY (pk);

ALTER TABLE t_pf_user
  ADD CONSTRAINT t_pf_user_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_bank_account_balance
  ADD CONSTRAINT t_plugin_bank_account_balance_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_bank_account
  ADD CONSTRAINT t_plugin_bank_account_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_bank_account_record
  ADD CONSTRAINT t_plugin_bank_account_record_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_calendar_event_attendee
  ADD CONSTRAINT t_plugin_calendar_event_attendee_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_calendar_event
  ADD CONSTRAINT t_plugin_calendar_event_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration_attr
  ADD CONSTRAINT t_plugin_employee_configuration_attr_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration_attrdata
  ADD CONSTRAINT t_plugin_employee_configuration_attrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration
  ADD CONSTRAINT t_plugin_employee_configuration_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration_timed
  ADD CONSTRAINT t_plugin_employee_configuration_timed_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration_timedattr
  ADD CONSTRAINT t_plugin_employee_configuration_timedattr_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_employee_configuration_timedattrdata
  ADD CONSTRAINT t_plugin_employee_configuration_timedattrdata_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_financialfairplay_accounting
  ADD CONSTRAINT t_plugin_financialfairplay_accounting_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT t_plugin_financialfairplay_debt_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_financialfairplay_event
  ADD CONSTRAINT t_plugin_financialfairplay_event_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_liqui_entry
  ADD CONSTRAINT t_plugin_liqui_entry_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_marketing_address_campaign
  ADD CONSTRAINT t_plugin_marketing_address_campaign_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_marketing_address_campaign_value
  ADD CONSTRAINT t_plugin_marketing_address_campaign_value_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_memo
  ADD CONSTRAINT t_plugin_memo_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_plugintemplate
  ADD CONSTRAINT t_plugin_plugintemplate_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_poll_attendee
  ADD CONSTRAINT t_plugin_poll_attendee_pkey PRIMARY KEY (pk);

ALTER TABLE t_plugin_poll_event
  ADD CONSTRAINT t_plugin_poll_event_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_poll
  ADD CONSTRAINT t_plugin_poll_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_poll_result
  ADD CONSTRAINT t_plugin_poll_result_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_skill
  ADD CONSTRAINT t_plugin_skill_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_skill_rating
  ADD CONSTRAINT t_plugin_skill_rating_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_skill_training_attendee
  ADD CONSTRAINT t_plugin_skill_training_attendee_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_skill_training
  ADD CONSTRAINT t_plugin_skill_training_pkey PRIMARY KEY (pk);


ALTER TABLE t_plugin_todo
  ADD CONSTRAINT t_plugin_todo_pkey PRIMARY KEY (pk);


ALTER TABLE t_task
  ADD CONSTRAINT t_task_pkey PRIMARY KEY (pk);


ALTER TABLE t_tenant
  ADD CONSTRAINT t_tenant_pkey PRIMARY KEY (pk);


ALTER TABLE t_tenant_user
  ADD CONSTRAINT t_tenant_user_pkey PRIMARY KEY (tenant_id, user_id);


ALTER TABLE t_timesheet
  ADD CONSTRAINT t_timesheet_pkey PRIMARY KEY (pk);


ALTER TABLE t_user_pref_entry
  ADD CONSTRAINT t_user_pref_entry_pkey PRIMARY KEY (pk);


ALTER TABLE t_user_pref
  ADD CONSTRAINT t_user_pref_pkey PRIMARY KEY (pk);


ALTER TABLE t_user_right
  ADD CONSTRAINT t_user_right_pkey PRIMARY KEY (pk);


ALTER TABLE t_user_xml_prefs
  ADD CONSTRAINT t_user_xml_prefs_pkey PRIMARY KEY (pk);


ALTER TABLE tb_base_ghistory_attr_data
  ADD CONSTRAINT tb_base_ghistory_attr_data_pkey PRIMARY KEY (base_ghistory_attr_data);


ALTER TABLE tb_base_ghistory_attr
  ADD CONSTRAINT tb_base_ghistory_attr_pkey PRIMARY KEY (base_ghistory_attr);


ALTER TABLE tb_base_ghistory
  ADD CONSTRAINT tb_base_ghistory_pkey PRIMARY KEY (base_ghistory);


ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT uk13cp590ny6so721hxtxg3ywe8 UNIQUE (nummer, tenant_id);


ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT uk18n5qocxv8v83v6pmwbyrg97y UNIQUE (event_id, attendee_user_id_from, attendee_user_id_to);


ALTER TABLE t_contract
  ADD CONSTRAINT uk1csljbvfcyc95uawchy475pyq UNIQUE (number, tenant_id);


ALTER TABLE t_group
  ADD CONSTRAINT uk20gfd8dmh2ts1tqpbfsbf3k5y UNIQUE (name, tenant_id);


ALTER TABLE t_imported_meb_entry
  ADD CONSTRAINT uk40iwr152inguwed6ndbffekdb UNIQUE (sender, date, check_sum);


ALTER TABLE t_group_task_access
  ADD CONSTRAINT uk5a3nsfk0dvmf7l0edi1x24i3d UNIQUE (group_id, task_id);


ALTER TABLE t_fibu_employee_timed
  ADD CONSTRAINT uk685q8bvv04e67aein01mnmdep UNIQUE (employee_id, group_name, start_time);


ALTER TABLE t_orga_visitorbook_timedattr
  ADD CONSTRAINT uk6ie2k7d9c4jjymbinuj3p0dtr UNIQUE (parent, propertyname);


ALTER TABLE t_fibu_auftrag_position
  ADD CONSTRAINT uk8iarwe4lp7hso7looh8o84ffm UNIQUE (auftrag_fk, number);


ALTER TABLE t_user_right
  ADD CONSTRAINT uk8iittd1cevoi4qhsen4rwaq4j UNIQUE (user_fk, right_id, tenant_id);


ALTER TABLE t_task
  ADD CONSTRAINT uka9iebxxdhoviessjiqs0ggd3c UNIQUE (parent_task_id, title);


ALTER TABLE t_hr_planning
  ADD CONSTRAINT ukaefphlrt2w0ekpr8jley6ej9w UNIQUE (user_fk, week, tenant_id);


ALTER TABLE t_fibu_kost2
  ADD CONSTRAINT ukb00lwresusebpmcjprp2i5smo UNIQUE (nummernkreis, bereich, teilbereich, kost2_art_id, tenant_id);


ALTER TABLE t_configuration
  ADD CONSTRAINT ukbrlhedvthe0dosbwnqeciyoxo UNIQUE (parameter, tenant_id);


ALTER TABLE t_pf_user
  ADD CONSTRAINT ukc0pygxqm81s78xkx23jdm44rv UNIQUE (username);


ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT ukdfpxew48ev9ivdgm56p3iys5c UNIQUE (vacation_id, calendar_id);


ALTER TABLE t_plugin_employee_configuration
  ADD CONSTRAINT uke140imnwc71t9mi4pjeysesr7 UNIQUE (tenant_id);


ALTER TABLE t_orga_visitorbook_timed
  ADD CONSTRAINT ukegcjr73jyarc2uloafisx3udo UNIQUE (visitor_id, group_name, start_time);


ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT ukel80a4o5nfg3hgymq39s44oi7 UNIQUE (nummer, kunde_id, tenant_id);


ALTER TABLE t_personal_address
  ADD CONSTRAINT ukfug33gy9pdwl0mhso3pql8kys UNIQUE (owner_id, address_id);


ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT ukg8teoe5ckqgta8cehbml2tujs UNIQUE (year, month, satznr, tenant_id);


ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT ukge3olaat4a0f7jdie8ae6em68 UNIQUE (nummer, tenant_id);


ALTER TABLE t_group_task_access_entry
  ADD CONSTRAINT ukgl5r4qk9tektks4ctybp9dfd6 UNIQUE (group_task_access_fk, access_type);


ALTER TABLE t_book
  ADD CONSTRAINT ukhcx59fnglysya535rwswhxjlx UNIQUE (signature, tenant_id);


ALTER TABLE t_personal_contact
  ADD CONSTRAINT ukiv076q5faseh0bji90eiiac6n UNIQUE (owner_id, contact_id);


ALTER TABLE t_fibu_kost1
  ADD CONSTRAINT ukk0vmq8380vd3vykfvg97tdi7b UNIQUE (nummernkreis, bereich, teilbereich, endziffer, tenant_id);


ALTER TABLE t_contactentry
  ADD CONSTRAINT ukk1qew3uptx8jdr8ejbsc9ql3d UNIQUE (contact_id, number);


ALTER TABLE t_user_pref
  ADD CONSTRAINT ukkxu311uovq47d3712rfoei47u UNIQUE (user_fk, area, name, tenant_id);


ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT ukkyctj07ir12bwl9r7rmdapdeo UNIQUE (index, rechnungs_pos_fk, kost1_fk, kost2_fk);


ALTER TABLE t_fibu_rechnung_position
  ADD CONSTRAINT ukm26g9mdosl7lhyp2yl9lmegxo UNIQUE (rechnung_fk, number);


ALTER TABLE t_user_xml_prefs
  ADD CONSTRAINT ukmmakqkkhqacgaer01nfc33ed4 UNIQUE (user_id, key, tenant_id);


ALTER TABLE t_fibu_eingangsrechnung_position
  ADD CONSTRAINT ukmw77ceaek67y2kb4k7xnsomnw UNIQUE (eingangsrechnung_fk, number);


ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT uknbb1uv9p1susfv6j4vj5i2b5j UNIQUE (index, employee_salary_fk, kost1_fk, kost2_fk);


ALTER TABLE t_plugin_bank_account
  ADD CONSTRAINT uknc8vsd8dm94yliqrd84rateyx UNIQUE (account_number, tenant_id);


ALTER TABLE t_fibu_payment_schedule
  ADD CONSTRAINT uknel6luw7jvwv07t0csv09rs3h UNIQUE (auftrag_id, number);


ALTER TABLE t_fibu_employee_salary
  ADD CONSTRAINT ukny6ovd5xylf8557h77srbariv UNIQUE (employee_id, year, month);


ALTER TABLE t_plugin_employee_configuration_timed
  ADD CONSTRAINT ukoenjbbnawcktc10pyq2rfke4k UNIQUE (employee_configuration_id, group_name, start_time);


ALTER TABLE t_user_pref_entry
  ADD CONSTRAINT ukoqjhg6yc238io26jsoyryxvwg UNIQUE (user_pref_fk, parameter, tenant_id);


ALTER TABLE t_fibu_konto
  ADD CONSTRAINT ukp1b0cyh606mpuuvfe4a0ueegl UNIQUE (nummer, tenant_id);


ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT ukpjcnxhlqkhto34uxocoef78q7 UNIQUE (nummer, intern_kost2_4, tenant_id);


ALTER TABLE t_fibu_employee
  ADD CONSTRAINT ukr59qqfs9h7kh1hybfs2kros2c UNIQUE (user_id, tenant_id);


ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT ukrb91hgk05pj49dbht6qmjt072 UNIQUE (index, eingangsrechnungs_pos_fk, kost1_fk, kost2_fk);


ALTER TABLE t_plugin_marketing_address_campaign_value
  ADD CONSTRAINT uktc7a5jtxuta0nyhhq6c3rdbmq UNIQUE (address_fk, address_campaign_fk);


ALTER TABLE t_plugin_calendar_event
  ADD CONSTRAINT unique_t_plugin_calendar_event_uid_calendar_fk UNIQUE (uid, calendar_fk);


CREATE INDEX idx_fibu_employee_timed_start_time
  ON t_fibu_employee_timed (start_time);


CREATE INDEX idx_fk_t_addressbook_address_address_id
  ON t_addressbook_address (address_id);

CREATE INDEX idx_fk_t_addressbook_address_addressbook_id
  ON t_addressbook_address (addressbook_id);

CREATE INDEX idx_fk_t_addressbook_tenant_id
  ON t_addressbook (tenant_id);

CREATE INDEX idx_fk_t_book_lend_out_by
  ON t_book (lend_out_by);

CREATE INDEX idx_fk_t_book_task_id
  ON t_book (task_id);

CREATE INDEX idx_fk_t_book_tenant_id
  ON t_book (tenant_id);

CREATE INDEX idx_fk_t_configuration_tenant_id
  ON t_configuration (tenant_id);

CREATE INDEX idx_fk_t_contact_tenant_id
  ON t_contact (tenant_id);

CREATE INDEX idx_fk_t_contactentry_tenant_id
  ON t_contactentry (tenant_id);

CREATE INDEX idx_fk_t_contract_tenant_id
  ON t_contract (tenant_id);

CREATE INDEX idx_fk_t_database_update_executed_by_user_fk
  ON t_database_update (executed_by_user_fk);

CREATE INDEX idx_fk_t_employee_vacation_substitution_substitution_id
  ON t_employee_vacation_substitution (substitution_id);

CREATE INDEX idx_fk_t_employee_vacation_substitution_vacation_id
  ON t_employee_vacation_substitution (vacation_id);

CREATE INDEX idx_fk_t_fibu_auftrag_contact_person_fk
  ON t_fibu_auftrag (contact_person_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_headofbusinessmanager_fk
  ON t_fibu_auftrag (headofbusinessmanager_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_kunde_fk
  ON t_fibu_auftrag (kunde_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_position_auftrag_fk
  ON t_fibu_auftrag_position (auftrag_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_position_task_fk
  ON t_fibu_auftrag_position (task_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_position_tenant_id
  ON t_fibu_auftrag_position (tenant_id);

CREATE INDEX idx_fk_t_fibu_auftrag_projectmanager_fk
  ON t_fibu_auftrag (projectmanager_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_projekt_fk
  ON t_fibu_auftrag (projekt_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_salesmanager_fk
  ON t_fibu_auftrag (salesmanager_fk);

CREATE INDEX idx_fk_t_fibu_auftrag_tenant_id
  ON t_fibu_auftrag (tenant_id);

CREATE INDEX idx_fk_t_fibu_buchungssatz_gegenkonto_id
  ON t_fibu_buchungssatz (gegenkonto_id);

CREATE INDEX idx_fk_t_fibu_buchungssatz_konto_id
  ON t_fibu_buchungssatz (konto_id);

CREATE INDEX idx_fk_t_fibu_buchungssatz_kost1_id
  ON t_fibu_buchungssatz (kost1_id);

CREATE INDEX idx_fk_t_fibu_buchungssatz_kost2_id
  ON t_fibu_buchungssatz (kost2_id);

CREATE INDEX idx_fk_t_fibu_buchungssatz_tenant_id
  ON t_fibu_buchungssatz (tenant_id);

CREATE INDEX idx_fk_t_fibu_eingangsrechnung_konto_id
  ON t_fibu_eingangsrechnung (konto_id);

CREATE INDEX idx_fk_t_fibu_eingangsrechnung_position_eingangsrechnung_fk
  ON t_fibu_eingangsrechnung_position (eingangsrechnung_fk);

CREATE INDEX idx_fk_t_fibu_eingangsrechnung_position_tenant_id
  ON t_fibu_eingangsrechnung_position (tenant_id);

CREATE INDEX idx_fk_t_fibu_eingangsrechnung_tenant_id
  ON t_fibu_eingangsrechnung (tenant_id);

CREATE INDEX idx_fk_t_fibu_employee_kost1_id
  ON t_fibu_employee (kost1_id);

CREATE INDEX idx_fk_t_fibu_employee_salary_employee_id
  ON t_fibu_employee_salary (employee_id);

CREATE INDEX idx_fk_t_fibu_employee_salary_tenant_id
  ON t_fibu_employee_salary (tenant_id);

CREATE INDEX idx_fk_t_fibu_employee_tenant_id
  ON t_fibu_employee (tenant_id);

CREATE INDEX idx_fk_t_fibu_employee_user_id
  ON t_fibu_employee (user_id);

CREATE INDEX idx_fk_t_fibu_konto_tenant_id
  ON t_fibu_konto (tenant_id);

CREATE INDEX idx_fk_t_fibu_kost1_tenant_id
  ON t_fibu_kost1 (tenant_id);

CREATE INDEX idx_fk_t_fibu_kost2_kost2_art_id
  ON t_fibu_kost2 (kost2_art_id);

CREATE INDEX idx_fk_t_fibu_kost2_projekt_id
  ON t_fibu_kost2 (projekt_id);

CREATE INDEX idx_fk_t_fibu_kost2_tenant_id
  ON t_fibu_kost2 (tenant_id);

CREATE INDEX idx_fk_t_fibu_kost2art_tenant_id
  ON t_fibu_kost2art (tenant_id);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_eingangsrechnungs_pos_fk
  ON t_fibu_kost_zuweisung (eingangsrechnungs_pos_fk);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_employee_salary_fk
  ON t_fibu_kost_zuweisung (employee_salary_fk);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_kost1_fk
  ON t_fibu_kost_zuweisung (kost1_fk);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_kost2_fk
  ON t_fibu_kost_zuweisung (kost2_fk);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_rechnungs_pos_fk
  ON t_fibu_kost_zuweisung (rechnungs_pos_fk);

CREATE INDEX idx_fk_t_fibu_kost_zuweisung_tenant_id
  ON t_fibu_kost_zuweisung (tenant_id);

CREATE INDEX idx_fk_t_fibu_kunde_konto_id
  ON t_fibu_kunde (konto_id);

CREATE INDEX idx_fk_t_fibu_kunde_tenant_id
  ON t_fibu_kunde (tenant_id);

CREATE INDEX idx_fk_t_fibu_payment_schedule_auftrag_id
  ON t_fibu_payment_schedule (auftrag_id);

CREATE INDEX idx_fk_t_fibu_payment_schedule_tenant_id
  ON t_fibu_payment_schedule (tenant_id);

CREATE INDEX idx_fk_t_fibu_projekt_headofbusinessmanager_fk
  ON t_fibu_projekt (headofbusinessmanager_fk);

CREATE INDEX idx_fk_t_fibu_projekt_konto_id
  ON t_fibu_projekt (konto_id);

CREATE INDEX idx_fk_t_fibu_projekt_kunde_id
  ON t_fibu_projekt (kunde_id);

CREATE INDEX idx_fk_t_fibu_projekt_projectmanager_fk
  ON t_fibu_projekt (projectmanager_fk);

CREATE INDEX idx_fk_t_fibu_projekt_projektmanager_group_fk
  ON t_fibu_projekt (projektmanager_group_fk);

CREATE INDEX idx_fk_t_fibu_projekt_salesmanager_fk
  ON t_fibu_projekt (salesmanager_fk);

CREATE INDEX idx_fk_t_fibu_projekt_task_fk
  ON t_fibu_projekt (task_fk);

CREATE INDEX idx_fk_t_fibu_projekt_tenant_id
  ON t_fibu_projekt (tenant_id);

CREATE INDEX idx_fk_t_fibu_rechnung_konto_id
  ON t_fibu_rechnung (konto_id);

CREATE INDEX idx_fk_t_fibu_rechnung_kunde_id
  ON t_fibu_rechnung (kunde_id);

CREATE INDEX idx_fk_t_fibu_rechnung_position_auftrags_position_fk
  ON t_fibu_rechnung_position (auftrags_position_fk);

CREATE INDEX idx_fk_t_fibu_rechnung_position_rechnung_fk
  ON t_fibu_rechnung_position (rechnung_fk);

CREATE INDEX idx_fk_t_fibu_rechnung_position_tenant_id
  ON t_fibu_rechnung_position (tenant_id);

CREATE INDEX idx_fk_t_fibu_rechnung_projekt_id
  ON t_fibu_rechnung (projekt_id);

CREATE INDEX idx_fk_t_fibu_rechnung_tenant_id
  ON t_fibu_rechnung (tenant_id);

CREATE INDEX idx_fk_t_gantt_chart_owner_fk
  ON t_gantt_chart (owner_fk);

CREATE INDEX idx_fk_t_gantt_chart_task_fk
  ON t_gantt_chart (task_fk);

CREATE INDEX idx_fk_t_gantt_chart_tenant_id
  ON t_gantt_chart (tenant_id);

CREATE INDEX idx_fk_t_group_task_access_entry_group_task_access_fk
  ON t_group_task_access_entry (group_task_access_fk);

CREATE INDEX idx_fk_t_group_task_access_entry_tenant_id
  ON t_group_task_access_entry (tenant_id);

CREATE INDEX idx_fk_t_group_task_access_group_id
  ON t_group_task_access (group_id);

CREATE INDEX idx_fk_t_group_task_access_task_id
  ON t_group_task_access (task_id);

CREATE INDEX idx_fk_t_group_task_access_tenant_id
  ON t_group_task_access (tenant_id);

CREATE INDEX idx_fk_t_group_tenant_id
  ON t_group (tenant_id);

CREATE INDEX idx_fk_t_group_user_group_id
  ON t_group_user (group_id);

CREATE INDEX idx_fk_t_group_user_user_id
  ON t_group_user (user_id);

CREATE INDEX idx_fk_t_hr_planning_entry_planning_fk
  ON t_hr_planning_entry (planning_fk);

CREATE INDEX idx_fk_t_hr_planning_entry_projekt_fk
  ON t_hr_planning_entry (projekt_fk);

CREATE INDEX idx_fk_t_hr_planning_entry_tenant_id
  ON t_hr_planning_entry (tenant_id);

CREATE INDEX idx_fk_t_hr_planning_tenant_id
  ON t_hr_planning (tenant_id);

CREATE INDEX idx_fk_t_hr_planning_user_fk
  ON t_hr_planning (user_fk);

CREATE INDEX idx_fk_t_imported_meb_entry_tenant_id
  ON t_imported_meb_entry (tenant_id);

CREATE INDEX idx_fk_t_meb_entry_owner_fk
  ON t_meb_entry (owner_fk);

CREATE INDEX idx_fk_t_meb_entry_tenant_id
  ON t_meb_entry (tenant_id);

CREATE INDEX idx_fk_t_orga_employee_employee_id
  ON t_orga_visitorbook_employee (employee_id);

CREATE INDEX idx_fk_t_orga_postausgang_tenant_id
  ON t_orga_postausgang (tenant_id);

CREATE INDEX idx_fk_t_orga_posteingang_tenant_id
  ON t_orga_posteingang (tenant_id);

CREATE INDEX idx_fk_t_orga_visitorbook_employee_id
  ON t_orga_visitorbook_employee (visitorbook_id);

CREATE INDEX idx_fk_t_orga_visitorbook_tenant_id
  ON t_orga_visitorbook (tenant_id);

CREATE INDEX idx_fk_t_personal_address_address_id
  ON t_personal_address (address_id);

CREATE INDEX idx_fk_t_personal_address_owner_id
  ON t_personal_address (owner_id);

CREATE INDEX idx_fk_t_personal_address_tenant_id
  ON t_personal_address (tenant_id);

CREATE INDEX idx_fk_t_personal_contact_tenant_id
  ON t_personal_contact (tenant_id);

CREATE INDEX idx_fk_t_pf_user_tenant_id
  ON t_pf_user (tenant_id);

CREATE INDEX idx_fk_t_plugin_bank_account_balance_tenant_id
  ON t_plugin_bank_account_balance (tenant_id);

CREATE INDEX idx_fk_t_plugin_bank_account_record_tenant_id
  ON t_plugin_bank_account_record (tenant_id);

CREATE INDEX idx_fk_t_plugin_bank_account_tenant_id
  ON t_plugin_bank_account (tenant_id);

CREATE INDEX idx_fk_t_plugin_calendar_event_attendee_address_id
  ON t_plugin_calendar_event_attendee (address_id);

CREATE INDEX idx_fk_t_plugin_calendar_event_attendee_team_event_fk
  ON t_plugin_calendar_event_attendee (team_event_fk);

CREATE INDEX idx_fk_t_plugin_calendar_event_attendee_tenant_id
  ON t_plugin_calendar_event_attendee (tenant_id);

CREATE INDEX idx_fk_t_plugin_calendar_event_attendee_user_id
  ON t_plugin_calendar_event_attendee (user_id);

CREATE INDEX idx_fk_t_plugin_calendar_event_calendar_fk
  ON t_plugin_calendar_event (calendar_fk);

CREATE INDEX idx_fk_t_plugin_calendar_event_tenant_id
  ON t_plugin_calendar_event (tenant_id);

CREATE INDEX idx_fk_t_plugin_financialfairplay_debt_event_id
  ON t_plugin_financialfairplay_debt (event_id);

CREATE INDEX idx_fk_t_plugin_financialfairplay_debt_from_id
  ON t_plugin_financialfairplay_debt (attendee_user_id_from);

CREATE INDEX idx_fk_t_plugin_financialfairplay_debt_to_id
  ON t_plugin_financialfairplay_debt (attendee_user_id_to);

CREATE INDEX idx_fk_t_plugin_liqui_entry_tenant_id
  ON t_plugin_liqui_entry (tenant_id);

CREATE INDEX idx_fk_t_plugin_marketing_address_campaign_tenant_id
  ON t_plugin_marketing_address_campaign (tenant_id);

CREATE INDEX idx_fk_t_plugin_marketing_address_campaign_value_address_campai
  ON t_plugin_marketing_address_campaign_value (address_campaign_fk);

CREATE INDEX idx_fk_t_plugin_marketing_address_campaign_value_address_fk
  ON t_plugin_marketing_address_campaign_value (address_fk);

CREATE INDEX idx_fk_t_plugin_marketing_address_campaign_value_tenant_id
  ON t_plugin_marketing_address_campaign_value (tenant_id);

CREATE INDEX idx_fk_t_plugin_memo_owner_fk
  ON t_plugin_memo (owner_fk);

CREATE INDEX idx_fk_t_plugin_memo_tenant_id
  ON t_plugin_memo (tenant_id);

CREATE INDEX idx_fk_t_plugin_poll_attendee_tenant_id
  ON t_plugin_poll_attendee (tenant_id);

CREATE INDEX idx_fk_t_plugin_poll_event_tenant_id
  ON t_plugin_poll_event (tenant_id);

CREATE INDEX idx_fk_t_plugin_poll_result_tenant_id
  ON t_plugin_poll_result (tenant_id);

CREATE INDEX idx_fk_t_plugin_poll_tenant_id
  ON t_plugin_poll (tenant_id);

CREATE INDEX idx_fk_t_plugin_skill_parent_fk
  ON t_plugin_skill (parent_fk);

CREATE INDEX idx_fk_t_plugin_skill_rating_skill_fk
  ON t_plugin_skill_rating (skill_fk);

CREATE INDEX idx_fk_t_plugin_skill_rating_tenant_id
  ON t_plugin_skill_rating (tenant_id);

CREATE INDEX idx_fk_t_plugin_skill_rating_user_fk
  ON t_plugin_skill_rating (user_fk);

CREATE INDEX idx_fk_t_plugin_skill_tenant_id
  ON t_plugin_skill (tenant_id);

CREATE INDEX idx_fk_t_plugin_skill_training_attendee_attendee_fk
  ON t_plugin_skill_training_attendee (attendee_fk);

CREATE INDEX idx_fk_t_plugin_skill_training_attendee_tenant_id
  ON t_plugin_skill_training_attendee (tenant_id);

CREATE INDEX idx_fk_t_plugin_skill_training_attendee_training_fk
  ON t_plugin_skill_training_attendee (training_fk);

CREATE INDEX idx_fk_t_plugin_skill_training_skill_fk
  ON t_plugin_skill_training (skill_fk);

CREATE INDEX idx_fk_t_plugin_skill_training_tenant_id
  ON t_plugin_skill_training (tenant_id);

CREATE INDEX idx_fk_t_plugin_todo_assignee_fk
  ON t_plugin_todo (assignee_fk);

CREATE INDEX idx_fk_t_plugin_todo_group_id
  ON t_plugin_todo (group_id);

CREATE INDEX idx_fk_t_plugin_todo_reporter_fk
  ON t_plugin_todo (reporter_fk);

CREATE INDEX idx_fk_t_plugin_todo_task_id
  ON t_plugin_todo (task_id);

CREATE INDEX idx_fk_t_plugin_todo_tenant_id
  ON t_plugin_todo (tenant_id);

CREATE INDEX idx_fk_t_task_gantt_predecessor_fk
  ON t_task (gantt_predecessor_fk);

CREATE INDEX idx_fk_t_task_parent_task_id
  ON t_task (parent_task_id);

CREATE INDEX idx_fk_t_task_responsible_user_id
  ON t_task (responsible_user_id);

CREATE INDEX idx_fk_t_task_tenant_id
  ON t_task (tenant_id);

CREATE INDEX idx_fk_t_timesheet_kost2_id
  ON t_timesheet (kost2_id);

CREATE INDEX idx_fk_t_timesheet_task_id
  ON t_timesheet (task_id);

CREATE INDEX idx_fk_t_timesheet_tenant_id
  ON t_timesheet (tenant_id);

CREATE INDEX idx_fk_t_timesheet_user_id
  ON t_timesheet (user_id);

CREATE INDEX idx_fk_t_user_pref_entry_tenant_id
  ON t_user_pref_entry (tenant_id);

CREATE INDEX idx_fk_t_user_pref_entry_user_pref_fk
  ON t_user_pref_entry (user_pref_fk);

CREATE INDEX idx_fk_t_user_pref_tenant_id
  ON t_user_pref (tenant_id);

CREATE INDEX idx_fk_t_user_pref_user_fk
  ON t_user_pref (user_fk);

CREATE INDEX idx_fk_t_user_right_tenant_id
  ON t_user_right (tenant_id);

CREATE INDEX idx_fk_t_user_right_user_fk
  ON t_user_right (user_fk);

CREATE INDEX idx_fk_t_user_xml_prefs_tenant_id
  ON t_user_xml_prefs (tenant_id);

CREATE INDEX idx_fk_t_user_xml_prefs_user_id
  ON t_user_xml_prefs (user_id);

CREATE INDEX idx_fk_t_vacation_employee_id
  ON t_employee_vacation (employee_id);

CREATE INDEX idx_fk_t_vacation_manager_id
  ON t_employee_vacation (manager_id);

CREATE INDEX idx_fk_t_vacation_tenant_id
  ON t_employee_vacation (tenant_id);

CREATE INDEX idx_orga_visitorbook_timed_start_time
  ON t_orga_visitorbook_timed (start_time);

CREATE INDEX idx_plugin_employee_configuration_timed_start_time
  ON t_plugin_employee_configuration_timed (start_time);

CREATE INDEX idx_plugin_team_cal_end_date
  ON t_plugin_calendar_event (calendar_fk, end_date);

CREATE INDEX idx_plugin_team_cal_start_date
  ON t_plugin_calendar_event (calendar_fk, start_date);

CREATE INDEX idx_plugin_team_cal_time
  ON t_plugin_calendar_event (calendar_fk, start_date, end_date);

CREATE INDEX idx_timesheet_user_time
  ON t_timesheet (user_id, start_time);

CREATE INDEX ix_base_ghistory_a_d_modat
  ON tb_base_ghistory_attr_data (modifiedat);

CREATE INDEX ix_base_ghistory_a_d_parent
  ON tb_base_ghistory_attr_data (parent_pk);

CREATE INDEX ix_base_ghistory_attr_modat
  ON tb_base_ghistory_attr (modifiedat);

CREATE INDEX ix_base_ghistory_attr_mst_fk
  ON tb_base_ghistory_attr (master_fk);

CREATE INDEX ix_base_ghistory_ent
  ON tb_base_ghistory (entity_id, entity_name);

CREATE INDEX ix_base_ghistory_mod
  ON tb_base_ghistory (modifiedat);

CREATE INDEX ix_pf_history_a_d_modat
  ON t_pf_history_attr_data (modifiedat);

CREATE INDEX ix_pf_history_a_d_parent
  ON t_pf_history_attr_data (parent_pk);

CREATE INDEX ix_pf_history_attr_masterpk
  ON t_pf_history_attr (master_fk);

CREATE INDEX ix_pf_history_attr_mod
  ON t_pf_history_attr (modifiedat);

CREATE INDEX ix_pf_history_ent
  ON t_pf_history (entity_id, entity_name);

CREATE INDEX ix_pf_history_mod
  ON t_pf_history (modifiedat);

ALTER TABLE t_plugin_skill_rating
  ADD CONSTRAINT fk10doyvde45sc5v8leo13nfrpx FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_skill_rating
  ADD CONSTRAINT fk124usepbwwtb0ctdmkwjlmxt3 FOREIGN KEY (skill_fk) REFERENCES t_plugin_skill (pk);

ALTER TABLE t_tenant_user
  ADD CONSTRAINT fk1bonq67qbnpjhsjil549uq754 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag_position
  ADD CONSTRAINT fk1hgrfsui035jiky2y1fw9234w FOREIGN KEY (auftrag_fk) REFERENCES t_fibu_auftrag (pk);

ALTER TABLE t_contactentry
  ADD CONSTRAINT fk1vfu1tv4ldtutelc9ubldxhk9 FOREIGN KEY (contact_id) REFERENCES t_contact (pk);

ALTER TABLE t_plugin_marketing_address_campaign_value
  ADD CONSTRAINT fk1yonmeymp3m0gclycm8vw22dj FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag_position
  ADD CONSTRAINT fk28ka3b6ouqv84610scuxwj6ed FOREIGN KEY (task_fk) REFERENCES t_task (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fk2rrqm09vxnsvlowhjakbcs0m8 FOREIGN KEY (projekt_fk) REFERENCES t_fibu_projekt (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT fk30b0ahte2wyc73qlh6yu0o3yy FOREIGN KEY (kost2_id) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_addressbook
  ADD CONSTRAINT fk30sea3bbjy5ilwop79vrsigg FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_orga_visitorbook_timed
  ADD CONSTRAINT fk319is9ttk265y6t4t2enaow81 FOREIGN KEY (visitor_id) REFERENCES t_orga_visitorbook (pk);

ALTER TABLE t_plugin_skill_training_attendee
  ADD CONSTRAINT fk33arexyvwg7kgdw8fi8uqgwjc FOREIGN KEY (attendee_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_poll_result
  ADD CONSTRAINT fk38at8w8yv249du6mm8eoc9xat FOREIGN KEY (poll_attendee_fk) REFERENCES t_plugin_poll_attendee (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fk3bllndn9bfet6e9vd4fatutl4 FOREIGN KEY (headofbusinessmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fk3ecaqtyama1a4mk5gsjnmwlfl FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skill_rating
  ADD CONSTRAINT fk3fl84khjsy2ev180lqehm19yb FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_personal_address
  ADD CONSTRAINT fk3ivee56sxs7pvypw50vpmcggx FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fk3jd7gr0ctlx0gl94po8tj8aag FOREIGN KEY (kost1_fk) REFERENCES t_fibu_kost1 (pk);

ALTER TABLE t_fibu_employee_timed
  ADD CONSTRAINT fk3jux4mav7fjfj31oicy05y40r FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_fibu_kost2art
  ADD CONSTRAINT fk3l68een25nk9b0mccbjsv6nh6 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_financialfairplay_accounting
  ADD CONSTRAINT fk3tbha24mfscy3q0pfw1jcikft FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_todo
  ADD CONSTRAINT fk3wxji6rw963q8l25cqmqmhc69 FOREIGN KEY (group_id) REFERENCES t_group (pk);

ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT fk43qnmkepikn3vop7f1p955031 FOREIGN KEY (projekt_id) REFERENCES t_fibu_projekt (pk);

ALTER TABLE t_plugin_financialfairplay_event_attendee
  ADD CONSTRAINT fk4c8pa8tnygkina1c81g12ru94 FOREIGN KEY (attendee_user_pk) REFERENCES t_pf_user (pk);

ALTER TABLE t_employee_vacation_substitution
  ADD CONSTRAINT fk4caln86jndtvybgj6aqdb81a2 FOREIGN KEY (substitution_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_plugin_employee_configuration_timedattr
  ADD CONSTRAINT fk4eyxqu1ef1siut63hx4kp8yfl FOREIGN KEY (parent) REFERENCES t_plugin_employee_configuration_timed (pk);

ALTER TABLE t_addressbook_address
  ADD CONSTRAINT fk4g1mkloy7jfnlqxxjyr9sp2i5 FOREIGN KEY (addressbook_id) REFERENCES t_addressbook (pk);

ALTER TABLE t_plugin_bank_account_balance
  ADD CONSTRAINT fk4mf93488nnj3b2r93kwkvj5jw FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_liqui_entry
  ADD CONSTRAINT fk4uqmo8qoi4u9sd36ckl6cf2uu FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_bank_account_record
  ADD CONSTRAINT fk4vhn56y9u7eohypx6tv2xlucc FOREIGN KEY (account_fk) REFERENCES t_plugin_bank_account (pk);

ALTER TABLE t_contact
  ADD CONSTRAINT fk4w9pwm48kr6v3wchq3hj0g887 FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fk54uw3v35p5c3r7lgpx8e7u3h2 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_database_update
  ADD CONSTRAINT fk59tolnnihv7wfdjoolsn05o55 FOREIGN KEY (executed_by_user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_employee_timedattrdata
  ADD CONSTRAINT fk5fmd25gexukvykjff57sibdrq FOREIGN KEY (parent_id) REFERENCES t_fibu_employee_timedattr (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fk5hqka33xb4vi917lr8mi70yxo FOREIGN KEY (projektmanager_group_fk) REFERENCES t_group (pk);

ALTER TABLE t_contactentry
  ADD CONSTRAINT fk5mywvrw692l2wn3utcxgv3y8c FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_timesheet
  ADD CONSTRAINT fk5njybgbq5hecwhvhvtc7sbb95 FOREIGN KEY (kost2_id) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fk5p683famfq17b405rbeaxrvn2 FOREIGN KEY (eingangsrechnungs_pos_fk) REFERENCES t_fibu_eingangsrechnung_position (pk);

ALTER TABLE t_plugin_financialfairplay_accounting
  ADD CONSTRAINT fk5vv8ol898uyvakfsf7ynqmpfv FOREIGN KEY (event_id) REFERENCES t_plugin_financialfairplay_event (pk);

ALTER TABLE t_fibu_employee_salary
  ADD CONSTRAINT fk6hl8c2jaf8buda007p0kj1iht FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_plugin_todo
  ADD CONSTRAINT fk6i9ejerk8iig97rhy5csdbs5b FOREIGN KEY (assignee_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_payment_schedule
  ADD CONSTRAINT fk6ixfa3ht63mym6n3gbinv63jl FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_vacation
  ADD CONSTRAINT fk6lhr9vq8bdjqmc7in83tsu41j FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_orga_visitorbook_timedattr
  ADD CONSTRAINT fk6sa7fc7xv71ommc0cma0gn5a4 FOREIGN KEY (parent) REFERENCES t_orga_visitorbook_timed (pk);

ALTER TABLE t_group_task_access
  ADD CONSTRAINT fk6ycgo8abhgo2amtcjdj1exh7b FOREIGN KEY (group_id) REFERENCES t_group (pk);

ALTER TABLE t_group
  ADD CONSTRAINT fk70s5xmw1krvdbl1691nla6wv9 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_orga_postausgang
  ADD CONSTRAINT fk7apgd9a6vfvy8qud3dwsi8ya9 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_pref_entry
  ADD CONSTRAINT fk7e3q5u89o98sa4lc4x6p55786 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_bank_account_record
  ADD CONSTRAINT fk7w0wam5rq9nkdpe2veinh67xq FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_pref
  ADD CONSTRAINT fk86jowtxvhil4e4y18e429x60j FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_group_task_access
  ADD CONSTRAINT fk8h5ohrq1fm9kysgw69dho0hdd FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_gantt_chart
  ADD CONSTRAINT fk8n7nx53990pjae5cvmvk6rk61 FOREIGN KEY (task_fk) REFERENCES t_task (pk);

ALTER TABLE t_fibu_eingangsrechnung_position
  ADD CONSTRAINT fk8ogbe5ereu6h3h9ctdxol74tc FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_employee
  ADD CONSTRAINT fk8qnqg72miq6gwldcuq381cag2 FOREIGN KEY (kost1_id) REFERENCES t_fibu_kost1 (pk);

ALTER TABLE t_group_task_access_entry
  ADD CONSTRAINT fk8wou34jvi8io7ii3iy1cn528k FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skill_training_attendee
  ADD CONSTRAINT fk95q95y49h4eramfm3f026ytyd FOREIGN KEY (training_fk) REFERENCES t_plugin_skill_training (pk);

ALTER TABLE t_plugin_marketing_address_campaign_value
  ADD CONSTRAINT fk9gwnb9sudgftfhxm0m9ax7y2r FOREIGN KEY (address_campaign_fk) REFERENCES t_plugin_marketing_address_campaign (pk);

ALTER TABLE t_plugin_skill
  ADD CONSTRAINT fk9pxmshy73nc3n9yg057rf6my FOREIGN KEY (parent_fk) REFERENCES t_plugin_skill (pk);

ALTER TABLE t_timesheet
  ADD CONSTRAINT fk9qw4krb40bbcjiravax5mlrgt FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_task
  ADD CONSTRAINT fka293ef8l4h74fqkbkhygjt76p FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_pf_history_attr
  ADD CONSTRAINT fka4vlagongwjibr2ckp9eujc8x FOREIGN KEY (master_fk) REFERENCES t_pf_history (pk);

ALTER TABLE t_fibu_eingangsrechnung_position
  ADD CONSTRAINT fka572jv88e5g6crhx60ekb8f0l FOREIGN KEY (eingangsrechnung_fk) REFERENCES t_fibu_eingangsrechnung (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fka6s1winlipu6cifxnpt5183bf FOREIGN KEY (headofbusinessmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT fkaauopc6sv9csw59h7ndmknhlb FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fkai2ubya0spb7f1o6dfaoeucoq FOREIGN KEY (contact_person_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_group_user
  ADD CONSTRAINT fkalajxj9b4h48bmj5vqm1683sl FOREIGN KEY (group_id) REFERENCES t_group (pk);

ALTER TABLE t_meb_entry
  ADD CONSTRAINT fkb5bq9nb0gsokoq7se3d13uf9g FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_eingangsrechnung
  ADD CONSTRAINT fkb8a76u9dth3adh00eyewru26f FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE tb_base_ghistory_attr_data
  ADD CONSTRAINT fkb8qu5w3xfsmglru81ys3o9nkq FOREIGN KEY (parent_pk) REFERENCES tb_base_ghistory_attr (base_ghistory_attr);

ALTER TABLE t_fibu_kost2
  ADD CONSTRAINT fkbc9br3ehxi77kqb6hjohcnixp FOREIGN KEY (kost2_art_id) REFERENCES t_fibu_kost2art (pk);

ALTER TABLE t_tenant
  ADD CONSTRAINT fkbn8j4gf8e48nsppwap97stv7q FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_orga_visitorbook_employee
  ADD CONSTRAINT fkbt7cwtrlh1nrub7ca41na635s FOREIGN KEY (visitorbook_id) REFERENCES t_orga_visitorbook (pk);

ALTER TABLE t_plugin_poll_attendee
  ADD CONSTRAINT fkbvsey6s87hoj631aobkbjosfh FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_group_user
  ADD CONSTRAINT fkbwl4duslng18xk133in8l6jw5 FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_meb_entry
  ADD CONSTRAINT fkc675k5cbebvbwobvxsviqpofr FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_todo
  ADD CONSTRAINT fkc937e53dfcb7h2ki5b03iyn1q FOREIGN KEY (reporter_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_eingangsrechnung
  ADD CONSTRAINT fkccyd03alt7ir28od4yo6mcbvn FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_fibu_payment_schedule
  ADD CONSTRAINT fkcmmxhs80ro6nrxilcy8kheo5j FOREIGN KEY (auftrag_id) REFERENCES t_fibu_auftrag (pk);

ALTER TABLE t_imported_meb_entry
  ADD CONSTRAINT fkcn55ey8xd14ldbx38d74tl4r9 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_hr_planning
  ADD CONSTRAINT fkcvhk0bo0y2amh0xbkhnss56yt FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_plugintemplate
  ADD CONSTRAINT fkcwdgdh2orl7uv8ubmqwbiopwi FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT fkcydfdtpxnflg4fx52ywapfvkd FOREIGN KEY (gegenkonto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_hr_planning_entry
  ADD CONSTRAINT fkd2jtgn0vxn2dpkcd8rbo5ab9w FOREIGN KEY (projekt_fk) REFERENCES t_fibu_projekt (pk);

ALTER TABLE t_orga_posteingang
  ADD CONSTRAINT fkd39yiep17ocm0xulyvcq5u62r FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_task
  ADD CONSTRAINT fkd8m6pwtt15iq4xsy7s2asibjj FOREIGN KEY (responsible_user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_employee_attrdata
  ADD CONSTRAINT fkdcae11vuhbns5ij7e7b8dq7xf FOREIGN KEY (parent_id) REFERENCES t_fibu_employee_attr (pk);

ALTER TABLE t_plugin_financialfairplay_event_attendee
  ADD CONSTRAINT fkdcsig8dlf38m46xabmou9r9r FOREIGN KEY (event_pk) REFERENCES t_plugin_financialfairplay_event (pk);

ALTER TABLE t_fibu_kunde
  ADD CONSTRAINT fkdgyqfvkgfbid3eui7gq9lofuj FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_task
  ADD CONSTRAINT fkdol3ek0ikpjruyoxr1dq7xgxw FOREIGN KEY (gantt_predecessor_fk) REFERENCES t_task (pk);

ALTER TABLE t_timesheet
  ADD CONSTRAINT fkdp2ljb6dkp8tfe1tm8s1sv60p FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_skill
  ADD CONSTRAINT fkdr8bqd18d61yvbths71a73649 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_address_attrdata
  ADD CONSTRAINT fkdwvdtrnof18mscry8rikbynxs FOREIGN KEY (parent_id) REFERENCES t_address_attr (pk);

ALTER TABLE t_plugin_financialfairplay_accounting
  ADD CONSTRAINT fke8jbl631wb6dh6q144823g0uf FOREIGN KEY (attendee_user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_gantt_chart
  ADD CONSTRAINT fkeah2ckofd4ombh31d60ubmfr FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_book
  ADD CONSTRAINT fkeah2udupkpmj5m6h10sgf6vi9 FOREIGN KEY (lend_out_by) REFERENCES t_pf_user (pk);

ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT fkeb7q4hm9s8aca8eqkqatb3ktk FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag_position
  ADD CONSTRAINT fkejbwghmdlcvrmc9f23eg6fqcc FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fkekqtgs9o8e2uskghn464nrexx FOREIGN KEY (projectmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_calendar_event
  ADD CONSTRAINT fkel9ep4vmsx32lm1m4o8w2nnjs FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_poll_event
  ADD CONSTRAINT fkeshk2l3y1wllkl9h4rkdifo39 FOREIGN KEY (poll_fk) REFERENCES t_plugin_poll (pk);

ALTER TABLE t_fibu_kost2
  ADD CONSTRAINT fkeun1s5bsmu5yyuqnwi8omxva5 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_calendar_event_attendee
  ADD CONSTRAINT fkfc9dphgb8d1pmts1fcbaooqgl FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fkfpchu797or19swbel4sx108c5 FOREIGN KEY (kunde_id) REFERENCES t_fibu_kunde (pk);

ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT fkfr76jrom2wfmc87th06wmn6b8 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT fkg03ybnijdma4jqofhhtrglhho FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_konto
  ADD CONSTRAINT fkg7jftbeaym5bb4jlhp87ijpuu FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_book
  ADD CONSTRAINT fkg9fla5hy57ctrcbs28irnkbfn FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_plugin_financialfairplay_event
  ADD CONSTRAINT fkg9hnim958ql4qsvd7wvnoofsj FOREIGN KEY (organizer_user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_employee_vacation
  ADD CONSTRAINT fkganncxt6ddh9bleu5g8jqbfw8 FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_fibu_rechnung_position
  ADD CONSTRAINT fkgf8csujq7mha93fbvb4be0j0q FOREIGN KEY (rechnung_fk) REFERENCES t_fibu_rechnung (pk);

ALTER TABLE t_plugin_memo
  ADD CONSTRAINT fkgj72ignhv3m4spo048i722roy FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_poll_attendee
  ADD CONSTRAINT fkgoi46ds8idsbjg80t1xojctd8 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_right
  ADD CONSTRAINT fkgumtciv7d1n5nupol4by60v5b FOREIGN KEY (user_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_poll_result
  ADD CONSTRAINT fkgyflm266npueo6mw3md1hbndt FOREIGN KEY (poll_event_fk) REFERENCES t_plugin_poll_event (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fkhbcvg0rb51dugcy8kgp17a0uh FOREIGN KEY (task_fk) REFERENCES t_task (pk);

ALTER TABLE t_contact
  ADD CONSTRAINT fki861594sl8ac41bbgq7bi14xj FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_pf_user
  ADD CONSTRAINT fkial73db4guuxaeord33g35fkx FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT fkibo4j8pc4ddbe00c9fm8hoglc FOREIGN KEY (vacation_id) REFERENCES t_employee_vacation (pk);

ALTER TABLE t_plugin_employee_configuration_timed
  ADD CONSTRAINT fkicc9gyn6xvffu7to8rp07jbkl FOREIGN KEY (employee_configuration_id) REFERENCES t_plugin_employee_configuration (pk);

ALTER TABLE t_pf_history_attr_data
  ADD CONSTRAINT fkiegugji0jn9x63d3ptua531ha FOREIGN KEY (parent_pk) REFERENCES t_pf_history_attr (pk);

ALTER TABLE t_user_xml_prefs
  ADD CONSTRAINT fkihr7nyxl6qtnxgdhpq7gy1245 FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_personal_contact
  ADD CONSTRAINT fkivudm19oeofjaxccpa1t0ea2d FOREIGN KEY (contact_id) REFERENCES t_contact (pk);

ALTER TABLE t_plugin_bank_account_balance
  ADD CONSTRAINT fkj5iiq7v72udvn7n02phsvam62 FOREIGN KEY (account_fk) REFERENCES t_plugin_bank_account (pk);

ALTER TABLE t_timesheet
  ADD CONSTRAINT fkj7erjh4mvhqpcctqnr4ri26ge FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skill_training
  ADD CONSTRAINT fkj8ij7dci7mw0aktrr6fady9m6 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_contract
  ADD CONSTRAINT fkk6l3hexl5xsdp9d4n1f7u43v9 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_task
  ADD CONSTRAINT fkkgwgdk280drb8eepwdjddsd6c FOREIGN KEY (parent_task_id) REFERENCES t_task (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fkkkpbq5ocj226s8otkohwqs26q FOREIGN KEY (kost2_fk) REFERENCES t_fibu_kost2 (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT fkkuum3ms168cm3ha7byb9xbs52 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_hr_planning_entry
  ADD CONSTRAINT fkl0qfro9eeh4ggdktquw95r7k7 FOREIGN KEY (planning_fk) REFERENCES t_hr_planning (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fkl1ba3gbkpol368ua16bquau2l FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fkl5ut39wg20lahylhdyie4okhp FOREIGN KEY (rechnungs_pos_fk) REFERENCES t_fibu_rechnung_position (pk);

ALTER TABLE t_fibu_kost_zuweisung
  ADD CONSTRAINT fkl9opsf0tcqeythihyhyfenowr FOREIGN KEY (employee_salary_fk) REFERENCES t_fibu_employee_salary (pk);

ALTER TABLE tb_base_ghistory_attr
  ADD CONSTRAINT fkla63htvpvsdx5cqj0ymg90fdx FOREIGN KEY (master_fk) REFERENCES tb_base_ghistory (base_ghistory);

ALTER TABLE t_plugin_poll
  ADD CONSTRAINT fklbao6n6lk8irqfwqq9renkmk6 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_xml_prefs
  ADD CONSTRAINT fklbw8fm2hu8jfh167dj54s765w FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_gantt_chart
  ADD CONSTRAINT fkljqgiuns8obaaud6iten8l8cg FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_group_task_access_entry
  ADD CONSTRAINT fkllygaoxkkw2pctqn1ewxp8m8t FOREIGN KEY (group_task_access_fk) REFERENCES t_group_task_access (pk);

ALTER TABLE t_plugin_todo
  ADD CONSTRAINT fklmlfuy69hr9byx1iu8u1ftbhv FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_employee_configuration_attr
  ADD CONSTRAINT fkm0p9h71xd4un0b1f3of3yxani FOREIGN KEY (parent) REFERENCES t_plugin_employee_configuration (pk);

ALTER TABLE t_orga_visitorbook_employee
  ADD CONSTRAINT fkm3gjnouqv4ntb0fb1mjygssuw FOREIGN KEY (employee_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_plugin_calendar_event_attendee
  ADD CONSTRAINT fkm7b18u3drw8nnyusv6o7snve1 FOREIGN KEY (team_event_fk) REFERENCES t_plugin_calendar_event (pk);

ALTER TABLE t_fibu_kunde
  ADD CONSTRAINT fkm825qbtcasusa4jdqthq4yxsp FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fkm951yiv5axhq63ynwddmdalns FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_right
  ADD CONSTRAINT fkmdw3ayfdlvorvbbnqu374ig5m FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_marketing_address_campaign
  ADD CONSTRAINT fkmf8nosv7lo4vc9vgauvcl242p FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_employee
  ADD CONSTRAINT fkmfs1jm21rbo1u0vxn429pd0at FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_orga_visitorbook
  ADD CONSTRAINT fkmhrkrry5fiu6ilxjudo0bvm1g FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_pref
  ADD CONSTRAINT fkmptnfrguxsevivu7hfpsr8esx FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skill_training_attendee
  ADD CONSTRAINT fkmrjnuqk26rf2plcy9yx8vaunv FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_kost2
  ADD CONSTRAINT fkmtpd28tj3olkblpw8jrvl0tsm FOREIGN KEY (projekt_id) REFERENCES t_fibu_projekt (pk);

ALTER TABLE t_employee_vacation_substitution
  ADD CONSTRAINT fkmyquxvs3hqhx6hdhgi2s6rim9 FOREIGN KEY (vacation_id) REFERENCES t_employee_vacation (pk);

ALTER TABLE t_fibu_kost1
  ADD CONSTRAINT fkn5agvsedakdwjs01olhxyx6c7 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fkn7vnx3jexem05ak7f808oygcg FOREIGN KEY (kunde_fk) REFERENCES t_fibu_kunde (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT fknevxk2si050ss6dxotxdpcqmi FOREIGN KEY (konto_id) REFERENCES t_fibu_konto (pk);

ALTER TABLE t_fibu_employee_salary
  ADD CONSTRAINT fkneycgjos15k5wqi3k2x1pt0go FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_calendar_event_attendee
  ADD CONSTRAINT fknoqv6rc28lmv29nl9joxd8loc FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_financialfairplay_event
  ADD CONSTRAINT fknso1qxr3v2kt0v5ryt9b2s91t FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_personal_address
  ADD CONSTRAINT fknux21a4rx6rn0x5n5pf3maohv FOREIGN KEY (owner_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_orga_visitorbook_timedattrdata
  ADD CONSTRAINT fknxdf86k0y9okbp9e1c21rgytn FOREIGN KEY (parent_id) REFERENCES t_orga_visitorbook_timedattr (pk);

ALTER TABLE t_plugin_employee_configuration
  ADD CONSTRAINT fko05jp5ihj00643avp3mw0ody FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_todo
  ADD CONSTRAINT fko296sbdijr6l3luhnnm0llede FOREIGN KEY (task_id) REFERENCES t_task (pk);

ALTER TABLE t_plugin_memo
  ADD CONSTRAINT fko6ub13xpknmrxc8402idlxw0d FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_calendar_event
  ADD CONSTRAINT fko9lj06ubf7g0cun9kv6hx7cje FOREIGN KEY (team_event_fk_creator) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_employee_timedattr
  ADD CONSTRAINT fkoncm1oec6ku9rxnfo6m96iag0 FOREIGN KEY (parent) REFERENCES t_fibu_employee_timed (pk);

ALTER TABLE t_hr_planning_entry
  ADD CONSTRAINT fkp7yycvp096km666uo43mvt27q FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_user_pref_entry
  ADD CONSTRAINT fkpffo6p7c2w2t1l3vpnqrgn14n FOREIGN KEY (user_pref_fk) REFERENCES t_user_pref (pk);

ALTER TABLE t_fibu_rechnung_position
  ADD CONSTRAINT fkphb6sjhmprf2bbexxoy56mm9c FOREIGN KEY (auftrags_position_fk) REFERENCES t_fibu_auftrag_position (pk);

ALTER TABLE t_fibu_auftrag
  ADD CONSTRAINT fkpl3tr1v1xyx71j6l0ct2thlq6 FOREIGN KEY (salesmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_rechnung
  ADD CONSTRAINT fkppeyvne5bxoxlu4hrl15ruev8 FOREIGN KEY (kunde_id) REFERENCES t_fibu_kunde (pk);

ALTER TABLE t_group_task_access
  ADD CONSTRAINT fkptk99nywlblnvx59i9y01b3yp FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_poll_attendee
  ADD CONSTRAINT fkqa55vla8htmhx3ts8e7p8ixq0 FOREIGN KEY (poll_fk) REFERENCES t_plugin_poll (pk);

ALTER TABLE t_plugin_poll_result
  ADD CONSTRAINT fkqiffsk0kebsh6ljvs2a0pcmgv FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_employee_vacation_calendar
  ADD CONSTRAINT fkqjgb7ru1uk5q1mns9w623dwcj FOREIGN KEY (event_id) REFERENCES t_plugin_calendar_event (pk);

ALTER TABLE t_employee_vacation
  ADD CONSTRAINT fkqlnhnc4cgyymcyohtdxy9gjn0 FOREIGN KEY (manager_id) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_personal_contact
  ADD CONSTRAINT fkqpqenrw1qh8vkighel6t81c70 FOREIGN KEY (owner_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_employee
  ADD CONSTRAINT fkquspdhdh4p0ruix99xc8e7o79 FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_employee_configuration_timedattrdata
  ADD CONSTRAINT fkqvvnrk9s2fmhr08n2ig5lu6v0 FOREIGN KEY (parent_id) REFERENCES t_plugin_employee_configuration_timedattr (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fkr7q530pbwf3gfdjj01ucesnja FOREIGN KEY (projectmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT fkrbma5seuqx5cppiwvsp710k4h FOREIGN KEY (attendee_user_id_to) REFERENCES t_pf_user (pk);

ALTER TABLE t_book
  ADD CONSTRAINT fkrc77oyb84c7y6tvh5mkhldol5 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT fkrgkmxgc3ggxwqh6knlgtmkxy2 FOREIGN KEY (attendee_user_id_from) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_projekt
  ADD CONSTRAINT fks1ewyx39y163jyf409vnyrsmn FOREIGN KEY (salesmanager_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_personal_contact
  ADD CONSTRAINT fks1qqtx3e4putvmruw6lj8tvab FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_configuration
  ADD CONSTRAINT fks2wharbcljkwm4n60e67bg47l FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_fibu_employee_attr
  ADD CONSTRAINT fks4kpshe7cjwk30doa9jhmn593 FOREIGN KEY (parent) REFERENCES t_fibu_employee (pk);

ALTER TABLE t_hr_planning
  ADD CONSTRAINT fks8ne51fwfd2jvbdl3nynrretm FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_poll
  ADD CONSTRAINT fkshnin439klc5hplao29fht2jr FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_tenant_user
  ADD CONSTRAINT fksojiftkoqimltkv0ceq5ko716 FOREIGN KEY (user_id) REFERENCES t_pf_user (pk);

ALTER TABLE t_plugin_financialfairplay_debt
  ADD CONSTRAINT fksorc8lxk7y238oxx0bakenf8w FOREIGN KEY (event_id) REFERENCES t_plugin_financialfairplay_event (pk);

ALTER TABLE t_addressbook
  ADD CONSTRAINT fksq4tvahtx6tpk3wir7xi3k4vw FOREIGN KEY (owner_fk) REFERENCES t_pf_user (pk);

ALTER TABLE t_fibu_rechnung_position
  ADD CONSTRAINT fkst7lprxqdpog1c8q572sa1yv1 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_skill_training
  ADD CONSTRAINT fksxstl65rj3q7uffpb4sxhy49e FOREIGN KEY (skill_fk) REFERENCES t_plugin_skill (pk);

ALTER TABLE t_plugin_bank_account
  ADD CONSTRAINT fkt1cjaklcxh979kuuisp9bgj76 FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_poll_event
  ADD CONSTRAINT fkt36yh6v7pqpsd4lotnu9qpaff FOREIGN KEY (tenant_id) REFERENCES t_tenant (pk);

ALTER TABLE t_plugin_employee_configuration_attrdata
  ADD CONSTRAINT fkt8jt3v41bmtw8fpao2d7ue1ri FOREIGN KEY (parent_id) REFERENCES t_plugin_employee_configuration_attr (pk);

ALTER TABLE t_fibu_buchungssatz
  ADD CONSTRAINT fktje88nabtn5lhkowpe2otl15a FOREIGN KEY (kost1_id) REFERENCES t_fibu_kost1 (pk);