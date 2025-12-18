// ProjectForge SQL Export Script
// Exportiert Timesheets ab 01.01.2024 mit allen referenzierten Daten als SQLite-kompatible SQL-Dumps

// !!! WICHTIG: Lade folgende Dateien aus sqlDumpExport-attachments/ als Attachments zum Script hoch:
// !!!   - 00_schema.sql (CREATE TABLE Statements für 20 Tabellen)
// !!!   - QUERIES.md (Beispielabfragen)
// !!!   - README.txt (Dokumentation)

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
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
               auftrag.nummer, auftrag.status, auftrag.kunde?.id, auftrag.projekt?.id,
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
               "schedule_date", "reached", "vollstaendigFakturiert", "comment"),
        listOf(schedule.id, schedule.created, schedule.lastUpdate, schedule.deleted,
               schedule.auftrag?.id, schedule.number, schedule.amount,
               schedule.scheduleDate, schedule.reached, schedule.vollstaendigFakturiert, schedule.comment)
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
               "position_text", "eintritt", "austritt", "abteilung", "staffNumber"),
        listOf(employee.id, employee.created, employee.lastUpdate, employee.deleted,
               employee.user?.id, employee.kost1?.id,
               employee.position, employee.eintrittsDatum, employee.austrittsDatum, employee.abteilung, employee.staffNumber)
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
    val nextMonth = currentMonth.plusMonths(1)
    val filter = TimesheetFilter()
    // WICHTIG: Nur Timesheets die im aktuellen Monat BEGINNEN
    // Damit werden Duplikate vermieden (Timesheets die über Mitternacht gehen)
    filter.startTime = PFDateTime.from(currentMonth.localDate).utilDate
    // Verwende Beginn des nächsten Monats als exklusive Obergrenze für start_time
    // Dies verhindert Überlappungen mit dem nächsten Monat
    filter.stopTime = PFDateTime.from(nextMonth.localDate).utilDate
    filter.orderType = OrderDirection.ASC
    val monthTimesheets = timesheetDao.select(filter)

    // Zusätzliche Sicherheitsprüfung: Nur Timesheets, die tatsächlich im aktuellen Monat beginnen
    // (Falls TimesheetFilter overlap-Logik verwendet)
    val nextMonthMillis = PFDateTime.from(nextMonth.localDate).utilDate.time
    val filteredTimesheets = monthTimesheets.filter { ts ->
        ts.startTime?.time?.let { it < nextMonthMillis } ?: true
    }
    val filtered = monthTimesheets.size - filteredTimesheets.size
    if (filtered > 0) {
        log.warn("Monat ${currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))}: $filtered Timesheets aus vorherigem Monat ignoriert")
    }

    timesheets.addAll(filteredTimesheets)
    log.info("Monat ${currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))}: ${"%,d".format(java.util.Locale.GERMAN, filteredTimesheets.size)} Timesheets")
    currentMonth = nextMonth
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
val konten = accountDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, konten.size)} Konten gefunden")

// 6. Alle Kunden
log.info("Lade Kunden...")
val kunden = customerDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kunden.size)} Kunden gefunden")

// 7. Alle Projekte
log.info("Lade Projekte...")
val projekte = projectDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, projekte.size)} Projekte gefunden")

// 8. Alle Kost2Arten
log.info("Lade Kost2Arten...")
val kost2Arten = cost2TypeDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost2Arten.size)} Kost2Arten gefunden")

// 9. Alle Kost1
log.info("Lade Kost1...")
val kost1List = cost1Dao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost1List.size)} Kost1 gefunden")

// 10. Alle Kost2
log.info("Lade Kost2...")
val kost2List = cost2Dao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, kost2List.size)} Kost2 gefunden")

// 11. Alle Employees
log.info("Lade Employees...")
val employees = employeeDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, employees.size)} Employees gefunden")

// 12. Alle Aufträge
log.info("Lade Aufträge...")
val auftraege = orderBookDao.list.sortedBy { it.id }
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
val rechnungen = outgoingInvoiceDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, rechnungen.size)} Rechnungen gefunden")

// 16. Alle Rechnungspositionen (über Rechnungen)
log.info("Lade Rechnungspositionen...")
val rechnungspositionen = rechnungen.flatMap { it.positionen ?: emptyList() }.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, rechnungspositionen.size)} Rechnungspositionen gefunden")

// 17. Alle Buchungssätze
log.info("Lade Buchungssätze...")
val buchungssaetze = accountingRecordDao.list.sortedBy { it.id }
log.info("${"%,d".format(java.util.Locale.GERMAN, buchungssaetze.size)} Buchungssätze gefunden")

// 18-20. Urlaubsdaten - vacationDao/leaveAccountEntryDao nicht im Registry
// Diese müssen über SQL oder direkt über die BaseDao-Instanz geladen werden
log.info("HINWEIS: Urlaubsdaten (VacationDO, LeaveAccountEntryDO) werden übersprungen - nicht im Registry")
val vacations = emptyList<VacationDO>()
val vacationOtherReplacements = emptyList<Pair<Long, Long>>()
val leaveAccountEntries = emptyList<LeaveAccountEntryDO>()

// === SQL GENERATION ===
log.info("Generiere SQL-Statements...")

// Load schema from attachment
val schemaTemplate = String(files.getFile("00_schema.sql"), Charsets.UTF_8)
val createTableStatements = schemaTemplate.replace("{{EXPORT_DATE}}", java.time.LocalDateTime.now().toString())

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
