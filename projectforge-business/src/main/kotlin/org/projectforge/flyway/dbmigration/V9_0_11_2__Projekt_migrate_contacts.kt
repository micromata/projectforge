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
 * Moves the project manager, head of business manager and sales manager of each project into its new contacts
 * (columns contact_person_fk and additional_contact_user_ids, see V9.0.11.1), as V8_0_36 did for orders: the first
 * manager given (in this order) becomes the main contact person, the others the further contacts, without
 * duplicates. The old columns are only read, never changed.
 */
@Suppress("ClassName")
class V9_0_11_2__Projekt_migrate_contacts : BaseJavaMigration() {
    /** The contacts of one project: the main contact person and the further ones as stored (", " separated). */
    internal data class Contacts(val contactPerson: Long?, val additionalContacts: String?)

    companion object {
        private val SELECT_PROJECTS = """
            SELECT pk, projectmanager_fk, headofbusinessmanager_fk, salesmanager_fk
            FROM t_fibu_projekt
            WHERE projectmanager_fk IS NOT NULL OR headofbusinessmanager_fk IS NOT NULL OR salesmanager_fk IS NOT NULL
            """.trimIndent()
        private const val UPDATE_PROJECT =
            "UPDATE t_fibu_projekt SET contact_person_fk=?, additional_contact_user_ids=? WHERE pk=?"

        /**
         * Self-contained on purpose (not ContactUserIds): a migration must keep doing what it did when it shipped.
         */
        internal fun buildContacts(vararg managers: Long?): Contacts {
            val ids = managers.filterNotNull().distinct()
            return Contacts(ids.firstOrNull(), ids.drop(1).takeIf { it.isNotEmpty() }?.joinToString(", "))
        }
    }

    override fun migrate(context: Context) {
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 (see V8_0_7).
        val connection = context.connection
        try {
            connection.autoCommit = true
            migrateProjects(connection)
        } catch (ex: Exception) {
            log.error(ex) { ex.message }
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun migrateProjects(connection: Connection) {
        var counter = 0
        connection.prepareStatement(UPDATE_PROJECT).use { update ->
            connection.createStatement().use { statement ->
                statement.executeQuery(SELECT_PROJECTS).use { resultSet ->
                    while (resultSet.next()) {
                        val contacts = buildContacts(
                            getLong(resultSet, "projectmanager_fk"),
                            getLong(resultSet, "headofbusinessmanager_fk"),
                            getLong(resultSet, "salesmanager_fk"),
                        )
                        val contactPerson = contacts.contactPerson ?: continue
                        update.setLong(1, contactPerson)
                        update.setString(2, contacts.additionalContacts)
                        update.setLong(3, getLong(resultSet, "pk")!!)
                        update.executeUpdate()
                        ++counter
                    }
                }
            }
        }
        log.info { "Migrated the managers of $counter projects into their contacts." }
    }
}
