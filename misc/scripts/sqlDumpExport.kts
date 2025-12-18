// ProjectForge SQL Export Script
// Exportiert Timesheets ab 01.01.2024 mit allen referenzierten Daten als SQLite-kompatible SQL-Dumps

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import java.text.SimpleDateFormat

user, zeitberichte, Aufgaben, Auftragsbuch, Kreditoren/debitorenrechnungen, Buchungssätze, Konten, Projekte, Kunde, Kost1/2, Kost2Arten,
Employees, Urlaubseinträge, Historie Wochenstunden/Urlaubstage

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

-- Indizes für bessere Performance
CREATE INDEX IF NOT EXISTS idx_timesheet_user ON T_TIMESHEET(user_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_task ON T_TIMESHEET(task_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_start ON T_TIMESHEET(start_time);
CREATE INDEX IF NOT EXISTS idx_task_parent ON T_TASK(parent_task_id);
CREATE INDEX IF NOT EXISTS idx_task_responsible ON T_TASK(responsible_user_id);
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

zip.add("README.txt", readme)

// Schema
zip.add("00_schema.sql", createTableStatements)

// Users (sortiert nach ID)
val userInserts = users.sortedBy { it.id }.map { userToInsert(it) }
zip.add("01_users.sql", wrapInTransaction(userInserts, "Users Export"))

// Groups (sortiert nach ID)
val groupInserts = groups.sortedBy { it.id }.map { groupToInsert(it) }
zip.add("02_groups.sql", wrapInTransaction(groupInserts, "Groups Export"))

// Tasks (sortiert nach ID, damit Parents vor Children kommen)
val taskInserts = tasks.sortedBy { it.id }.map { taskToInsert(it) }
zip.add("03_tasks.sql", wrapInTransaction(taskInserts, "Tasks Export"))

// Timesheets (sortiert nach ID)
val timesheetInserts = timesheets.sortedBy { it.id }.map { timesheetToInsert(it) }
zip.add("04_timesheets.sql", wrapInTransaction(timesheetInserts, "Timesheets Export"))

// Query Documentation
zip.add("QUERIES.md", queriesMarkdown)

log.info("Export erfolgreich abgeschlossen!")
log.info("ZIP-Datei enthält ${users.size + groups.size + tasks.size + timesheets.size} Einträge in 4 Tabellen")
log.info("Zusätzliche Dateien: README.txt, QUERIES.md")

zip
