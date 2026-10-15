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

package org.projectforge.flyway.dbmigration

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import java.sql.Connection

private val log = KotlinLogging.logger {}

/**
 * Moves the project manager, head of business manager and sales manager of each order into the new list of
 * additional contacts (column additional_contact_user_ids, see V8.0.35): in this order, without duplicates and
 * without the main contact person. The old columns are only read, never changed.
 */
@Suppress("ClassName")
class V8_0_36__Auftrag_migrate_additional_contacts : BaseJavaMigration() {
    companion object {
        private val SELECT_ORDERS = """
            SELECT pk, contact_person_fk, projectmanager_fk, headofbusinessmanager_fk, salesmanager_fk
            FROM t_fibu_auftrag
            WHERE projectmanager_fk IS NOT NULL OR headofbusinessmanager_fk IS NOT NULL OR salesmanager_fk IS NOT NULL
            """.trimIndent()
        private const val UPDATE_ORDER = "UPDATE t_fibu_auftrag SET additional_contact_user_ids=? WHERE pk=?"

        /**
         * The additional contacts of an order, formatted as `User.toLongList` does (", " separated).
         * @return null if there is none.
         */
        internal fun buildAdditionalContacts(contactPerson: Long?, vararg managers: Long?): String? {
            return managers.filterNotNull().distinct().filter { it != contactPerson }
                .takeIf { it.isNotEmpty() }?.joinToString(", ")
        }
    }

    override fun migrate(context: Context) {
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 (see V8_0_7).
        val connection = context.connection
        try {
            connection.autoCommit = true
            migrateOrders(connection)
        } catch (ex: Exception) {
            log.error(ex) { ex.message }
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun migrateOrders(connection: Connection) {
        var counter = 0
        connection.prepareStatement(UPDATE_ORDER).use { update ->
            connection.createStatement().use { statement ->
                statement.executeQuery(SELECT_ORDERS).use { resultSet ->
                    while (resultSet.next()) {
                        val ids = buildAdditionalContacts(
                            getLong(resultSet, "contact_person_fk"),
                            getLong(resultSet, "projectmanager_fk"),
                            getLong(resultSet, "headofbusinessmanager_fk"),
                            getLong(resultSet, "salesmanager_fk"),
                        ) ?: continue
                        update.setString(1, ids)
                        update.setLong(2, getLong(resultSet, "pk")!!)
                        update.executeUpdate()
                        ++counter
                    }
                }
            }
        }
        log.info { "Migrated the managers of $counter orders into their additional contacts." }
    }
}
