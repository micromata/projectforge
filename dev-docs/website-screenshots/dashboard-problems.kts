// Sample problems of the system dashboard (overview) for the website screenshot: log groups with hourly counts of
// the last days, so that the tiles show figures and trend bars. Fictional messages only. Run it again to start over:
// it deletes its own groups (fingerprints of the keys "screenshot-...") first.
// Usage: node run-script.mjs dashboard-problems.kts
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.support.LogAggregationService
import org.projectforge.framework.support.LogBucketDO
import org.projectforge.framework.support.LogGroupDO
import org.projectforge.framework.support.LogGroupStatus
import java.util.Date

fun <T : Any> bean(c: Class<T>): T = ApplicationContextProvider.getApplicationContext().getBean(c)

val hourMillis = 3_600_000L
val now = System.currentTimeMillis()
val currentHour = now - now % hourMillis

/** [hoursAgo] → occurrences of that hour (0 = current hour). */
class Problem(
    val key: String,
    val code: String,
    val category: LogCategory,
    val level: LogLevel,
    val location: String,
    val exceptionClass: String?,
    val message: String,
    val status: LogGroupStatus = LogGroupStatus.NEW,
    val firstSeenHoursAgo: Int,
    val mutedHours: Int? = null,
    val reopenedHoursAgo: Int? = null,
    val counts: (Int) -> Int,
)

// Deterministic "noise", so that every run produces the same picture.
fun noise(h: Int, seed: Int): Int = ((h * 7919 + seed * 104729) % 97 + 97) % 97

val problems = listOf(
    // The mail server fails since four days, with a break: errors every hour, more in the morning.
    Problem(
        "screenshot-mail", "mail.sendFailed", LogCategory.EXTERNAL, LogLevel.ERROR, "SendMail.send",
        "org.eclipse.angus.mail.util.MailConnectException",
        "Couldn't connect to host, port: smtp.example.org, 587; timeout 30000",
        firstSeenHoursAgo = 100,
    ) { h -> if (h > 100 || h in 50..70) 0 else 6 + noise(h, 1) % 9 + (if ((currentHour / hourMillis - h) % 24 in 7..10) 14 else 0) },
    // The exchange rate API answered slowly a few times during the week.
    Problem(
        "screenshot-exchange-rate", "fibu.exchangeRate.fetchFailed", LogCategory.EXTERNAL, LogLevel.ERROR,
        "ExchangeRateApiProbeJob.execute", "java.net.SocketTimeoutException", "Exchange rates not fetched: Read timed out",
        status = LogGroupStatus.ACKNOWLEDGED, firstSeenHoursAgo = 150,
    ) { h -> if (noise(h, 2) < 9) 1 + noise(h, 3) % 3 else 0 },
    // A slow cache refresh, muted for the next days.
    Problem(
        "screenshot-cache", "support.loggedError", LogCategory.UNCLASSIFIED, LogLevel.WARN, "AbstractCache.checkRefresh", null,
        "Refresh of cache 'AuftragsCache' took 41 s",
        firstSeenHoursAgo = 120, mutedHours = 72,
    ) { h -> if (h % 6 == 0) 1 + noise(h, 4) % 2 else 0 },
    // A holiday calendar that is offline now and then: ignored.
    Problem(
        "screenshot-ical", "ical.subscriptions.systemicFailure", LogCategory.EXTERNAL, LogLevel.WARN,
        "TeamEventExternalSubscriptionCache.refresh", null,
        "Calendar subscription 'Public holidays' not updated: HTTP 503 Service Unavailable",
        status = LogGroupStatus.IGNORED, firstSeenHoursAgo = 160,
    ) { h -> if (noise(h, 5) < 20) 2 else 0 },
    // A bug fixed last week that is back since this morning: a regression.
    Problem(
        "screenshot-regression", "support.requestError", LogCategory.BUG, LogLevel.ERROR, "TimesheetPagesRest.onBeforeSaveOrUpdate",
        "java.lang.NullPointerException", "Cannot invoke \"Kost2DO.getId()\" because \"timesheet.kost2\" is null",
        firstSeenHoursAgo = 400, reopenedHoursAgo = 5,
    ) { h -> if (h <= 5) 2 + noise(h, 6) % 4 else if (h in 100..140 && noise(h, 7) < 30) 1 else 0 },
    // New today.
    Problem(
        "screenshot-new", "support.loggedError", LogCategory.BUG, LogLevel.ERROR, "InvoiceExportService.exportXRechnung",
        "java.lang.IllegalStateException", "Missing buyer reference (BT-10) for invoice 2026-1043",
        firstSeenHoursAgo = 3,
    ) { h -> if (h <= 3) 1 + noise(h, 8) % 2 else 0 },
    // Failed logins of unknown users, spread over the week.
    Problem(
        "screenshot-login", "rest.authenticationFailed", LogCategory.SECURITY, LogLevel.WARN, "LoginService.authenticate",
        null, "Login failed: unknown user",
        firstSeenHoursAgo = 168,
    ) { h -> if (noise(h, 9) < 25) 1 + noise(h, 10) % 3 else 0 },
)

val aggregation = bean(LogAggregationService::class.java)
val fingerprints = problems.associate { it.key to LogAggregationService.fingerprintOf(it.key) }
var inserted = 0
bean(PfPersistenceService::class.java).runInNewTransaction { context ->
    val old = context.executeQuery(
        "select g.id from LogGroupDO g where g.fingerprint in :fingerprints", java.lang.Long::class.java,
        "fingerprints" to fingerprints.values,
    ).map { it.toLong() }
    if (old.isNotEmpty()) {
        context.executeUpdate("delete from LogBucketDO b where b.groupId in :ids", "ids" to old)
        context.executeUpdate("delete from LogGroupDO g where g.id in :ids", "ids" to old)
    }
    problems.forEach { p ->
        val hours = (0..minOf(p.firstSeenHoursAgo, 7 * 24 - 1)).map { it to p.counts(it) }.filter { it.second > 0 }
        val group = LogGroupDO().also {
            it.fingerprint = fingerprints[p.key]
            it.code = p.code
            it.category = p.category
            it.level = p.level
            it.location = p.location
            it.exceptionClass = p.exceptionClass
            it.sampleMessage = p.message
            it.firstSeen = Date(currentHour - p.firstSeenHoursAgo * hourMillis + 17 * 60_000)
            it.lastSeen = Date(minOf(now, currentHour - (hours.minOfOrNull { h -> h.first } ?: 0) * hourMillis + 40 * 60_000))
            it.totalCount = hours.sumOf { h -> h.second }.toLong() + if (p.reopenedHoursAgo != null) 23 else 0
            it.status = p.status
            it.mutedUntil = p.mutedHours?.let { m -> Date(now + m * hourMillis) }
            it.reopenedAt = p.reopenedHoursAgo?.let { r -> Date(currentHour - r * hourMillis + 5 * 60_000) }
        }
        context.insert(group)
        hours.forEach { (h, occurrences) ->
            context.insert(LogBucketDO().also {
                it.groupId = group.id
                it.bucketStart = Date(currentHour - h * hourMillis)
                it.occurrences = occurrences
                it.distinctUsers = if (p.category == LogCategory.BUG) 1 else 0
            })
        }
        inserted++
    }
}
aggregation.reloadStates()
"$inserted sample problems inserted."
