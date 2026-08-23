/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.user

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Keeps the accounts the E2E test suite of projectforge-next logs in with, on a developer's machine.
 *
 * The suite runs against the **running** system, so it needs real accounts — and one per rights
 * situation, because a rejection is only reachable with a user who lacks the right. Creating those by
 * hand is the step every developer used to have to do (and the passwords of the users in
 * `data/pfTestdata.sql` are of no help: they are hashes of a clear text nobody has).
 *
 * Therefore ProjectForge creates them itself, on every start **in development mode only**, and notes
 * the generated passwords in [FILENAME] in ProjectForge's home directory — the file
 * `e2e/fixtures/credentials.ts` reads. Everything here is idempotent: an existing account is only
 * repaired (missing group, missing right, password that no longer matches what the file says), so a
 * restart is the way to fix a lost or stale file.
 *
 * The accounts are as harmless as the development mode they come with: their passwords are random per
 * instance and stand in a file only the developer can read. They are never created on a production
 * system, because `projectforge.development.mode` is false there.
 */
@Service
class E2ETestAccountsService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var systemStatus: SystemStatus

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userPasswordDao: UserPasswordDao

    @Autowired
    private lateinit var userRightDao: UserRightDao

    /**
     * [ApplicationReadyEvent] rather than `@PostConstruct`: the database is migrated and the caches are
     * usable by then, and a failure cannot keep the application from starting.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (!systemStatus.developmentMode) {
            return
        }
        try {
            ensureAccounts(File(configurationService.applicationHomeDir))
        } catch (ex: Exception) {
            // Logged, not rethrown: test accounts are a convenience, and an unusable one must not cost
            // the developer the whole application.
            log.error(ex) { "Can't create or update the E2E test accounts: ${ex.message}" }
        }
    }

    /**
     * The work itself, with the home directory given so that a test can point it somewhere temporary.
     *
     * @return The accounts as they now stand, by role — the content of [FILENAME].
     */
    fun ensureAccounts(homeDir: File): Map<String, Account> {
        val file = File(homeDir, FILENAME)
        val remembered = read(file)
        val changes = Changes()
        val accounts = withSystemAdminUser {
            persistenceService.runInTransaction { _ ->
                E2ETestAccount.entries.associate { it.role to ensureAccount(it, remembered[it.role], changes) }
            }
        }
        if (changes.any) {
            // Only after the transaction: the cache reads the database, and it must see the committed
            // state. Skipped when nothing was written, so the usual start doesn't pay for a full reload.
            userGroupCache.forceReload()
        }
        if (accounts != remembered) {
            write(file, accounts)
        }
        log.info {
            "E2E test accounts of this development instance (passwords in ${file.absolutePath}): " +
                    accounts.entries.joinToString { "${it.key}=${it.value.username}" }
        }
        return accounts
    }

    /**
     * Runs [block] as the system admin pseudo user, the way [org.projectforge.ProjectForgeApp] does for
     * its own work outside a request.
     *
     * `checkAccess = false` gets the DAOs' own checks out of the way, but writing a history entry asks
     * the [org.projectforge.framework.access.AccessChecker] whether the logged-in user is a restricted
     * one — and *no* user counts as restricted, so the group assignment would fail with
     * `access.exception.restrictedUserHasNoAccess`. An existing context (a test that logged someone on)
     * is put back afterwards.
     */
    private fun <T> withSystemAdminUser(block: () -> T): T {
        val previous = ThreadLocalUserContext.userContext
        ThreadLocalUserContext.userContext =
            UserContext.__internalCreateWithSpecialUser(DatabaseService.__internalGetSystemAdminPseudoUser())
        try {
            return block()
        } finally {
            ThreadLocalUserContext.userContext = previous
        }
    }

    /**
     * Creates or repairs one account, and answers with the credentials to note in the file.
     *
     * A remembered line naming a *different* user is left untouched: that is a developer pointing the
     * role at an account of their own, and overwriting it would silently undo their choice.
     */
    private fun ensureAccount(account: E2ETestAccount, remembered: Account?, changes: Changes): Account {
        if (remembered != null && remembered.username != account.username) {
            log.info { "Role '${account.role}' is set to '${remembered.username}' by hand, leaving it alone." }
            return remembered
        }
        val user = ensureUser(account, changes)
        ensureGroups(account, user, changes)
        ensureRights(account, user, changes)
        return Account(account.username, ensurePassword(user, remembered?.password, changes))
    }

    private fun ensureUser(account: E2ETestAccount, changes: Changes): PFUserDO {
        userDao.getInternalByName(account.username)?.let { user ->
            if (user.deleted) {
                throw IllegalStateException(
                    "User '${account.username}' is deleted. Undelete it (or rename it) — the E2E suite " +
                            "can't log in with it, and a second user of that name can't be created."
                )
            }
            if (user.deactivated) {
                // Reactivated rather than reported: it is this instance's own test account, and a
                // deactivated one fails the tests with a plain "login failed" that explains nothing.
                log.info { "Reactivating the deactivated E2E test account '${account.username}'." }
                user.deactivated = false
                userDao.update(user, checkAccess = false)
                changes.any = true
            }
            return user
        }
        val user = PFUserDO()
        user.username = account.username
        user.firstname = "E2E"
        user.lastname = account.lastname
        user.description = account.description
        user.email = "devnull@localhost"
        user.locale = account.locale
        user.timeZone = TimeZone.getTimeZone("Europe/Berlin")
        user.localUser = true // Not to be synchronized with any external user management.
        userDao.insert(user, checkAccess = false)
        changes.any = true
        log.info { "Created the E2E test account '${account.username}'." }
        return user
    }

    private fun ensureGroups(account: E2ETestAccount, user: PFUserDO, changes: Changes) {
        val missing = account.groups.mapNotNull { group ->
            val groupDO = userGroupCache.getGroup(group)
            if (groupDO == null) {
                log.warn { "Group '$group' doesn't exist on this instance, can't assign '${account.username}' to it." }
                null
            } else if (userGroupCache.isUserMemberOfGroup(user.id, groupDO.id)) {
                null
            } else {
                groupDO.id
            }
        }
        if (missing.isNotEmpty()) {
            // The cache is reloaded once at the end of ensureAccounts, hence updateUserGroupCache = false.
            groupDao.assignGroupByIds(user, missing, null, false)
            changes.any = true
        }
    }

    /**
     * The rights are inserted one by one rather than through `UserRightDao.updateUserRights`: that one
     * drops every value the user's groups don't make available, and the groups assigned a moment ago are
     * not in the cache it consults yet.
     */
    private fun ensureRights(account: E2ETestAccount, user: PFUserDO, changes: Changes) {
        val existing = userRightDao.select(user, checkAccess = false).associateBy { it.rightIdString }
        account.rights.forEach { (rightId, value) ->
            val right = existing[rightId.id]
            if (right == null) {
                userRightDao.insert(UserRightDO(user, rightId, value), checkAccess = false)
                changes.any = true
            } else if (right.value != value) {
                right.value = value
                userRightDao.update(right, checkAccess = false)
                changes.any = true
            }
        }
    }

    /**
     * @param remembered The password the file names, if any.
     * @return The password to note in the file: the remembered one if it still works, otherwise a new one.
     */
    private fun ensurePassword(user: PFUserDO, remembered: String?, changes: Changes): String {
        // A fresh array per call: checkPassword and encryptAndSavePassword clear what they are given.
        if (remembered != null && userPasswordDao.checkPassword(user, remembered.toCharArray())?.isOK == true) {
            return remembered
        }
        val password = NumberHelper.getSecureRandomReducedAlphanumeric(PASSWORD_LENGTH)
        userPasswordDao.encryptAndSavePassword(user.id!!, password.toCharArray(), false)
        changes.any = true
        log.info { "Set a new random password for the E2E test account '${user.username}'." }
        return password
    }

    private fun read(file: File): Map<String, Account> {
        if (!file.canRead()) {
            return emptyMap()
        }
        val accounts = mutableMapOf<String, Account>()
        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return@forEach
            }
            // role=username/password. The password may contain a slash, the username may not, so both are
            // split at their first separator only.
            val role = trimmed.substringBefore('=', missingDelimiterValue = "")
            val username = trimmed.substringAfter('=').substringBefore('/', missingDelimiterValue = "")
            val password = trimmed.substringAfter('=').substringAfter('/', missingDelimiterValue = "")
            if (role.isEmpty() || username.isEmpty() || password.isEmpty()) {
                // Without the line itself: it holds a password, and this goes into the log file.
                log.warn { "Ignoring a line of ${file.absolutePath} that isn't 'role=username/password'." }
                return@forEach
            }
            accounts[role] = Account(username, password)
        }
        return accounts
    }

    private fun write(file: File, accounts: Map<String, Account>) {
        val content = StringBuilder()
        content.appendLine("# The accounts the E2E test suite of projectforge-next logs in with, one line per role:")
        content.appendLine("#   role=username/password")
        content.appendLine("#")
        content.appendLine("# Written by ProjectForge itself (development mode only, E2ETestAccountsService) and read by")
        content.appendLine("# projectforge-next/e2e/fixtures/credentials.ts. Nothing here is worth keeping: delete a line")
        content.appendLine("# (or the whole file) and restart, and a new password is generated. Point a role at an account")
        content.appendLine("# of your own by naming another user in its line — that line is then left untouched.")
        accounts.forEach { (role, account) ->
            content.appendLine("$role=${account.username}/${account.password}")
        }
        file.writeText(content.toString())
        try {
            Files.setPosixFilePermissions(
                file.toPath(),
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            )
        } catch (ex: Exception) {
            // No POSIX permissions (Windows): the file simply keeps the ones it got.
            log.info { "Can't restrict the permissions of ${file.absolutePath} to the owner: ${ex.message}" }
        }
        log.info { "Wrote the E2E test accounts to ${file.absolutePath}." }
    }

    /** One line of the file. */
    data class Account(val username: String, val password: String)

    /**
     * Whether anything was written to the database — the usual start finds all four accounts in place and
     * then has no reason to reload the [UserGroupCache].
     */
    private class Changes(var any: Boolean = false)

    companion object {
        /** In ProjectForge's home directory, beside `projectforge.properties`. */
        const val FILENAME = "testAccounts.txt"

        private const val PASSWORD_LENGTH = 20
    }
}

/**
 * The accounts, and what makes each of them interesting for a test.
 *
 * The roles are the vocabulary of `e2e/fixtures/credentials.ts`; this is the only place that maps them
 * to a user, its groups and its rights.
 */
private enum class E2ETestAccount(
    val role: String,
    val username: String,
    val lastname: String,
    val description: String,
    val locale: Locale,
    val groups: List<ProjectForgeGroup>,
    val rights: Map<UserRightId, UserRightValue>,
) {
    /** Sees everything and may write everything — the account a test about a *feature* uses. */
    FULL_ACCESS(
        "full-access-user",
        "e2e-full-access",
        "Full access",
        "E2E test account: every group, every right.",
        Locale.GERMAN,
        ProjectForgeGroup.values().toList(),
        ALL_RIGHTS,
    ),

    /** The finance rights without the admin group. */
    FINANCE(
        "finance-user",
        "e2e-finance",
        "Finance",
        "E2E test account: finance and controlling, but not admin.",
        Locale.GERMAN,
        listOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
        FINANCE_RIGHTS,
    ),

    /**
     * Admin **without** the finance rights: the case where a user gets past the menu and is refused at
     * the entity — invoices and the order book are not the admin's business.
     */
    ADMIN(
        "admin-user",
        "e2e-admin",
        "Admin",
        "E2E test account: the admin group, and no finance rights.",
        Locale.GERMAN,
        listOf(ProjectForgeGroup.ADMIN_GROUP),
        emptyMap(),
    ),

    /**
     * A logged-in user with nothing else, and an English locale — so an assertion that only holds for a
     * German account fails here rather than in front of a customer.
     */
    NORMALO(
        "normalo-user",
        "e2e-normalo",
        "Normalo",
        "E2E test account: no group, no right, English locale.",
        Locale.ENGLISH,
        emptyList(),
        emptyMap(),
    ),
}

/**
 * Every right a user can hold, at its widest value.
 *
 * `FIBU_DATEV_IMPORT` is a flag rather than a read/write right, so it takes TRUE — a value a right
 * doesn't offer is stored but never grants anything.
 */
private val ALL_RIGHTS: Map<UserRightId, UserRightValue> = mapOf(
    UserRightId.FIBU_AUSGANGSRECHNUNGEN to UserRightValue.READWRITE,
    UserRightId.FIBU_EINGANGSRECHNUNGEN to UserRightValue.READWRITE,
    UserRightId.FIBU_COST_UNIT to UserRightValue.READWRITE,
    UserRightId.FIBU_ACCOUNTS to UserRightValue.READWRITE,
    UserRightId.FIBU_CURRENCY_CONVERSION to UserRightValue.READWRITE,
    UserRightId.FIBU_DATEV_IMPORT to UserRightValue.TRUE,
    UserRightId.ORGA_CONTRACTS to UserRightValue.READWRITE,
    UserRightId.ORGA_INCOMING_MAIL to UserRightValue.READWRITE,
    UserRightId.ORGA_OUTGOING_MAIL to UserRightValue.READWRITE,
    UserRightId.ORGA_VISITORBOOK to UserRightValue.READWRITE,
    UserRightId.PM_ORDER_BOOK to UserRightValue.READWRITE,
    UserRightId.PM_PROJECT to UserRightValue.READWRITE,
    UserRightId.PM_HR_PLANNING to UserRightValue.READWRITE,
    UserRightId.HR_EMPLOYEE to UserRightValue.READWRITE,
    UserRightId.HR_EMPLOYEE_SALARY to UserRightValue.READWRITE,
    UserRightId.HR_VACATION to UserRightValue.READWRITE,
)

/** What the finance department needs, including the order book. */
private val FINANCE_RIGHTS: Map<UserRightId, UserRightValue> = mapOf(
    UserRightId.FIBU_AUSGANGSRECHNUNGEN to UserRightValue.READWRITE,
    UserRightId.FIBU_EINGANGSRECHNUNGEN to UserRightValue.READWRITE,
    UserRightId.FIBU_COST_UNIT to UserRightValue.READWRITE,
    UserRightId.FIBU_ACCOUNTS to UserRightValue.READWRITE,
    UserRightId.FIBU_CURRENCY_CONVERSION to UserRightValue.READWRITE,
    UserRightId.FIBU_DATEV_IMPORT to UserRightValue.TRUE,
    UserRightId.PM_ORDER_BOOK to UserRightValue.READWRITE,
    UserRightId.PM_PROJECT to UserRightValue.READWRITE,
)
