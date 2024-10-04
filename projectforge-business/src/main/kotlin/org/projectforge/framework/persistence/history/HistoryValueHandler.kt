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

package org.projectforge.framework.persistence.history

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.*

/**
 * Deserializes values of the history attr table and format the values for displaying.
 */
interface HistoryValueHandler<T> {
    fun serialize(value: T): String {
        return value.toString()
    }

    fun deserialize(value: String): T?
    fun format(value: T): String
    //fun accept(propertyType: Class<*>): Boolean
}

open class DefaultHistoryValueHandler : HistoryValueHandler<Any> {
    override fun deserialize(value: String): Any? {
        return value
    }

    override fun format(value: Any): String {
        return value.toString()
    }

    /*    override fun accept(propertyType: Class<*>): Boolean {
            return propertyType == Any::class.java
        }*/
}

// java.util.Date, net.fortuna.ical4j.model.Date, net.fortuna.ical4j.model.DateTime
class DateHistoryValueHandler : HistoryValueHandler<Date> {
    override fun serialize(value: Date): String {
        return PFDateTimeUtils.formatUTCDate(value)
    }

    override fun deserialize(value: String): Date? {
        return PFDateTimeUtils.parse(value)?.utilDate
    }

    override fun format(value: Date): String {
        return PFDateTime.from(value).format()
    }
}

class LocalDateHistoryValueHandler : HistoryValueHandler<LocalDate> {
    override fun serialize(value: LocalDate): String {
        return value.toString()
    }

    override fun deserialize(value: String): LocalDate? {
        if (value.length > 10 && value.contains(':')) {
            // Format: 2017-03-16 23:00:00:000
            PFDateTimeUtils.parse(value)?.let { date ->
                return date.localDate
            }
        }
        return PFDayUtils.parseDate(value)
    }

    override fun format(value: LocalDate): String {
        return PFDay.from(value).format()
    }
}

class SqlDateHistoryValueHandler : HistoryValueHandler<java.sql.Date> {
    override fun serialize(value: java.sql.Date): String {
        return value.toLocalDate().toString()
    }

    override fun deserialize(value: String): java.sql.Date? {
        if (value.length > 10 && value.contains(':')) {
            // Format: 2017-03-16 23:00:00:000
            PFDateTimeUtils.parse(value)?.let { date ->
                return date.sqlDate
            }
        }
        val localDate = PFDayUtils.parseDate(value) ?: return null
        return java.sql.Date.valueOf(localDate)
    }

    override fun format(value: java.sql.Date): String {
        return PFDay.from(value).format()
    }
}

class TimestampHistoryValueHandler : HistoryValueHandler<Timestamp> {
    override fun serialize(value: Timestamp): String {
        return PFDateTimeUtils.formatUTCDate(value)
    }

    override fun deserialize(value: String): Timestamp? {
        return PFDateTimeUtils.parse(value)?.sqlTimestamp
    }

    override fun format(value: Timestamp): String {
        return PFDateTime.from(value).format()
    }
}

// boolean, java.lang.Boolean
class BooleanHistoryValueHandler : HistoryValueHandler<Boolean> {
    override fun deserialize(value: String): Boolean? {
        // Only three entries do exist in the data base: NULL, false, true
        return when (value) {
            "true" -> true
            "NULL" -> null
            else -> false
        }
    }

    override fun format(value: Boolean): String {
        return translate(value)
    }
}

// int, java.lang.Integer
class IntHistoryValueHandler : HistoryValueHandler<Int> {
    override fun deserialize(value: String): Int? {
        return value.toIntOrNull()
    }

    override fun format(value: Int): String {
        return NumberFormatter.format(value)
    }
}

// java.lang.Short
class ShortHistoryValueHandler : HistoryValueHandler<Short> {
    override fun deserialize(value: String): Short? {
        return value.toShortOrNull()
    }

    override fun format(value: Short): String {
        return NumberFormatter.format(value)
    }
}

// java.math.BigDecimal
class BigDecimalHistoryValueHandler : HistoryValueHandler<BigDecimal> {
    override fun deserialize(value: String): BigDecimal? {
        return value.toBigDecimalOrNull()
    }

    override fun format(value: BigDecimal): String {
        return NumberFormatter.format(value)
    }
}

// [B Was only use by accident.
class ByteArrayHistoryValueHandler : HistoryValueHandler<ByteArray> {
    override fun serialize(value: ByteArray): String {
        return "byte[${value.size}]" // Don't serialize the content.
    }

    override fun deserialize(value: String): ByteArray? {
        return null
    }

    override fun format(value: ByteArray): String {
        return "[...]" // Don't serialize the content.
    }
}

class LocaleHistoryValueHandler : HistoryValueHandler<Locale> {
    override fun deserialize(value: String): Locale? {
        return Locale(value)
    }

    override fun format(value: Locale): String {
        return value.toString()
    }
}

class VoidHistoryValueHandler : HistoryValueHandler<Void> {
    override fun serialize(value: Void): String {
        return ""
    }

    override fun deserialize(value: String): Void? {
        return null
    }

    override fun format(value: Void): String {
        return ""
    }
}

// All used property_type_class since 2001:
//  [B
// boolean
// int
// java.lang.Boolean
// java.lang.Integer
// java.lang.Short
// java.lang.String
// java.math.BigDecimal
// java.sql.Date
// java.sql.Timestamp
// java.time.LocalDate
// java.util.Date
// java.util.Locale
// net.fortuna.ical4j.model.Date
// net.fortuna.ical4j.model.DateTime
// void
//
// de.micromata.fibu.AuftragsArt
// de.micromata.fibu.AuftragsPositionsArt
// de.micromata.fibu.AuftragsPositionsStatus
// de.micromata.fibu.AuftragsStatus
// de.micromata.fibu.EmployeeStatus
// de.micromata.fibu.kost.KostentraegerStatus
// de.micromata.fibu.KundeStatus
// de.micromata.fibu.ProjektStatus
// de.micromata.fibu.RechnungStatus
// de.micromata.fibu.RechnungTyp
// de.micromata.genome.db.jpa.history.entities.PropertyOpType
// de.micromata.projectforge.address.AddressStatus
// de.micromata.projectforge.address.ContactStatus
// de.micromata.projectforge.address.FormOfAddress
// de.micromata.projectforge.book.BookStatus
// de.micromata.projectforge.core.Priority
// de.micromata.projectforge.humanresources.HRPlanningEntryStatus
// de.micromata.projectforge.orga.PostType
// de.micromata.projectforge.scripting.ScriptParameterType
// de.micromata.projectforge.task.TaskDO
// de.micromata.projectforge.task.TaskStatus
// de.micromata.projectforge.task.TimesheetBookingStatus
// de.micromata.projectforge.user.PFUserDO
// org.projectforge.address.AddressStatus
// org.projectforge.address.ContactStatus
// org.projectforge.address.FormOfAddress
// org.projectforge.book.BookStatus
// org.projectforge.book.BookType
// org.projectforge.business.address.AddressbookDO
// org.projectforge.business.address.AddressDO
// org.projectforge.business.address.AddressDO_$$_jvst148_2f
// org.projectforge.business.address.AddressDO_$$*
// org.projectforge.business.address.AddressDO$HibernateProxy$En8hKwh8
// org.projectforge.business.address.AddressDO$HibernateProxy$*
// org.projectforge.business.address.AddressStatus
// org.projectforge.business.address.ContactStatus
// org.projectforge.business.address.FormOfAddress
// org.projectforge.business.book.BookStatus
// org.projectforge.business.book.BookType
// org.projectforge.business.fibu.AuftragsPositionDO
// org.projectforge.business.fibu.AuftragsPositionDO$HibernateProxy$0HIvA23x
// org.projectforge.business.fibu.AuftragsPositionDO$HibernateProxy$*
// org.projectforge.business.fibu.AuftragsPositionsArt
// org.projectforge.business.fibu.AuftragsPositionsPaymentType
// org.projectforge.business.fibu.AuftragsPositionsStatus
// org.projectforge.business.fibu.AuftragsStatus
// org.projectforge.business.fibu.EingangsrechnungsPositionDO
// org.projectforge.business.fibu.EmployeeDO
// org.projectforge.business.fibu.EmployeeDO_$$_jvst77_1e
// org.projectforge.business.fibu.EmployeeDO_$$*
// org.projectforge.business.fibu.EmployeeSalaryType
// org.projectforge.business.fibu.EmployeeStatus
// org.projectforge.business.fibu.Gender
// org.projectforge.business.fibu.IsoGender
// org.projectforge.business.fibu.KontoDO
// org.projectforge.business.fibu.KontoDO_$$_jvst148_1c
// org.projectforge.business.fibu.KontoDO_$$*
// org.projectforge.business.fibu.KontoDO$HibernateProxy$0DASBQzw
// org.projectforge.business.fibu.KontoDO$HibernateProxy$*
// org.projectforge.business.fibu.KontoStatus
// org.projectforge.business.fibu.kost.Kost1DO
// org.projectforge.business.fibu.kost.Kost2ArtDO
// org.projectforge.business.fibu.kost.Kost2DO
// org.projectforge.business.fibu.kost.Kost2DO$HibernateProxy$0epA96ZQ
// org.projectforge.business.fibu.kost.Kost2DO$HibernateProxy$*
// org.projectforge.business.fibu.kost.KostentraegerStatus
// org.projectforge.business.fibu.kost.KostZuweisungDO
// org.projectforge.business.fibu.kost.SHType
// org.projectforge.business.fibu.KundeDO
// org.projectforge.business.fibu.KundeDO$HibernateProxy$0M8kO4CB
// org.projectforge.business.fibu.KundeDO$HibernateProxy$*
// org.projectforge.business.fibu.KundeStatus
// org.projectforge.business.fibu.ModeOfPaymentType
// org.projectforge.business.fibu.PaymentScheduleDO
// org.projectforge.business.fibu.PaymentType
// org.projectforge.business.fibu.PeriodOfPerformanceType
// org.projectforge.business.fibu.ProjektDO
// org.projectforge.business.fibu.ProjektDO$HibernateProxy$2Zpfmf2Q
// org.projectforge.business.fibu.ProjektDO$HibernateProxy$*
// org.projectforge.business.fibu.ProjektStatus
// org.projectforge.business.fibu.RechnungsPositionDO
// org.projectforge.business.fibu.RechnungStatus
// org.projectforge.business.fibu.RechnungTyp
// org.projectforge.business.gantt.GanttObjectType
// org.projectforge.business.gantt.GanttRelationType
// org.projectforge.business.humanresources.HRPlanningEntryDO
// org.projectforge.business.humanresources.HRPlanningEntryStatus
// org.projectforge.business.orga.ContractStatus
// org.projectforge.business.orga.PostType
// org.projectforge.business.orga.VisitorType
// org.projectforge.business.poll.PollDO
// org.projectforge.business.poll.PollDO$State
// org.projectforge.business.scripting.ScriptDO$ScriptType
// org.projectforge.business.scripting.ScriptParameterType
// org.projectforge.business.task.TaskDO
// org.projectforge.business.task.TaskDO_$$_jvst148_1f
// org.projectforge.business.task.TaskDO_$$*
// org.projectforge.business.task.TaskDO$HibernateProxy$06x39fNa
// org.projectforge.business.task.TaskDO$HibernateProxy$*
// org.projectforge.business.teamcal.admin.model.TeamCalDO
// org.projectforge.business.teamcal.admin.model.TeamCalDO_$$_jvst148_6
// org.projectforge.business.teamcal.admin.model.TeamCalDO_$$*
// org.projectforge.business.teamcal.admin.model.TeamCalDO$HibernateProxy$0igncMuL
// org.projectforge.business.teamcal.admin.model.TeamCalDO$HibernateProxy$*
// org.projectforge.business.teamcal.event.model.ReminderActionType
// org.projectforge.business.teamcal.event.model.ReminderDurationUnit
// org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
// org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus
// org.projectforge.business.user.UserRightValue
// org.projectforge.business.vacation.model.VacationDO
// org.projectforge.business.vacation.model.VacationStatus
// org.projectforge.common.i18n.Priority
// org.projectforge.common.task.TaskStatus
// org.projectforge.common.task.TimesheetBookingStatus
// org.projectforge.common.TimeNotation
// org.projectforge.core.Priority
// org.projectforge.fibu.AuftragsPositionsArt
// org.projectforge.fibu.AuftragsPositionsStatus
// org.projectforge.fibu.AuftragsStatus
// org.projectforge.fibu.EmployeeStatus
// org.projectforge.fibu.KontoDO
// org.projectforge.fibu.KontoStatus
// org.projectforge.fibu.kost.KostentraegerStatus
// org.projectforge.fibu.kost.SHType
// org.projectforge.fibu.KundeStatus
// org.projectforge.fibu.ModeOfPaymentType
// org.projectforge.fibu.PaymentType
// org.projectforge.fibu.PeriodOfPerformanceType
// org.projectforge.fibu.ProjektStatus
// org.projectforge.fibu.RechnungStatus
// org.projectforge.fibu.RechnungTyp
// org.projectforge.framework.access.AccessEntryDO
// org.projectforge.framework.configuration.ConfigurationType
// org.projectforge.framework.persistence.user.entities.Gender
// org.projectforge.framework.persistence.user.entities.GroupDO
// org.projectforge.framework.persistence.user.entities.GroupDO_$$_jvst148_37
// org.projectforge.framework.persistence.user.entities.GroupDO_$$*
// org.projectforge.framework.persistence.user.entities.GroupDO$HibernateProxy$EgauAiy8
// org.projectforge.framework.persistence.user.entities.GroupDO$HibernateProxy$*
// org.projectforge.framework.persistence.user.entities.PFUserDO
// org.projectforge.framework.persistence.user.entities.PFUserDO_$$_jvst148_56
// org.projectforge.framework.persistence.user.entities.PFUserDO_$$*
// org.projectforge.framework.persistence.user.entities.PFUserDO$HibernateProxy$1BBv5CQJ
// org.projectforge.framework.persistence.user.entities.PFUserDO$HibernateProxy$*
// org.projectforge.framework.persistence.user.entities.TenantDO
// org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvst148_1e
// org.projectforge.framework.persistence.user.entities.TenantDO_$$*
// org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$0CoFsucl
// org.projectforge.framework.persistence.user.entities.TenantDO$HibernateProxy$*
// org.projectforge.framework.time.TimeNotation
// org.projectforge.gantt.GanttDependencyType
// org.projectforge.gantt.GanttObjectType
// org.projectforge.gantt.GanttRelationType
// org.projectforge.humanresources.HRPlanningEntryStatus
// org.projectforge.orga.PostType
// org.projectforge.plugins.banking.BankAccountDO
// org.projectforge.plugins.ffp.model.FFPAccountingDO
// org.projectforge.plugins.ffp.model.FFPEventDO
// org.projectforge.plugins.marketing.AddressCampaignDO
// org.projectforge.plugins.marketing.AddressCampaignDO_$$_jvst148_26
// org.projectforge.plugins.marketing.AddressCampaignDO_$$*
// org.projectforge.plugins.skillmatrix.SkillDO
// org.projectforge.plugins.skillmatrix.SkillDO_$$_jvstf4d_19
// org.projectforge.plugins.skillmatrix.SkillRating
// org.projectforge.plugins.teamcal.event.ReminderActionType
// org.projectforge.plugins.teamcal.event.ReminderDurationUnit
// org.projectforge.plugins.todo.ToDoStatus
// org.projectforge.plugins.todo.ToDoType
// org.projectforge.scripting.ScriptParameterType
// org.projectforge.task.TaskDO
// org.projectforge.task.TaskStatus
// org.projectforge.task.TimesheetBookingStatus
// org.projectforge.user.PFUserDO
// org.projectforge.user.UserRightValue
