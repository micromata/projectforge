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

package org.projectforge.business.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.business.testdata.TestDataSeeder
import org.projectforge.business.user.E2ETestAccountsService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.TimeZone

private val log = KotlinLogging.logger {}

/** Setup target: create an empty database or populate it with test data. */
enum class SetupTarget { EMPTY_DATABASE, TEST_DATA }

/** A field-level validation error returned by [SetupService.validate]. */
data class SetupValidationError(
    /** The form field name the error belongs to. */
    val field: String,
    /** Already-localised human-readable error message. */
    val message: String,
)

/**
 * Service encapsulating the initial database setup flow, used by the Next.js setup page
 * (/next/setup) via its REST endpoint (/rsPublic/setup).
 *
 * The caller is responsible for creating an HTTP login session for the returned
 * admin user after [finish] succeeds.
 */
@Service
class SetupService {

    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var passwordQualityService: PasswordQualityService

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var testDataSeeder: TestDataSeeder

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var systemStatus: SystemStatus

    @Autowired
    private lateinit var e2eTestAccountsService: E2ETestAccountsService

    /**
     * The progress of a running [finish] in percent (only the test data is measured), null if no setup is running.
     */
    @Volatile
    var progress: Int? = null
        private set

    /**
     * Validates setup parameters before [finish] is called.
     * Returns an empty list when everything is valid.
     */
    fun validate(
        password: String,
        passwordRepeat: String,
        calendarDomain: String,
    ): List<SetupValidationError> {
        val errors = mutableListOf<SetupValidationError>()
        if (password != passwordRepeat) {
            errors.add(
                SetupValidationError(
                    field = "passwordRepeat",
                    message = I18nHelper.getLocalizedMessage("user.error.passwordAndRepeatDoesNotMatch"),
                )
            )
        } else {
            val qualityErrors: List<I18nKeyAndParams> =
                passwordQualityService.checkPasswordQuality(password.toCharArray())
            qualityErrors.forEach { keyAndParams ->
                errors.add(
                    SetupValidationError(
                        field = "password",
                        message = I18nHelper.getLocalizedMessage(keyAndParams),
                    )
                )
            }
        }
        if (!Configuration.isDomainValid(calendarDomain)) {
            errors.add(
                SetupValidationError(
                    field = "calendarDomain",
                    message = I18nHelper.getLocalizedMessage("validation.error.generic"),
                )
            )
        }
        return errors
    }

    /**
     * Performs the full setup sequence: creates DB content, sets configuration params,
     * clears the [SystemStatus.setupRequiredFirst] flag, and calls plugin hooks.
     *
     * Pre-conditions: [validate] returned no errors and
     * [DatabaseService.databaseTablesWithEntriesExist] returned `false`.
     *
     * @return The newly created (or updated) admin [PFUserDO] — pass it to
     *   [org.projectforge.login.LoginService.internalLogin] to start a session.
     */
    fun finish(
        target: SetupTarget,
        adminUsername: String,
        password: String,
        timeZone: TimeZone,
        calendarDomain: String,
        sysopEMail: String?,
        feedbackEMail: String?,
    ): PFUserDO {
        progress = 0
        try {
            return internalFinish(target, adminUsername, password, timeZone, calendarDomain, sysopEMail, feedbackEMail)
        } finally {
            progress = null
        }
    }

    private fun internalFinish(
        target: SetupTarget,
        adminUsername: String,
        password: String,
        timeZone: TimeZone,
        calendarDomain: String,
        sysopEMail: String?,
        feedbackEMail: String?,
    ): PFUserDO {
        log.info { "Starting setup: target=$target, adminUsername=$adminUsername" }
        val adminUser = PFUserDO().also { it.username = adminUsername }

        databaseService.insertGlobalAddressbook()

        val testData = if (target == SetupTarget.EMPTY_DATABASE) {
            databaseService.initializeDefaultData(adminUser, timeZone)
            null
        } else {
            // The sample users get the admin's password, so they are usable right away.
            testDataSeeder.seedCore(password, timeZone) { progress = (it * 90).toInt() }
                .also { Configuration.instance.forceReload() }
        }

        val updatedAdmin = databaseService.updateAdminUser(adminUser, timeZone)
        if (password.isNotBlank()) {
            userService.encryptAndSavePassword(updatedAdmin, password.toCharArray())
        }

        systemStatus.setupRequiredFirst = false

        configurationDao.checkAndUpdateDatabaseEntries()
        configurationDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE)?.let { config ->
            config.timeZone = timeZone
            configurationDao.update(config)
        }
        configure(ConfigurationParam.CALENDAR_DOMAIN, calendarDomain)
        configure(ConfigurationParam.SYSTEM_ADMIN_E_MAIL, sysopEMail)
        configure(ConfigurationParam.FEEDBACK_E_MAIL, feedbackEMail)

        pluginAdminService.afterSetup()
        pluginAdminService.activatePluginsAfterSetup()
        testData?.let {
            testDataSeeder.seedPlugins(it) { fraction -> progress = 90 + (fraction * 9).toInt() }
            databaseService.afterCreatedTestDb(false)
        }
        // Skipped on start because the database was empty then.
        e2eTestAccountsService.syncAccounts()

        log.info { "Setup finished successfully." }
        return updatedAdmin
    }

    private fun configure(param: ConfigurationParam, value: String?) {
        if (value.isNullOrBlank()) return
        val config = configurationDao.getEntry(param)
        if (config != null) {
            config.stringValue = value
            configurationDao.update(config)
        } else {
            log.warn { "Configuration param $param not found — can be configured later in admin settings." }
        }
    }
}
