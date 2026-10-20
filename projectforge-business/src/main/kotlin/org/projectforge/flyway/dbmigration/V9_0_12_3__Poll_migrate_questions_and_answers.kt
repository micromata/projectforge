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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import java.sql.Connection
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Moves the polls into the columns of V9.0.12.1:
 * - the state, stored as ordinal so far, by name (a finished poll whose mail was sent is just finished),
 * - the attendees into `attendee_user_ids`/`attendee_group_ids`,
 * - the questions (`inputFields`) into `questions`, every option with a uid of its own, every question with a uid
 *   unique within its poll (a template taken twice into a poll had duplicates),
 * - the answers (`responses`) into `answers`, naming the chosen options by uid. They are matched to the questions by
 *   position, the way the old form bound them (`responses[i]` to `inputFields[i]`).
 *
 * Of several responses of one user to one poll, the latest is kept: one response per user and poll is a unique
 * constraint afterwards. Finally the old columns are dropped (the attending groups were never stored, the old form
 * resolved them into users on the fly). This DDL is the same for PostgreSQL and HSQLDB, so it is done here rather than
 * in an SQL script of its own after this migration.
 *
 * The written json is plain maps, not the classes of today, so this migration keeps working when they change.
 */
@Suppress("ClassName")
class V9_0_12_3__Poll_migrate_questions_and_answers : BaseJavaMigration() {
    companion object {
        private val mapper = ObjectMapper()

        private val QUESTION_TYPES = mapOf(
            "PollTextQuestion" to "TEXT",
            "PollSingleResponseQuestion" to "SINGLE_CHOICE",
            "PollMultiResponseQuestion" to "MULTIPLE_CHOICE",
        )

        /** RUNNING or FINISHED, of the ordinal (or name) stored so far. */
        internal fun convertState(state: String?): String {
            return when (state?.trim()) {
                "0", "RUNNING", null, "" -> "RUNNING"
                else -> "FINISHED"
            }
        }

        /** A csv list of ids, sorted, without duplicates; null if empty. */
        internal fun normalizeIds(ids: String?): String? {
            ids ?: return null
            return ids.split(",").mapNotNull { it.trim().toLongOrNull() }.distinct().sorted()
                .takeIf { it.isNotEmpty() }?.joinToString(",")
        }

        /** The questions of the new model (maps of uid, text, type, options). */
        internal fun convertQuestions(inputFields: String?): List<Map<String, Any?>> {
            if (inputFields.isNullOrBlank()) return emptyList()
            val usedUids = mutableSetOf<String>()
            return (mapper.readValue(inputFields, List::class.java) as List<*>).filterIsInstance<Map<*, *>>().map { field ->
                var uid = field["uid"] as? String
                if (uid.isNullOrBlank() || !usedUids.add(uid)) {
                    uid = UUID.randomUUID().toString()
                    usedUids.add(uid)
                }
                val type = QUESTION_TYPES[field["type"] as? String] ?: "TEXT"
                val options = if (type == "TEXT") {
                    emptyList()
                } else {
                    (field["answers"] as? List<*>).orEmpty().map {
                        mapOf("uid" to UUID.randomUUID().toString(), "text" to it?.toString())
                    }
                }
                mapOf("uid" to uid, "text" to field["question"] as? String, "type" to type, "options" to options)
            }
        }

        /** The answers of the new model to the given (converted) questions, matched by position. */
        internal fun convertAnswers(responses: String?, questions: List<Map<String, Any?>>): List<Map<String, Any?>> {
            if (responses.isNullOrBlank()) return emptyList()
            val old = (mapper.readValue(responses, List::class.java) as List<*>).filterIsInstance<Map<*, *>>()
            return questions.mapIndexedNotNull { index, question ->
                val values = (old.getOrNull(index)?.get("answers") as? List<*>) ?: return@mapIndexedNotNull null
                if (question["type"] == "TEXT") {
                    val text = values.firstOrNull()?.toString()?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                    mapOf("questionUid" to question["uid"], "text" to text, "optionUids" to emptyList<String>())
                } else {
                    val options = (question["options"] as List<*>).filterIsInstance<Map<*, *>>()
                    val chosen = values.mapIndexedNotNull { optionIndex, value ->
                        if (value == true || value?.toString() == "true") options.getOrNull(optionIndex)?.get("uid") else null
                    }
                    if (chosen.isEmpty()) return@mapIndexedNotNull null
                    mapOf("questionUid" to question["uid"], "text" to null, "optionUids" to chosen)
                }
            }
        }
    }

    override fun migrate(context: Context) {
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 (see V8_0_7).
        val connection = context.connection
        try {
            connection.autoCommit = true
            migratePolls(connection)
            dropOldColumns(connection)
        } catch (ex: Exception) {
            log.error(ex) { ex.message }
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun migratePolls(connection: Connection) {
        val questionsByPoll = mutableMapOf<Long, List<Map<String, Any?>>>()
        connection.prepareStatement(
            "UPDATE t_poll SET state=?, attendee_user_ids=?, attendee_group_ids=?, full_access_user_ids=?, full_access_group_ids=?, questions=? WHERE pk=?"
        ).use { update ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT pk, state, attendeeIds, groupAttendeeIds, full_access_user_ids, full_access_group_ids, inputFields FROM t_poll"
                ).use { rs ->
                    while (rs.next()) {
                        val pk = getLong(rs, "pk")!!
                        val questions = convertQuestions(rs.getString("inputFields"))
                        questionsByPoll[pk] = questions
                        update.setString(1, convertState(rs.getString("state")))
                        update.setString(2, normalizeIds(rs.getString("attendeeIds")))
                        update.setString(3, normalizeIds(rs.getString("groupAttendeeIds")))
                        update.setString(4, normalizeIds(rs.getString("full_access_user_ids")))
                        update.setString(5, normalizeIds(rs.getString("full_access_group_ids")))
                        update.setString(6, mapper.writeValueAsString(questions))
                        update.setLong(7, pk)
                        update.executeUpdate()
                    }
                }
            }
        }
        // The latest response of a user to a poll is kept, the others removed.
        val kept = mutableMapOf<Pair<Long, Long>, Long>()
        val removed = mutableListOf<Long>()
        var migrated = 0
        connection.prepareStatement("UPDATE t_poll_response SET answers=? WHERE pk=?").use { update ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT pk, poll_fk, owner_fk, responses FROM t_poll_response ORDER BY last_update DESC, pk DESC"
                ).use { rs ->
                    while (rs.next()) {
                        val pk = getLong(rs, "pk")!!
                        val pollId = getLong(rs, "poll_fk")!!
                        val key = pollId to getLong(rs, "owner_fk")!!
                        if (kept.containsKey(key)) {
                            removed.add(pk)
                            continue
                        }
                        kept[key] = pk
                        val answers = convertAnswers(rs.getString("responses"), questionsByPoll[pollId].orEmpty())
                        update.setString(1, mapper.writeValueAsString(answers))
                        update.setLong(2, pk)
                        update.executeUpdate()
                        ++migrated
                    }
                }
            }
        }
        connection.prepareStatement("DELETE FROM t_poll_response WHERE pk=?").use { delete ->
            removed.forEach {
                delete.setLong(1, it)
                delete.executeUpdate()
            }
        }
        log.info { "Migrated ${questionsByPoll.size} polls and $migrated responses, removed ${removed.size} duplicate responses." }
    }

    internal fun dropOldColumns(connection: Connection) {
        connection.createStatement().use { statement ->
            listOf(
                "ALTER TABLE t_poll DROP COLUMN attendeeIds",
                "ALTER TABLE t_poll DROP COLUMN groupAttendeeIds",
                "ALTER TABLE t_poll DROP COLUMN inputFields",
                "ALTER TABLE t_poll_response DROP COLUMN responses",
                // Also the index of the responses of a poll:
                "ALTER TABLE t_poll_response ADD CONSTRAINT unique_t_poll_response_poll_owner UNIQUE (poll_fk, owner_fk)",
            ).forEach { statement.execute(it) }
        }
    }
}
