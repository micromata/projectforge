/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Address images were moved to separate entity (db table). So do the migration here.
 */
class V7_4_1_3__ReleaseUserPassword : BaseJavaMigration() {
  class UserPassword(
    val userId: Int?,
    val userName: String?,
    val passwordHash: String?,
    val passwordSalt: String?,
  )

  override fun migrate(context: Context) {
    log.info("Migrating passwords from t_pf_user to t_pf_user_password...'")
    val ds = context.configuration.dataSource
    val jdbc = JdbcTemplate(ds)
    var counter = 0
    val now = Date()
    val rowMapper = RowMapper { rs, _ ->
      UserPassword(
        rs.getInt(UserPassword::userId.name),
        rs.getString(UserPassword::userName.name),
        rs.getString(UserPassword::passwordHash.name),
        rs.getString(UserPassword::passwordSalt.name),
      )
    }
    jdbc.query(
      "select u.pk as userId, u.username as userName, u.password as passwordHash, u.password_salt as passwordSalt from t_pf_user as u",
      rowMapper
    )
      .forEach {
        log.info { "Migrating password hashes of user '${it.userName}' (${it.userId})..." }
        val parameters = mutableMapOf<String, Any?>()
        parameters["pk"] = ++counter
        parameters["user_id"] = it.userId
        parameters["deleted"] = false
        parameters["created"] = now
        parameters["last_update"] = now
        parameters["password_hash"] = it.passwordHash
        parameters["password_salt"] = it.passwordSalt
        SimpleJdbcInsert(ds).withTableName("T_PF_USER_PASSWORD").execute(parameters)
      }
    if (counter > 0) { // counter > 0
      log.info("Number of successful migrated user passwords: $counter")
    } else {
      log.info("No user passwords found to migrate (OK, for new systems).")
    }
  }
}
