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

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate

/**
 * VacationDO will only have one replacement (coverage during leave) instead of set of replacements.
 */
class V7_0_0_10__MigrateEmployeeVacationReplacement : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val ds = context.configuration.dataSource
        log.info("Trying to migrate vacation replacements (only one person as holiday replacement instead of several).")
        val jdbc = JdbcTemplate(ds)
        val rs = jdbc.queryForRowSet("select p.vacation_id as vacation_id, p.substitution_id as substitution_id, v.comment as comment, u.firstname as substitution_firstname, u.lastname as substitution_lastname "
                + "from t_employee_vacation_substitution as p "
                + "inner join t_employee_vacation as v on p.vacation_id=v.pk "
                + "inner join t_fibu_employee as e on p.substitution_id=e.pk "
                + "inner join t_pf_user as u on e.user_id=u.pk order by vacation_id")
        var counter = 0
        var list = mutableListOf<Replacement>()
        var lastVacationId: Int? = null
        while (rs.next()) {
            val vacationId = rs.getInt("vacation_id")
            if (counter == 0) {
                lastVacationId = vacationId
            }
            ++counter
            if (vacationId != lastVacationId) { // New entry. Write previous substitutes:
                if (lastVacationId != null) // null shouldn't occur.
                    writeReplacement(jdbc, list, lastVacationId)
                list.clear()
                lastVacationId = vacationId
            }
            val substitutionId = rs.getInt("substitution_id")
            val firstname = rs.getString("substitution_firstname")
            val lastname = rs.getString("substitution_lastname")
            val comment = rs.getString("comment")
            list.add(Replacement(substitutionId, firstname, lastname, comment))
        }
        if (lastVacationId != null) { // counter > 0
            writeReplacement(jdbc, list, lastVacationId)
            log.info("Number of successful migrated vacation entries: $counter")
        } else {
            log.info("No vacation entries found to migrate (OK, if vacation entries aren't used.)")
        }
    }

    private fun writeReplacement(jdbc: JdbcTemplate, list: List<Replacement>, vacationId: Int) {
        if (list.isEmpty()) {
            return // Empty list of replacements.
        }
        val replacementId = list[0].substitutionId
        val comment = list[0].comment
        var replacements = if (list.size > 1) {
            list.subList(1, list.size).joinToString(", ") { "${it.firstname} ${it.lastname}" }
        } else ""
        val newComment = if (comment.isNullOrBlank()) replacements else "$comment $replacements"
        jdbc.update("update t_employee_vacation set replacement_id=?, comment=? where pk=?", replacementId, newComment, vacationId);
    }

    private inner class Replacement(var substitutionId: Int, var firstname: String?, var lastname: String?, var comment: String?)

    private val log = LoggerFactory.getLogger(V7_0_0_10__MigrateEmployeeVacationReplacement::class.java)
}
