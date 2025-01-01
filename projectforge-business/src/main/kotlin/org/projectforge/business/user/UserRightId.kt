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

package org.projectforge.business.user

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.api.RightRightIdProviderService
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Indexed //@ClassBridge(index = Index.YES /* TOKENIZED */, store = Store.NO, impl = HibernateSearchUserRightIdBridge.class)
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchUserRightIdTypeBinder::class))
enum class UserRightId
/**
 * @param id          Must be unique (including all plugins).
 * @param orderString For displaying the rights in e. g. UserEditPage in the correct order.
 * @param i18nKey
 */(override val id: String, override val orderString: String, override val i18nKey: String) : IUserRightId {
    //For test fix. Has to be removed.
    TEST("", "", ""),

    ADMIN_CORE(
        "ADMIN_CORE", "admin",
        "access.right.admin.core"
    ),

    FIBU_EINGANGSRECHNUNGEN(
        "FIBU_EINGANGSRECHNUNGEN", "fibu1",
        "access.right.fibu.eingangsrechnungen"
    ),

    FIBU_AUSGANGSRECHNUNGEN(
        "FIBU_AUSGANGSRECHNUNGEN", "fibu2",
        "access.right.fibu.ausgangsrechnungen"
    ),

    FIBU_DATEV_IMPORT(
        "FIBU_DATEV_IMPORT", "fibu5",
        "access.right.fibu.datevImport"
    ),

    FIBU_COST_UNIT(
        "FIBU_COST_UNIT", "fibu6",
        "access.right.fibu.costUnit"
    ),

    FIBU_ACCOUNTS(
        "FIBU_ACCOUNTS", "fibu7",
        "access.right.fibu.accounts"
    ),

    MISC_ADDRESSBOOK("MISC_ADDRESSBOOK", "misc2", "access.right.misc.addressbook"),

    PM_GANTT("PM_GANTT", "pm1", "access.right.pm.gantt"),

    PM_ORDER_BOOK("PM_ORDER_BOOK", "pm2", "access.right.pm.orderbook"),

    PM_HR_PLANNING(
        "PM_HR_PLANNING", "pm3",
        "access.right.pm.hrPlanning"
    ),

    PM_PROJECT("PM_PROJECT", "pm4", "access.right.pm.project"),

    ORGA_CONTRACTS(
        "ORGA_CONTRACTS", "orga1",
        "access.right.orga.contracts"
    ),

    ORGA_INCOMING_MAIL(
        "ORGA_INCOMING_MAIL", "orga2",
        "access.right.orga.incomingmail"
    ),

    ORGA_OUTGOING_MAIL(
        "ORGA_OUTGOING_MAIL", "orga3",
        "access.right.orga.outgoingmail"
    ),

    ORGA_VISITORBOOK(
        "ORGA_VISITORBOOK", "orga4",
        "access.right.orga.visitorbook"
    ),

    HR_EMPLOYEE(
        "HR_EMPLOYEE", "hr1",
        "access.right.hr.employee"
    ),

    HR_EMPLOYEE_SALARY(
        "HR_EMPLOYEE_SALARY", "hr2",
        "access.right.hr.employeeSalaries"
    ),

    HR_VACATION(
        "HR_VACATION", "hr3",
        "access.right.hr.vacation"
    ),

    PLUGIN_CALENDAR(
        "PLUGIN_CALENDAR", "plugin15",
        "plugins.teamcal.calendar"
    ),

    PLUGIN_CALENDAR_EVENT(
        "PLUGIN_CALENDAR_EVENT", "plugin15",
        "plugins.teamcalendar.event"
    ),

    CALENDAR_EVENT(
        "CALENDAR_EVENT", "plugin15",
        "plugins.teamcalendar.event"
    );

    class ProviderService : RightRightIdProviderService {
        override fun getUserRightIds(): Collection<IUserRightId> {
            return Arrays.asList<IUserRightId>(*entries.toTypedArray())
        }
    }

    override fun toString(): String {
        return id.toString()
    }

    override fun compareTo(o: IUserRightId?): Int {
        return this.compareTo(o)
    }
}
