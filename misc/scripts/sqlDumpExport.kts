// ProjectForge SQL Export Script
// Exportiert Timesheets ab 01.01.2024 mit allen referenzierten Daten als SQLite-kompatible SQL-Dumps

// !!! Lade sqlDumpExport-attachments/QUERIES.md und README.txt als Attachment zum Script hoch.

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import java.text.SimpleDateFormat

// === CONFIGURATION ===
val EXPORT_START_DATE = PFDay.of(2024, 1, 1)

// === SQL GENERATOR ===
object SqliteInsertGenerator {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    fun escapeString(value: String?): String {
        if (value == null) return "NULL"
        // SQLite string escaping: einfache Anführungszeichen verdoppeln
        return "'" + value.replace("'", "''") + "'"
    }

    fun formatValue(value: Any?): String {
        return when (value) {
            null -> "NULL"
            is String -> escapeString(value)
            is Boolean -> if (value) "1" else "0"
            is java.util.Date -> escapeString(dateFormatter.format(value))
            is java.time.LocalDate -> escapeString(value.toString())
            is Number -> value.toString()
            is Enum<*> -> escapeString(value.name)
            else -> escapeString(value.toString())
        }
    }

    fun generateInsert(tableName: String, columns: List<String>, values: List<Any?>): String {
        val columnList = columns.joinToString(", ")
        val valueList = values.map { formatValue(it) }.joinToString(", ")
        return "INSERT INTO $tableName ($columnList) VALUES ($valueList);"
    }
}

// === ENTITY CONVERTERS ===
fun timesheetToInsert(ts: TimesheetDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_TIMESHEET",
        listOf(
            "pk", "created", "last_update", "deleted",
            "user_id", "task_id", "start_time", "stop_time",
            "location", "description", "reference", "tag", "time_zone"
        ),
        listOf(
            ts.id, ts.created, ts.lastUpdate, ts.deleted,
            ts.user?.id, ts.task?.id, ts.startTime, ts.stopTime,
            ts.location, ts.description, ts.reference, ts.tag, ts.timeZone
        )
    )
}

fun taskToInsert(task: TaskDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_TASK",
        listOf(
            "pk", "created", "last_update", "deleted",
            "parent_task_id", "title", "status", "priority",
            "short_description", "description", "responsible_user_id", "reference"
        ),
        listOf(
            task.id, task.created, task.lastUpdate, task.deleted,
            task.parentTask?.id, task.title, task.status, task.priority,
            task.shortDescription, task.description, task.responsibleUser?.id, task.reference
        )
    )
}

fun userToInsert(user: PFUserDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_PF_USER",
        listOf(
            "pk", "created", "last_update", "deleted",
            "username", "firstname", "lastname", "email",
            "description", "deactivated", "local_user"
        ),
        listOf(
            user.id, user.created, user.lastUpdate, user.deleted,
            user.username, user.firstname, user.lastname, user.email,
            user.description, user.deactivated, user.localUser
        )
    )
}

fun groupToInsert(group: GroupDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_GROUP",
        listOf(
            "pk", "created", "last_update", "deleted",
            "name", "description", "organization", "local_group"
        ),
        listOf(
            group.id, group.created, group.lastUpdate, group.deleted,
            group.name, group.description, group.organization, group.localGroup
        )
    )
}

// === NEW CONVERTERS FOR 16 ADDITIONAL TABLES ===

// 1. KontoDO
fun kontoToInsert(konto: KontoDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_KONTO",
        listOf("pk", "created", "last_update", "deleted", "nummer", "bezeichnung", "description", "status"),
        listOf(konto.id, konto.created, konto.lastUpdate, konto.deleted,
               konto.nummer, konto.bezeichnung, konto.description, konto.status)
    )
}

// 2. KundeDO
fun kundeToInsert(kunde: KundeDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_KUNDE",
        listOf("pk", "created", "last_update", "deleted", "name", "identifier", "division", "status", "konto_id", "description"),
        listOf(kunde.id, kunde.created, kunde.lastUpdate, kunde.deleted,
               kunde.name, kunde.identifier, kunde.division, kunde.status, kunde.konto?.id, kunde.description)
    )
}

// 3. ProjektDO
fun projektToInsert(projekt: ProjektDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_PROJEKT",
        listOf("pk", "created", "last_update", "deleted", "nummer", "name", "identifier", "status",
               "kunde_id", "konto_id", "task_fk", "intern_kost2_4", "projektmanager_group_fk",
               "projectmanager_fk", "headofbusinessmanager_fk", "salesmanager_fk", "description"),
        listOf(projekt.id, projekt.created, projekt.lastUpdate, projekt.deleted,
               projekt.nummer, projekt.name, projekt.identifier, projekt.status,
               projekt.kunde?.id, projekt.konto?.id, projekt.task?.id, projekt.internKost2_4,
               projekt.projektManagerGroup?.id, projekt.projectManager?.id,
               projekt.headOfBusinessManager?.id, projekt.salesManager?.id, projekt.description)
    )
}

// 4. AuftragDO
fun auftragToInsert(auftrag: AuftragDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_AUFTRAG",
        listOf("pk", "created", "last_update", "deleted", "nummer", "status", "kunde_fk", "projekt_fk",
               "contact_person_fk", "projectmanager_fk", "headofbusinessmanager_fk", "salesmanager_fk",
               "titel", "bemerkung", "referenz", "kunde_text", "angebots_datum", "erfassungs_datum",
               "entscheidungs_datum", "bindungs_frist", "beauftragungs_datum"),
        listOf(auftrag.id, auftrag.created, auftrag.lastUpdate, auftrag.deleted,
               auftrag.nummer, auftrag.auftragsStatus, auftrag.kunde?.id, auftrag.projekt?.id,
               auftrag.contactPerson?.id, auftrag.projectManager?.id, auftrag.headOfBusinessManager?.id,
               auftrag.salesManager?.id, auftrag.titel, auftrag.bemerkung, auftrag.referenz, auftrag.kundeText,
               auftrag.angebotsDatum, auftrag.erfassungsDatum, auftrag.entscheidungsDatum, auftrag.bindungsFrist,
               auftrag.beauftragungsDatum)
    )
}

// 5. AuftragsPositionDO
fun auftragspositionToInsert(position: AuftragsPositionDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_AUFTRAG_POSITION",
        listOf("pk", "created", "last_update", "deleted", "auftrag_fk", "number", "art", "titel",
               "bemerkung", "nettoSumme", "status", "vollstaendigFakturiert", "task_id"),
        listOf(position.id, position.created, position.lastUpdate, position.deleted,
               position.auftrag?.id, position.number, position.art, position.titel,
               position.bemerkung, position.nettoSumme, position.status, position.vollstaendigFakturiert, position.task?.id)
    )
}

// 6. PaymentScheduleDO
fun paymentScheduleToInsert(schedule: PaymentScheduleDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_PAYMENT_SCHEDULE",
        listOf("pk", "created", "last_update", "deleted", "auftrag_fk", "number", "amount",
               "schedule_date", "reached", "vollstaendigFakturiert", "titel", "bemerkung"),
        listOf(schedule.id, schedule.created, schedule.lastUpdate, schedule.deleted,
               schedule.auftrag?.id, schedule.number, schedule.amount,
               schedule.scheduleDate, schedule.reached, schedule.vollstaendigFakturiert, schedule.titel, schedule.bemerkung)
    )
}

// 7. RechnungDO
fun rechnungToInsert(rechnung: RechnungDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_RECHNUNG",
        listOf("pk", "created", "last_update", "deleted", "nummer", "kunde_id", "projekt_id",
               "datum", "faelligkeit", "bezahl_datum", "zahlBetrag", "status", "typ",
               "bemerkung", "besonderheiten", "konto_id", "discountPercent", "discountMaturity"),
        listOf(rechnung.id, rechnung.created, rechnung.lastUpdate, rechnung.deleted,
               rechnung.nummer, rechnung.kunde?.id, rechnung.projekt?.id,
               rechnung.datum, rechnung.faelligkeit, rechnung.bezahlDatum, rechnung.zahlBetrag, rechnung.status, rechnung.typ,
               rechnung.bemerkung, rechnung.besonderheiten, rechnung.konto?.id, rechnung.discountPercent, rechnung.discountMaturity)
    )
}

// 8. RechnungsPositionDO
fun rechnungspositionToInsert(position: RechnungsPositionDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_RECHNUNG_POSITION",
        listOf("pk", "created", "last_update", "deleted", "rechnung_fk", "number", "text",
               "menge", "einzelNetto", "vat", "auftragsPosition_fk"),
        listOf(position.id, position.created, position.lastUpdate, position.deleted,
               position.rechnung?.id, position.number, position.text,
               position.menge, position.einzelNetto, position.vat, position.auftragsPosition?.id)
    )
}

// 9. Kost1DO
fun kost1ToInsert(kost1: Kost1DO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_KOST1",
        listOf("pk", "created", "last_update", "deleted", "nummernkreis", "bereich", "teilbereich", "endziffer",
               "kostentraeger_status", "description"),
        listOf(kost1.id, kost1.created, kost1.lastUpdate, kost1.deleted,
               kost1.nummernkreis, kost1.bereich, kost1.teilbereich, kost1.endziffer,
               kost1.kostentraegerStatus, kost1.description)
    )
}

// 10. Kost2ArtDO
fun kost2ArtToInsert(art: Kost2ArtDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_KOST2ART",
        listOf("pk", "created", "last_update", "deleted", "id", "name", "description", "fakturiert"),
        listOf(art.id, art.created, art.lastUpdate, art.deleted,
               art.id, art.name, art.description, art.fakturiert)
    )
}

// 11. Kost2DO
fun kost2ToInsert(kost2: Kost2DO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_KOST2",
        listOf("pk", "created", "last_update", "deleted", "nummernkreis", "bereich", "teilbereich",
               "kost2_art_id", "work_fraction", "description", "comment", "kostentraeger_status", "projekt_id"),
        listOf(kost2.id, kost2.created, kost2.lastUpdate, kost2.deleted,
               kost2.nummernkreis, kost2.bereich, kost2.teilbereich,
               kost2.kost2Art?.id, kost2.workFraction, kost2.description, kost2.comment, kost2.kostentraegerStatus, kost2.projekt?.id)
    )
}

// 12. BuchungssatzDO
fun buchungssatzToInsert(satz: BuchungssatzDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_BUCHUNGSSATZ",
        listOf("pk", "created", "last_update", "deleted", "year", "month", "satznr",
               "betrag", "sh", "datum", "konto_id", "gegen_konto_id", "kost1_id", "kost2_id",
               "menge", "beleg", "text", "comment"),
        listOf(satz.id, satz.created, satz.lastUpdate, satz.deleted,
               satz.year, satz.month, satz.satznr,
               satz.betrag, satz.sh, satz.datum, satz.konto?.id, satz.gegenKonto?.id, satz.kost1?.id, satz.kost2?.id,
               satz.menge, satz.beleg, satz.text, satz.comment)
    )
}

// 13. EmployeeDO
fun employeeToInsert(employee: EmployeeDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_FIBU_EMPLOYEE",
        listOf("pk", "created", "last_update", "deleted", "user_id", "kost1_id",
               "position_text", "eintritt", "austritt", "division", "abteilung", "staffNumber"),
        listOf(employee.id, employee.created, employee.lastUpdate, employee.deleted,
               employee.user?.id, employee.kost1?.id,
               employee.position, employee.eintrittsDatum, employee.austrittsDatum, employee.division, employee.abteilung, employee.staffNumber)
    )
}

// 14. VacationDO
fun vacationToInsert(vacation: VacationDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_EMPLOYEE_VACATION",
        listOf("pk", "created", "last_update", "deleted", "employee_id", "start_date", "end_date",
               "replacement_id", "manager_id", "vacation_status", "is_special", "is_half_day_begin", "is_half_day_end", "comment"),
        listOf(vacation.id, vacation.created, vacation.lastUpdate, vacation.deleted,
               vacation.employee?.id, vacation.startDate, vacation.endDate,
               vacation.replacement?.id, vacation.manager?.id, vacation.status, vacation.special, vacation.halfDayBegin, vacation.halfDayEnd, vacation.comment)
    )
}

// 15. LeaveAccountEntryDO
fun leaveAccountEntryToInsert(entry: LeaveAccountEntryDO): String {
    return SqliteInsertGenerator.generateInsert(
        "T_EMPLOYEE_LEAVE_ACCOUNT_ENTRY",
        listOf("pk", "created", "last_update", "deleted", "employee_id", "date", "amount", "description"),
        listOf(entry.id, entry.created, entry.lastUpdate, entry.deleted,
               entry.employee?.id, entry.date, entry.amount, entry.description)
    )
}


// === DATA COLLECTION ===
log.info("Starte SQL-Export...")

// 1. Timesheets ab 01.01.2024 laden (monatsweise wegen 100k Limit)
log.info("Lade Timesheets ab ${EXPORT_START_DATE}...")
val timesheets = mutableListOf<TimesheetDO>()
var currentMonth = EXPORT_START_DATE.beginOfMonth
val today = PFDay.now()

while (currentMonth.isBefore(today) || currentMonth == today) {
    val monthEnd = currentMonth.endOfMonth
    val filter = TimesheetFilter()
    filter.startTime = PFDateTime.from(currentMonth.localDate).utilDate
    filter.stopTime = PFDateTime.from(monthEnd.localDate).endOfDay.utilDate
    filter.orderType = OrderDirection.ASC
    val monthTimesheets = timesheetDao.select(filter)
    timesheets.addAll(monthTimesheets)
    log.info("Monat ${currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))}: ${"%,d".format(java.util.Locale.GERMAN, monthTimesheets.size)} Timesheets")
    currentMonth = currentMonth.plusMonths(1)
}
log.info("Gesamt: ${"%,d".format(java.util.Locale.GERMAN, timesheets.size)} Timesheets gefunden")

// 2. Referenzierte Tasks sammeln (inkl. Parent-Tasks für vollständige Hierarchie)
log.info("Sammle referenzierte Tasks...")
val tasks = mutableListOf<TaskDO>()
val taskIds = mutableSetOf<Long>()
val tasksToProcess = timesheets.mapNotNull { it.task?.id }.toMutableSet()

while (tasksToProcess.isNotEmpty()) {
    val taskId = tasksToProcess.first()
    tasksToProcess.remove(taskId)

    if (!taskIds.contains(taskId)) {
        val taskObj = caches.getTask(taskId)
        if (taskObj != null) {
            tasks.add(taskObj)
            taskIds.add(taskId)
            // Parent-Task auch hinzufügen
            taskObj.parentTask?.id?.let { parentId ->
                if (!taskIds.contains(parentId)) {
                    tasksToProcess.add(parentId)
                }
            }
        }
    }
}
log.info("${"%,d".format(java.util.Locale.GERMAN, tasks.size)} Tasks gefunden (inkl. Parent-Tasks)")

// 3. Referenzierte Users sammeln
log.info("Sammle referenzierte Users...")
val referencedUserIds = mutableSetOf<Long>()
timesheets.forEach { ts -> ts.user?.id?.let { referencedUserIds.add(it) } }
tasks.forEach { task -> task.responsibleUser?.id?.let { referencedUserIds.add(it) } }

val users = mutableListOf<PFUserDO>()
referencedUserIds.forEach { userId ->
    val userObj = caches.getUser(userId)
    if (userObj != null) {
        users.add(userObj)
    }
}
log.info("${"%,d".format(java.util.Locale.GERMAN, users.size)} Users gefunden")

// 4. Referenzierte Groups sammeln
log.info("Sammle referenzierte Groups...")
val allGroups = groupDao.list
val groups = mutableListOf<GroupDO>()

allGroups.forEach { group ->
    val hasReferencedUser = group.assignedUsers?.any { user ->
        referencedUserIds.contains(user.id)
    } ?: false
    if (hasReferencedUser) {
        groups.add(group)
    }
}
log.info("${"%,d".format(java.util.Locale.GERMAN, groups.size)} Groups gefunden")

// === NEW DATA COLLECTION FOR 16 ADDITIONAL TABLES ===

// 5. Alle Konten
log.info("Lade Konten...")
val konten = kontoDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, konten.size)} Konten gefunden")

// 6. Alle Kunden
log.info("Lade Kunden...")
val kunden = kundeDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kunden.size)} Kunden gefunden")

// 7. Alle Projekte
log.info("Lade Projekte...")
val projekte = projektDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, projekte.size)} Projekte gefunden")

// 8. Alle Kost2Arten
log.info("Lade Kost2Arten...")
val kost2Arten = kost2ArtDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost2Arten.size)} Kost2Arten gefunden")

// 9. Alle Kost1
log.info("Lade Kost1...")
val kost1List = kost1Dao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost1List.size)} Kost1 gefunden")

// 10. Alle Kost2
log.info("Lade Kost2...")
val kost2List = kost2Dao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost2List.size)} Kost2 gefunden")

// 11. Alle Employees
log.info("Lade Employees...")
val employees = employeeDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, employees.size)} Employees gefunden")

// 12. Alle Aufträge
log.info("Lade Aufträge...")
val auftraege = auftragDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, auftraege.size)} Aufträge gefunden")

// 13. Alle Auftragspositionen (über Aufträge)
log.info("Lade Auftragspositionen...")
val auftragspositionen = auftraege.flatMap { it.positionen ?: emptyList() }.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, auftragspositionen.size)} Auftragspositionen gefunden")

// 14. Alle Zahlungspläne (über Aufträge)
log.info("Lade Zahlungspläne...")
val zahlungsplaene = auftraege.flatMap { it.paymentSchedules ?: emptyList() }.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, zahlungsplaene.size)} Zahlungspläne gefunden")

// 15. Alle Rechnungen
log.info("Lade Rechnungen...")
val rechnungen = rechnungDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, rechnungen.size)} Rechnungen gefunden")

// 16. Alle Rechnungspositionen (über Rechnungen)
log.info("Lade Rechnungspositionen...")
val rechnungspositionen = rechnungen.flatMap { it.positionen ?: emptyList() }.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, rechnungspositionen.size)} Rechnungspositionen gefunden")

// 17. Alle Buchungssätze
log.info("Lade Buchungssätze...")
val buchungssaetze = buchungssatzDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, buchungssaetze.size)} Buchungssätze gefunden")

// 18. Alle Urlaubseinträge
log.info("Lade Urlaubseinträge...")
val vacations = vacationDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, vacations.size)} Urlaubseinträge gefunden")

// 19. M:N Tabelle für Urlaubsvertretungen
log.info("Sammle Urlaubsvertretungen...")
val vacationOtherReplacements = mutableListOf<Pair<Long, Long>>()
vacations.forEach { vacation ->
    vacation.otherReplacements?.forEach { employee ->
        vacationOtherReplacements.add(Pair(vacation.id!!, employee.id!!))
    }
}
log.info("${"%,d".format(java.util.Locale.GERMAN, vacationOtherReplacements.size)} Urlaubsvertretungen gefunden")

// 20. Alle Urlaubskonto-Einträge (Historie)
log.info("Lade Urlaubskonto-Historie...")
val leaveAccountEntries = leaveAccountEntryDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, leaveAccountEntries.size)} Urlaubskonto-Einträge gefunden")

// === SQL GENERATION ===
log.info("Generiere SQL-Statements...")

val createTableStatements = """
PRAGMA foreign_keys = ON;
PRAGMA encoding = "UTF-8";

-- ProjectForge SQL-Export - Schema
-- Exportiert am: ${java.time.LocalDateTime.now()}

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
    titel TEXT,
    bemerkung TEXT,
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
    division TEXT,
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
""".trimIndent()

fun wrapInTransaction(statements: List<String>, comment: String): String {
    return buildString {
        appendLine("-- $comment")
        appendLine("-- Anzahl: ${statements.size}")
        appendLine("BEGIN TRANSACTION;")
        statements.forEach { appendLine(it) }
        appendLine("COMMIT;")
    }
}

// === ZIP CREATION ===
log.info("Erstelle ZIP-Archiv...")

val zip = ExportZipArchive("projectforge_export_${java.time.LocalDate.now()}")

// Load documentation from attachments
val queriesMarkdown = String(files.getFile("QUERIES.md"), Charsets.UTF_8)
val readmeTemplate = String(files.getFile("README.txt"), Charsets.UTF_8)

val readme = readmeTemplate
    .replace("{{EXPORT_DATE}}", java.time.LocalDateTime.now().toString())
    .replace("{{USER_COUNT}}", users.size.toString())
    .replace("{{GROUP_COUNT}}", groups.size.toString())
    .replace("{{TASK_COUNT}}", tasks.size.toString())
    .replace("{{TIMESHEET_COUNT}}", timesheets.size.toString())
    .replace("{{KONTO_COUNT}}", konten.size.toString())
    .replace("{{KUNDE_COUNT}}", kunden.size.toString())
    .replace("{{PROJEKT_COUNT}}", projekte.size.toString())
    .replace("{{KOST1_COUNT}}", kost1List.size.toString())
    .replace("{{KOST2ART_COUNT}}", kost2Arten.size.toString())
    .replace("{{KOST2_COUNT}}", kost2List.size.toString())
    .replace("{{EMPLOYEE_COUNT}}", employees.size.toString())
    .replace("{{AUFTRAG_COUNT}}", auftraege.size.toString())
    .replace("{{AUFTRAGSPOSITION_COUNT}}", auftragspositionen.size.toString())
    .replace("{{PAYMENT_SCHEDULE_COUNT}}", zahlungsplaene.size.toString())
    .replace("{{RECHNUNG_COUNT}}", rechnungen.size.toString())
    .replace("{{RECHNUNGSPOSITION_COUNT}}", rechnungspositionen.size.toString())
    .replace("{{BUCHUNGSSATZ_COUNT}}", buchungssaetze.size.toString())
    .replace("{{VACATION_COUNT}}", vacations.size.toString())
    .replace("{{VACATION_REPLACEMENT_COUNT}}", vacationOtherReplacements.size.toString())
    .replace("{{LEAVE_ACCOUNT_ENTRY_COUNT}}", leaveAccountEntries.size.toString())

zip.add("README.txt", readme)

// Schema
zip.add("00_schema.sql", createTableStatements)

// === EXISTING TABLES (Users, Groups, Tasks) ===

// Users (sortiert nach ID)
val userInserts = users.sortedBy { it.id }.map { userToInsert(it) }
zip.add("01_users.sql", wrapInTransaction(userInserts, "Users Export"))

// Groups (sortiert nach ID)
val groupInserts = groups.sortedBy { it.id }.map { groupToInsert(it) }
zip.add("02_groups.sql", wrapInTransaction(groupInserts, "Groups Export"))

// Tasks (sortiert nach ID, damit Parents vor Children kommen)
val taskInserts = tasks.sortedBy { it.id }.map { taskToInsert(it) }
zip.add("03_tasks.sql", wrapInTransaction(taskInserts, "Tasks Export"))

// === NEW TABLES (Foreign Key Order beachten!) ===

// 5. Konten (referenced by Kunden, Projekte, Rechnungen)
log.info("Generiere Konten SQL...")
val kontenInserts = konten.map { kontoToInsert(it) }
zip.add("05_konten.sql", wrapInTransaction(kontenInserts, "Konten Export"))

// 6. Kunden (referenced by Projekte, Aufträge, Rechnungen)
log.info("Generiere Kunden SQL...")
val kundenInserts = kunden.map { kundeToInsert(it) }
zip.add("06_kunden.sql", wrapInTransaction(kundenInserts, "Kunden Export"))

// 7. Projekte (referenced by Aufträge, Rechnungen, Kost2)
log.info("Generiere Projekte SQL...")
val projekteInserts = projekte.map { projektToInsert(it) }
zip.add("07_projekte.sql", wrapInTransaction(projekteInserts, "Projekte Export"))

// 8. Kost2Arten (referenced by Kost2)
log.info("Generiere Kost2Arten SQL...")
val kost2ArtenInserts = kost2Arten.map { kost2ArtToInsert(it) }
zip.add("08_kost2arten.sql", wrapInTransaction(kost2ArtenInserts, "Kost2Arten Export"))

// 9. Kost1 (referenced by Employees, Buchungssätze)
log.info("Generiere Kost1 SQL...")
val kost1Inserts = kost1List.map { kost1ToInsert(it) }
zip.add("09_kost1.sql", wrapInTransaction(kost1Inserts, "Kost1 Export"))

// 10. Kost2 (referenced by Buchungssätze, Timesheets)
log.info("Generiere Kost2 SQL...")
val kost2Inserts = kost2List.map { kost2ToInsert(it) }
zip.add("10_kost2.sql", wrapInTransaction(kost2Inserts, "Kost2 Export"))

// 11. Employees (referenced by Urlaubseinträge)
log.info("Generiere Employees SQL...")
val employeeInserts = employees.map { employeeToInsert(it) }
zip.add("11_employees.sql", wrapInTransaction(employeeInserts, "Employees Export"))

// 12. Aufträge
log.info("Generiere Aufträge SQL...")
val auftraegeInserts = auftraege.map { auftragToInsert(it) }
zip.add("12_auftraege.sql", wrapInTransaction(auftraegeInserts, "Aufträge Export"))

// 13. Auftragspositionen
log.info("Generiere Auftragspositionen SQL...")
val auftragspositionenInserts = auftragspositionen.map { auftragspositionToInsert(it) }
zip.add("13_auftragspositionen.sql", wrapInTransaction(auftragspositionenInserts, "Auftragspositionen Export"))

// 14. Zahlungspläne
log.info("Generiere Zahlungspläne SQL...")
val zahlungsplaeneInserts = zahlungsplaene.map { paymentScheduleToInsert(it) }
zip.add("14_zahlungsplaene.sql", wrapInTransaction(zahlungsplaeneInserts, "Zahlungspläne Export"))

// 15. Rechnungen
log.info("Generiere Rechnungen SQL...")
val rechnungenInserts = rechnungen.map { rechnungToInsert(it) }
zip.add("15_rechnungen.sql", wrapInTransaction(rechnungenInserts, "Rechnungen Export"))

// 16. Rechnungspositionen
log.info("Generiere Rechnungspositionen SQL...")
val rechnungspositionenInserts = rechnungspositionen.map { rechnungspositionToInsert(it) }
zip.add("16_rechnungspositionen.sql", wrapInTransaction(rechnungspositionenInserts, "Rechnungspositionen Export"))

// 17. Buchungssätze (nach Konten/Kost1/Kost2)
log.info("Generiere Buchungssätze SQL...")
val buchungssaetzeInserts = buchungssaetze.map { buchungssatzToInsert(it) }
zip.add("17_buchungssaetze.sql", wrapInTransaction(buchungssaetzeInserts, "Buchungssätze Export"))

// 4. Timesheets (nach Kost2, da Kost2 in Timesheets referenziert werden kann)
log.info("Generiere Timesheets SQL...")
val timesheetInserts = timesheets.sortedBy { it.id }.map { timesheetToInsert(it) }
zip.add("04_timesheets.sql", wrapInTransaction(timesheetInserts, "Timesheets Export"))

// 18. Urlaubseinträge (nach Employees)
log.info("Generiere Urlaubseinträge SQL...")
val vacationInserts = vacations.map { vacationToInsert(it) }
zip.add("18_urlaubseintraege.sql", wrapInTransaction(vacationInserts, "Urlaubseinträge Export"))

// 19. Urlaubsvertretungen (M:N Tabelle)
log.info("Generiere Urlaubsvertretungen SQL...")
val vacationReplacementInserts = vacationOtherReplacements.map { (vacationId, employeeId) ->
    SqliteInsertGenerator.generateInsert(
        "T_EMPLOYEE_VACATION_OTHER_REPLACEMENTS",
        listOf("vacation_id", "employee_id"),
        listOf(vacationId, employeeId)
    )
}
zip.add("19_urlaubsvertretungen.sql", wrapInTransaction(vacationReplacementInserts, "Urlaubsvertretungen Export"))

// 20. Urlaubskonto-Historie (nach Employees)
log.info("Generiere Urlaubskonto-Historie SQL...")
val leaveAccountInserts = leaveAccountEntries.map { leaveAccountEntryToInsert(it) }
zip.add("20_urlaubskonto_historie.sql", wrapInTransaction(leaveAccountInserts, "Urlaubskonto-Historie Export"))

// Query Documentation
zip.add("QUERIES.md", queriesMarkdown)

// === FINAL SUMMARY ===
val totalEntries = users.size + groups.size + tasks.size + timesheets.size +
                  konten.size + kunden.size + projekte.size +
                  kost1List.size + kost2Arten.size + kost2List.size +
                  employees.size + auftraege.size + auftragspositionen.size + zahlungsplaene.size +
                  rechnungen.size + rechnungspositionen.size + buchungssaetze.size +
                  vacations.size + vacationOtherReplacements.size + leaveAccountEntries.size

log.info("Export erfolgreich abgeschlossen!")
log.info("ZIP-Datei enthält ${"%,d".format(java.util.Locale.GERMAN, totalEntries)} Einträge in 20 Tabellen")
log.info("Zusätzliche Dateien: README.txt, QUERIES.md")

zip
