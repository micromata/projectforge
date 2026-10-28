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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Runs the scripts of the poll migration (V9.0.12.1 to V9.0.12.3, HSQLDB variant) on the tables as V7.5.1.2
 * created them, filled as the old React pages stored polls and responses.
 */
class V9_0_12_PollMigrationHsqldbTest {
    private val migrateDir = File("src/main/resources/flyway/migrate")

    @Test
    fun `old polls and responses are migrated`() {
        DriverManager.getConnection("jdbc:hsqldb:mem:pollMigration;sql.syntax_pgs=true", "sa", "").use { connection ->
            val oldScript = File(migrateDir, "common/V7.5.1.2__7.5.1.0-POLL.sql").readText()
                // No user table here:
                .replace(Regex("""ALTER TABLE T_POLL(_RESPONSE)?\s+ADD CONSTRAINT fk_t_poll(_response)?_pf_user[^;]*;"""), "")
            execute(connection, oldScript)
            val inputFields = """[
                {"uid":"a","question":"Coming?","type":"PollSingleResponseQuestion","answers":["Yes","No"]},
                {"uid":"a","question":"Remarks","type":"PollTextQuestion","answers":[]}
            ]"""
            execute(
                connection,
                """INSERT INTO t_poll (pk, deleted, title, owner_fk, deadline, state, attendeeIds, full_access_user_ids, inputFields)
                   VALUES (1, false, 'Party', 10, DATE '2026-01-10', '2', '12, 11', '13', '${inputFields.replace("\n", "")}');
                   INSERT INTO t_poll_response (pk, deleted, last_update, poll_fk, owner_fk, responses)
                   VALUES (2, false, TIMESTAMP '2026-01-01 10:00:00', 1, 11, '[{"answers":[true,false]},{"answers":["old"]}]');
                   INSERT INTO t_poll_response (pk, deleted, last_update, poll_fk, owner_fk, responses)
                   VALUES (3, false, TIMESTAMP '2026-01-02 10:00:00', 1, 11, '[{"answers":[false,"true"]},{"answers":["new"]}]');"""
            )
            execute(connection, File(migrateDir, "common/V9.0.12.1__RELEASE-Poll_cleanup.sql").readText())
            execute(connection, File(migrateDir, "hsqldb/V9.0.12.2__RELEASE-Poll_column_lengths.sql").readText())
            V9_0_12_3__Poll_migrate_questions_and_answers().also {
                it.migratePolls(connection)
                it.dropOldColumns(connection)
            }

            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT state, attendee_user_ids, full_access_user_ids, questions, template FROM t_poll").use {
                    it.next()
                    assertEquals("FINISHED", it.getString("state"))
                    assertEquals("11,12", it.getString("attendee_user_ids"))
                    assertEquals("13", it.getString("full_access_user_ids"))
                    assertEquals(false, it.getBoolean("template"))
                    val questions = ObjectMapper().readValue(it.getString("questions"), List::class.java)
                    assertEquals(2, questions.size)
                }
                statement.executeQuery("SELECT pk, answers FROM t_poll_response").use {
                    it.next()
                    // The latest response is kept:
                    assertEquals(3, it.getLong("pk"))
                    val answers = ObjectMapper().readValue(it.getString("answers"), List::class.java)
                    assertEquals("new", (answers[1] as Map<*, *>)["text"])
                    assertEquals(false, it.next())
                }
            }
            // One response per user and poll:
            assertThrows(SQLException::class.java) {
                execute(connection, "INSERT INTO t_poll_response (pk, deleted, poll_fk, owner_fk) VALUES (4, false, 1, 11);")
            }
        }
    }

    private fun execute(connection: Connection, script: String) {
        script.lines().filter { !it.trim().startsWith("--") }.joinToString("\n").split(";")
            .map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { sql -> connection.createStatement().use { it.execute(sql) } }
    }
}
