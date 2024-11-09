/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import org.projectforge.business.task.TaskDO
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.persistence.database.JdbcUtils.getBigDecimal
import org.projectforge.framework.persistence.database.JdbcUtils.getBoolean
import org.projectforge.framework.persistence.database.JdbcUtils.getDate
import org.projectforge.framework.persistence.database.JdbcUtils.getInt
import org.projectforge.framework.persistence.database.JdbcUtils.getLocalDate
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import org.projectforge.framework.persistence.database.JdbcUtils.getShort
import org.projectforge.framework.persistence.database.JdbcUtils.getString
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Uses jdbc instead of Hibernate for faster access to the database.
 */
@Service
class AuftragsJdbcService {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    fun selectAuftragsList(): List<AuftragDO> {
        return jdbcTemplate.query(SELECT_ORDERS, object : RowMapper<AuftragDO> {
            @Throws(SQLException::class)
            override fun mapRow(rs: ResultSet, rowNum: Int): AuftragDO {
                return AuftragDO().also {
                    it.id = getLong(rs, "pk")
                    it.nummer = getInt(rs, "nummer")
                    it.deleted = rs.getBoolean("deleted")
                    it.titel = getString(rs, "titel")
                    it.auftragsStatus = AuftragsStatus.safeValueOf(rs.getString("status"))
                    it.angebotsDatum = getLocalDate(rs, "angebots_datum")
                    it.created = getDate(rs, "created")
                    it.erfassungsDatum = getLocalDate(rs, "erfassungs_datum")
                    it.entscheidungsDatum = getLocalDate(rs, "entscheidungs_datum")
                    it.probabilityOfOccurrence = getInt(rs, "probability_of_occurrence")
                    it.periodOfPerformanceBegin = getLocalDate(rs, "period_of_performance_begin")
                    it.periodOfPerformanceEnd = getLocalDate(rs, "period_of_performance_end")
                    it.bemerkung = getString(rs, "bemerkung")?.abbreviate(30)
                    getLong(rs, "contact_person_fk")?.let { userId ->
                        it.contactPerson = PFUserDO().also { it.id = userId }
                    }
                }
            }
        })
    }

    fun selectNonDeletedAuftragsPositions(): List<AuftragsPositionDO> {
        return jdbcTemplate.query(SELECT_POSITIONS, object : RowMapper<AuftragsPositionDO> {
            @Throws(SQLException::class)
            override fun mapRow(rs: ResultSet, rowNum: Int): AuftragsPositionDO {
                return AuftragsPositionDO().also {
                    it.id = getLong(rs, "pk")
                    getLong(rs, "auftrag_fk")?.let { auftragId -> it.auftrag = AuftragDO().also { it.id = auftragId } }
                    it.number = rs.getShort("number")
                    it.deleted = rs.getBoolean("deleted")
                    it.titel = getString(rs, "titel")
                    it.status = AuftragsPositionsStatus.safeValueOf(rs.getString("status"))
                    it.paymentType = AuftragsPositionsPaymentType.safeValueOf(rs.getString("paymenttype"))
                    it.art = AuftragsPositionsArt.safeValueOf(rs.getString("art"))
                    it.personDays = getBigDecimal(rs, "person_days")
                    it.nettoSumme = getBigDecimal(rs, "netto_summe")
                    it.vollstaendigFakturiert = getBoolean(rs, "vollstaendig_fakturiert")
                    it.periodOfPerformanceType =
                        PeriodOfPerformanceType.safeValueOf(rs.getString("period_of_performance_type"))
                    it.periodOfPerformanceBegin = getLocalDate(rs, "period_of_performance_begin")
                    it.periodOfPerformanceEnd = getLocalDate(rs, "period_of_performance_end")
                    getLong(rs, "task_fk")?.let { taskId -> it.task = TaskDO().also { it.id = taskId } }
                    it.bemerkung = getString(rs, "bemerkung")?.abbreviate(30)
                }
            }
        })
    }

    fun selectNonDeletedPaymentSchedules(): List<PaymentScheduleDO> {
        return jdbcTemplate.query(SELECT_PAYMENTS_SCHEDULES, object : RowMapper<PaymentScheduleDO> {
            @Throws(SQLException::class)
            override fun mapRow(rs: ResultSet, rowNum: Int): PaymentScheduleDO {
                return PaymentScheduleDO().also {
                    it.id = getLong(rs, "pk")
                    it.number = rs.getShort("number")
                    it.deleted = rs.getBoolean("deleted")
                    it.created = getDate(rs, "created")
                    getLong(rs, "auftrag_id")?.let { auftragId -> it.auftrag = AuftragDO().also { it.id = auftragId } }
                    it.amount = getBigDecimal(rs, "amount")
                    it.reached = rs.getBoolean("reached")
                    it.vollstaendigFakturiert = rs.getBoolean("vollstaendig_fakturiert")
                    it.scheduleDate = getLocalDate(rs, "schedule_date")
                    it.positionNumber = getShort(rs, "position_number")
                }
            }
        })
    }

    companion object {
        private val SELECT_ORDERS = """
            SELECT pk,deleted,created,titel,status,nummer,angebots_datum,erfassungs_datum,entscheidungs_datum,probability_of_occurrence,
                   bemerkung,period_of_performance_begin,period_of_performance_end,contact_person_fk
            FROM t_fibu_auftrag
        """.trimIndent()
        private val SELECT_POSITIONS = """
            SELECT pk,auftrag_fk,task_fk,number,deleted,titel,status,paymenttype,art,person_days,netto_summe,vollstaendig_fakturiert,
                   period_of_performance_type,period_of_performance_begin,period_of_performance_end,bemerkung
            FROM t_fibu_auftrag_position
            WHERE deleted = false
        """.trimIndent()
        private val SELECT_PAYMENTS_SCHEDULES = """
            SELECT pk,deleted,created,number,auftrag_id,amount,reached,vollstaendig_fakturiert,schedule_date,position_number
            FROM t_fibu_payment_schedule
            WHERE deleted = false
        """.trimIndent()
    }
}
