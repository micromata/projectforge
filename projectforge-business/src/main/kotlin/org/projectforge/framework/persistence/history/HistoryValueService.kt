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

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.access.AccessEntryDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for deserializing history values.
 */
@Service
class HistoryValueService private constructor() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * Key is the property type in history attr table, value is the class.
     */
    internal val typeClassMapping = mutableMapOf<String, Class<*>>()

    /**
     * Key is the property type in history attr table, value is the [ValueType].
     */
    internal val typeMapping = mutableMapOf<String, ValueType>()

    internal val unknownPropertyTypes = mutableSetOf<String>()

    internal enum class ValueType {
        ENTITY,
        ENUM,
        I18N_ENUM,
        BASE_TYPE,
        BINARY,
        UNKNOWN,
    }

    private val defaultHandler = DefaultHistoryValueHandler()

    init {
        instance = this
    }

    /**
     * @param propertyType The property type in the history table (must be unified before!).
     */
    fun format(valueString: String?, propertyType: String?): String {
        valueString ?: return ""
        if (valueString.isBlank() || propertyType.isNullOrBlank()) {
            return valueString
        }
        return when (getValueType(propertyType)) {
            ValueType.BASE_TYPE -> formatBaseType(valueString)
            ValueType.ENTITY -> formatEntity(valueString, propertyType)
            ValueType.ENUM -> formatEnum(valueString)
            ValueType.I18N_ENUM -> formatI18nEnum(valueString, propertyType)
            ValueType.BINARY -> formatBinary(valueString)
            ValueType.UNKNOWN -> valueString
        }
    }

    fun getObjectValue(value: String?, context: HistoryLoadContext): Any? {
        value ?: return null
        val attr = context.currentHistoryEntryAttr ?: return null
        val valueType = getValueType(attr.propertyTypeClass)
        if (valueType != ValueType.ENTITY) {
            return null
        }
        val propertyTypeClass = getClass(attr.propertyTypeClass) ?: return null
        if (propertyTypeClass == PFUserDO::class.java) {
            if (value.isNotBlank() && !value.contains(",")) {
                // Single user expected.
                return userGroupCache.getUserById(value) ?: "###"
            }
        }
        if (propertyTypeClass == EmployeeDO::class.java || propertyTypeClass == AddressbookDO::class.java) {
            return getDBObjects(value, context).joinToString { dbObject ->
                if (dbObject is EmployeeDO) {
                    dbObject.user?.getFullname() ?: "???"
                } else if (dbObject is AddressbookDO) {
                    dbObject.title ?: "???"
                } else {
                    dbObject.toString() // Shouldn't occur.
                }
            }
        }
        if (propertyTypeClass == AccessEntryDO::class.java) {
            return value
        }
        return getDBObjects(value, context)
    }


    /**
     * @param propertyType The property type in the history table (must be unified before!).
     */
    internal fun getClass(propertyType: String?): Class<*>? {
        propertyType ?: return null
        getValueType(propertyType) // Ensure that the mapping is set.
        synchronized(typeClassMapping) {
            typeClassMapping[propertyType]?.let { return it }
        }
        return null
    }

    /**
     * If return value is [ValueType.BASE_TYPE], the value for displaying should be formatted by [formatBaseType].
     * This type may differ from the property type of the entity (due to migration issues).
     * @param typeString The property type in the history table (must be unified before!).
     */
    internal fun getValueType(typeString: String?): ValueType {
        typeString ?: return ValueType.UNKNOWN
        synchronized(typeMapping) {
            typeMapping[typeString]?.let { return it }
        }
        synchronized(unknownPropertyTypes) {
            if (unknownPropertyTypes.contains(typeString)) {
                return ValueType.UNKNOWN
            }
        }
        if (baseTypes.contains(typeString)) {
            return addMapping(typeString, ValueType.BASE_TYPE)
        }
        if (typeString == "[B") {
            return addMapping(typeString, ValueType.BINARY, ByteArray::class.java)
        }
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName(typeString)
        } catch (_: ClassNotFoundException) {
            log.warn("Class '$typeString' not found.")
        }
        if (clazz == null) {
            return addUnknownPropertyType(typeString)
        }
        if (clazz.isEnum) {
            if (I18nEnum::class.java.isAssignableFrom(clazz)) {
                return addMapping(typeString, ValueType.I18N_ENUM, clazz)
            }
            return addMapping(typeString, ValueType.ENUM, clazz)
        }
        if (HibernateUtils.isEntity(clazz)) {
            return addMapping(typeString, ValueType.ENTITY, clazz)
        }
        return addUnknownPropertyType(typeString)
    }

    private fun addMapping(typeString: String, valueType: ValueType, clazz: Class<*>? = null): ValueType {
        synchronized(typeMapping) {
            typeMapping[typeString] = valueType
        }
        if (clazz != null) {
            synchronized(typeClassMapping) {
                typeClassMapping[typeString] = clazz
            }
        }
        return valueType
    }

    private fun addUnknownPropertyType(typeString: String): ValueType {
        synchronized(unknownPropertyTypes) {
            unknownPropertyTypes.add(typeString)
        }
        return ValueType.UNKNOWN
    }

    /**
     * Formats the value for displaying.
     */
    internal fun formatBaseType(valueString: String?): String {
        if (valueString.isNullOrBlank()) {
            return ""
        }
        try {
            val value = defaultHandler.deserialize(valueString) ?: return ""
            return defaultHandler.format(value)
        } catch (_: Exception) {
            return valueString
        }
    }

    internal fun formatEnum(valueString: String): String {
        return valueString
    }

    internal fun formatI18nEnum(valueString: String, propertyType: String): String {
        val type = getClass(propertyType) ?: return valueString
        val i18nEnum = I18nEnum.create(type, valueString) as? I18nEnum ?: return valueString
        return translate(i18nEnum.i18nKey)
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun formatBinary(valueString: String): String {
        return "[...]"
    }

    internal fun formatEntity(valueString: String, propertyType: String): String {
        val clazz = getClass(propertyType) ?: return valueString
        return "${clazz.simpleName}#${valueString}"
    }

    /**
     * Loads the objects from the database, references as id's in the given value.
     * @param value The old or new value from the history entry attr.
     */
    private fun getDBObjects(value: String?, context: HistoryLoadContext): List<Any> {
        val currentAttr = context.currentHistoryEntryAttr ?: return emptyList()
        val propertyClass = getClass(currentAttr.propertyTypeClass) ?: return emptyList()
        val propertyName = currentAttr.propertyName
        val ret = mutableListOf<Any>()
        val ids = StringUtils.split(value, ", ")
        if (ids.isEmpty()) {
            return emptyList()
        }
        persistenceService.runReadOnly { persistenceContext ->
            val em = persistenceContext.em
            ids.forEach { idString ->
                try {
                    val id = idString.toLong()
                    val ent = em.find(propertyClass, id)
                    if (ent != null) {
                        ret.add(ent)
                    } else {
                        log.warn("Cannot find object of entity $propertyClass with id for property $propertyName (should only occur in test cases): $idString")
                        ret.add("${propertyClass.simpleName}#$id")
                    }
                } catch (_: NumberFormatException) {
                    log.warn("Cannot parse id for property $propertyName: $idString")
                }
            }
        }
        return ret
    }

    internal fun toDisplayNames(value: Any?): String {
        value ?: return ""
        return if (value is Collection<*>) {
            value.map { input -> toDisplayName(input) }.sorted().joinToString()
        } else toDisplayName(value)
    }

    internal fun toDisplayName(obj: Any?): String {
        obj ?: return ""
        return if (obj is DisplayNameCapable) obj.displayName ?: "---" else obj.toString()
    }


    companion object {
        @JvmStatic
        lateinit var instance: HistoryValueService
            private set

        /**
         * Fixes old property types with suffixes such as _$$_jvst148_1e or $HibernateProxy$* by cutting the
         * tail.
         * Examples:
         * - org.projectforge.business.address.AddressDO$HibernateProxy$En8hKwh8 -> org.projectforge.business.address.AddressDO
         * - org.projectforge.business.address.AddressDO_$$_jvst148_2f -> org.projectforge.business.address.AddressDO
         */
        internal fun getUnifiedTypeName(propertyType: String?): String {
            propertyType ?: return ""
            return propertyType.substringBefore("_$$").substringBefore("$")
        }

        private val baseTypes = arrayOf(
            "boolean",
            "int",
            "java.lang.Boolean",
            "java.lang.Integer",
            "java.lang.Short",
            "java.lang.String",
            "java.math.BigDecimal",
            "java.sql.Date",
            "java.sql.Timestamp",
            "java.time.LocalDate",
            "java.util.Date",
            "java.util.Locale",
            "net.fortuna.ical4j.model.Date",
            "net.fortuna.ical4j.model.DateTime",
            "void",
        )
    }
}
// All used entity_name since 2001:
// org.projectforge.business.address.AddressbookDO
// org.projectforge.business.address.AddressDO
// org.projectforge.business.book.BookDO
// org.projectforge.business.fibu.AuftragDO
// org.projectforge.business.fibu.AuftragsPositionDO
// org.projectforge.business.fibu.EingangsrechnungDO
// org.projectforge.business.fibu.EingangsrechnungsPositionDO
// org.projectforge.business.fibu.EmployeeDO
// org.projectforge.business.fibu.EmployeeSalaryDO
// org.projectforge.business.fibu.KontoDO
// org.projectforge.business.fibu.kost.BuchungssatzDO
// org.projectforge.business.fibu.kost.Kost1DO
// org.projectforge.business.fibu.kost.Kost2ArtDO
// org.projectforge.business.fibu.kost.Kost2DO
// org.projectforge.business.fibu.kost.KostZuweisungDO
// org.projectforge.business.fibu.KundeDO
// org.projectforge.business.fibu.PaymentScheduleDO
// org.projectforge.business.fibu.ProjektDO
// org.projectforge.business.fibu.RechnungDO
// org.projectforge.business.fibu.RechnungsPositionDO
// org.projectforge.business.humanresources.HRPlanningDO
// org.projectforge.business.humanresources.HRPlanningEntryDO
// org.projectforge.business.orga.ContractDO
// org.projectforge.business.orga.PostausgangDO
// org.projectforge.business.orga.PosteingangDO
// org.projectforge.business.orga.VisitorbookDO
// org.projectforge.business.poll.PollDO
// org.projectforge.business.poll.PollResponseDO
// org.projectforge.business.scripting.ScriptDO
// org.projectforge.business.task.TaskDO
// org.projectforge.business.teamcal.admin.model.TeamCalDO
// org.projectforge.business.teamcal.event.model.CalEventDO
// org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO
// org.projectforge.business.teamcal.event.model.TeamEventDO
// org.projectforge.business.timesheet.TimesheetDO
// org.projectforge.business.vacation.model.LeaveAccountEntryDO
// org.projectforge.business.vacation.model.RemainingLeaveDO
// org.projectforge.business.vacation.model.VacationCalendarDO
// org.projectforge.business.vacation.model.VacationDO
// org.projectforge.framework.access.GroupTaskAccessDO
// org.projectforge.framework.configuration.entities.ConfigurationDO
// org.projectforge.framework.persistence.user.entities.GroupDO
// org.projectforge.framework.persistence.user.entities.PFUserDO
// org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
// org.projectforge.framework.persistence.user.entities.UserPasswordDO
// org.projectforge.framework.persistence.user.entities.UserRightDO
// org.projectforge.plugins.banking.BankAccountDO
// org.projectforge.plugins.banking.BankAccountRecordDO
// org.projectforge.plugins.eed.model.EmployeeConfigurationDO
// org.projectforge.plugins.ffp.model.FFPAccountingDO
// org.projectforge.plugins.ffp.model.FFPDebtDO
// org.projectforge.plugins.ffp.model.FFPEventDO
// org.projectforge.plugins.licensemanagement.LicenseDO
// org.projectforge.plugins.liquidityplanning.LiquidityEntryDO
// org.projectforge.plugins.marketing.AddressCampaignDO
// org.projectforge.plugins.marketing.AddressCampaignValueDO
// org.projectforge.plugins.skillmatrix.SkillDO
// org.projectforge.plugins.skillmatrix.SkillEntryDO
// org.projectforge.plugins.skillmatrix.SkillRatingDO
// org.projectforge.plugins.skillmatrix.TrainingAttendeeDO
// org.projectforge.plugins.skillmatrix.TrainingDO
// org.projectforge.plugins.to do.To DoDO

// All used property_type_class since 2001:
//  [B
// boolean
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
// void
