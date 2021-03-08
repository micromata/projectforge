/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.flyway.dbmigration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.projectforge.ProjectForgeApp
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.framework.utils.Crypt
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.min

/**
 * EmployeeDO will now support timed annual leave days instead of fixed annual leave days (aka urlaubstage). So [org.projectforge.business.vacation.service.VacationStats]
 * may be calculated for former years properly if the amount of annual leave days was changed.
 */
class V7_0_0_15__AuthenticationToken : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val configFile = File(System.getProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR), "projectforge.properties")
        log.info("Trying to read configuration from '${configFile.absolutePath}'...")
        val configuration = Properties()
        val param = "projectforge.security.authenticationTokenEncryptionKey"
        configFile.inputStream().use { configuration.load(it) }
        var authenticationTokenEncryptionKey = configuration[param] as? String
        if (authenticationTokenEncryptionKey.isNullOrBlank()) {
            EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.IMPORTANT, EmphasizedLogSupport.Alignment.LEFT)
                    .setLogLevel(EmphasizedLogSupport.LogLevel.WARN)
                    .log("No authenticationTokenEncryptionKey found:")
                    .log("")
                    .log("Defining and appending a random generated key:")
                    .log("")
                    .log("'${configFile.absolutePath}':")
                    .log("projectforge.security.authenticationTokenEncryptionKey=..." )
                    .logEnd()
            authenticationTokenEncryptionKey =  NumberHelper.getSecureRandomAlphanumeric(20)
            val text = "# All authentication tokens of the user's will be encrypted with this key.\n" +
                    "# If you loose this key or if you change it later, all users have to renew their authentication passwords (their passwords will NOT be affected).\n" +
                    "projectforge.security.authenticationTokenEncryptionKey=$authenticationTokenEncryptionKey\n"
            configFile.appendText(text, StandardCharsets.UTF_8)
        }
        log.info("Using authenticationToken '${authenticationTokenEncryptionKey.substring(0..min(3, authenticationTokenEncryptionKey.length))}....'")
        val ds = context.configuration.dataSource
        log.info("Trying to migrate authentication token of t_pf_user.authentication_token.")
        val jdbc = JdbcTemplate(ds)
        val rs = jdbc.queryForRowSet("select u.pk as userId, u.authentication_token as token, u.stay_logged_in_key as stayLoggedInKey from t_pf_user as u")
        var counter = 0
        val now = Date()
        while (rs.next()) {
            val userId = rs.getInt("userId")
            val token = rs.getString("token")
            val stayLoggedInKey = rs.getString("stayLoggedInKey")
            if (token.isNullOrBlank() && stayLoggedInKey.isNullOrBlank()) {
                continue
            }
            ++counter
            var simpleJdbcInsert = SimpleJdbcInsert(ds).withTableName("T_PF_USER_AUTHENTICATIONS")
            val parameters = mutableMapOf<String, Any?>()
            parameters["pk"] = counter
            parameters["deleted"] = false
            parameters["createdat"] = now
            parameters["createdby"] = "anon"
            parameters["modifiedat"] = now
            parameters["modifiedby"] = "anon"
            parameters["user_id"] = userId
            val encryptedToken = if (token.isNullOrBlank()) null else Crypt.encrypt(authenticationTokenEncryptionKey, token)
            parameters["calendar_export_token"] = encryptedToken
            val encryptedStayLoggedInKey = if (stayLoggedInKey.isNullOrBlank()) null else Crypt.encrypt(authenticationTokenEncryptionKey, stayLoggedInKey)
            parameters["stay_logged_in_key"] = encryptedStayLoggedInKey
            simpleJdbcInsert.execute(parameters)
        }
        if (counter > 0) { // counter > 0
            log.info("Number of successful migrated pf_user entries: $counter")
        } else {
            log.info("No pf_user entries found to migrate (OK for empty database or no user with authentication_token or stay_logged_in_key was found).")
        }
    }

    private val log = LoggerFactory.getLogger(V7_0_0_15__AuthenticationToken::class.java)
}
