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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.plugins.core.IProjectForge2FAInitialization
import org.projectforge.rest.*
import org.projectforge.rest.admin.AdminLogViewerPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.fibu.*
import org.projectforge.rest.fibu.kost.Kost1EntityRest
import org.projectforge.rest.fibu.kost.Kost2EntityRest
import org.projectforge.rest.hr.HRPlanningEntityRest
import org.projectforge.rest.hr.HRPlanningEntryEntityRest
import org.projectforge.rest.hr.LeaveAccountEntryPagesRest
import org.projectforge.rest.orga.*
import org.projectforge.rest.scripting.MyScriptExecutePageRest
import org.projectforge.rest.scripting.MyScriptPagesRest
import org.projectforge.rest.scripting.ScriptExecutePageRest
import org.projectforge.rest.scripting.ScriptPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import kotlin.reflect.KFunction

private val log = KotlinLogging.logger {}

/**
 * Definition of 2FA shortcuts
 */
@Configuration
open class ProjectForge2FAInitialization : IProjectForge2FAInitialization {
  @Autowired
  internal lateinit var my2FARequestHandler: My2FARequestHandler

  @PostConstruct
  internal fun init() {
    registerShortCutValues(
      My2FAShortCut.ADMIN_WRITE,
      // WRITE:access gates the save of the migrated access-rights form (GroupAccessEntityRest, /rs/access): its
      // page is a static file of projectforge-next served by a resource handler (see WebApplicationConfig), so no
      // filter sees its url - only the rest call is left to gate. Without it, an installation configuring
      // ADMIN_WRITE but not ADMIN would ask for a second factor before Wicket's /wa/accessEdit but no longer
      // before the migrated one. WRITE:configuration is the same case for the migrated system-configuration
      // form (ConfigurationEntityRest, /rs/configuration): its Wicket form was removed, so the /wa gate is
      // gone and the rest call is all there is left to gate.
      "WRITE:user;WRITE:group;WRITE:access;WRITE:configuration;",
      "/wa/userEdit;/wa/groupEdit;/wa/admin",
      "/wa/accessEdit",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin"
    )
    // The migrated System page (SystemRest, /rs/system), successor of Wicket's /wa/admin, and the migrated Plugins
    // page (PluginAdminRest, /rs/pluginList), successor of Wicket's org.projectforge.web.admin.PluginListPage: both
    // legacy pages are gated by ADMIN_WRITE as a whole (see above). Their next pages are static files served by a
    // resource handler, so only their rest calls are left to gate, and the whole path keeps the write period.
    registerShortCutClasses(My2FAShortCut.ADMIN_WRITE, SystemRest::class.java, PluginAdminRest::class.java)
    registerShortCutValues(
      My2FAShortCut.ADMIN,
      "/wa/user;/wa/group;/wa/admin",
      "/wa/access",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin"
    )
    registerShortCutClasses(
      My2FAShortCut.ADMIN,
      UserPagesRest::class.java,
      GroupPagesRest::class.java,
      AdminLogViewerPageRest::class.java,
      GroupAccessEntityRest::class.java,
      // The migrated Plugins admin page (PluginAdminRest, /rs/pluginList): its next page is a static file served by a
      // resource handler, so no filter sees its url - only the REST call is left to gate. The classic Wicket page sat
      // behind the org.projectforge.web.admin admin 2FA prefix, so its successor keeps the admin second factor here.
      PluginAdminRest::class.java,
      ConfigurationEntityRest::class.java,
      // The migrated System page (/wa/admin, see ADMIN_WRITE above):
      SystemRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.HR_WRITE,
      // WRITE:hrPlanning gates the save of the migrated weekly HR planning (HRPlanningEntityRest, /rs/hrPlanning),
      // which replaced the Wicket /wa/hrPlanningEdit gated here before.
      "WRITE:employee;WRITE:leaveAccountEntry;WRITE:employee;WRITE:hrPlanning;"
    )
    registerShortCutValues(
      My2FAShortCut.HR,
      "WRITE:employee;/wa/hr"
    )
    registerShortCutClasses(
      My2FAShortCut.HR,
      LeaveAccountEntryPagesRest::class.java,
      HRPlanningEntityRest::class.java,
      HRPlanningEntryEntityRest::class.java,
      EmployeePagesRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.FINANCE_WRITE,
      "WRITE:incomingInvoice;WRITE:outgoingInvoice;WRITE:project;",
      // The REST counterparts of /wa/orderBookEdit and /wa/cost.*Edit below: the forms of the order and of the
      // cost1/cost2 are pages of projectforge-next now, and a page of that app is a static file served by a resource
      // handler (see WebApplicationConfig), so no filter ever sees its url - only the rest calls of the form are
      // left to gate. Without these, an installation configuring FINANCE_WRITE but not FINANCE would ask for
      // a second factor before Wicket's form, but no longer before the migrated one.
      // The entity of WRITE: is the rest category (/rs/order), which for the order is not the identifier of its
      // dao ("auftrag"): the write access of Wicket's own form is gated by the url below, not by this entry.
      "WRITE:order;WRITE:account;WRITE:cost1;WRITE:cost2;WRITE:cost2Type;WRITE:customer;",
      "/wa/reportEdit;/wa/accountingEdit;/wa/datev;/wa/incomingInvoiceEdit;/wa/outgoingInvoiceEdit;/wa/cost.*Edit;/wa/customerEdit;/wa/accountEdit;",
      "/wa/projectEdit;/wa/orderBookEdit"
    )

    registerShortCutValues(
      My2FAShortCut.FINANCE,
      "WRITE:employeeSalary",
      "/wa/report;/wa/accounting;/wa/datev;/wa/incomingInvoice;/wa/outgoingInvoice;/wa/cost;/wa/customer;/wa/account;",
      "/wa/project;/wa/orderBook"
    )
    registerShortCutClasses(
      My2FAShortCut.FINANCE,
      AccountingRecordEntityRest::class.java,
      EmployeeSalaryEntityRest::class.java,
      Kost1EntityRest::class.java,
      Kost2EntityRest::class.java,
      KontoEntityRest::class.java,
      IncomingInvoiceEntityRest::class.java,
      OutgoingInvoiceEntityRest::class.java,
      CustomerPagesRest::class.java,
      ProjectEntityRest::class.java, ProjectMultiSelectedPageRest::class.java,
      OrderEntityRest::class.java,
      EingangsrechnungMultiSelectedPageRest::class.java,
      RechnungMultiSelectedPageRest::class.java,
    )

    my2FARequestHandler.registerShortCutValues(
      My2FAShortCut.ORGA_WRITE,
      "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;WRITE:visitorbook"
    )
    registerShortCutClasses(
      My2FAShortCut.ORGA,
      PostausgangPagesRest::class.java,
      PosteingangPagesRest::class.java,
      VisitorbookPagesRest::class.java,
      ContractPagesRest::class.java,
    )

    my2FARequestHandler.registerShortCutValues(
      My2FAShortCut.SCRIPT_WRITE, "WRITE:script"
    )
    registerShortCutClasses(
      My2FAShortCut.SCRIPT,
      ScriptPagesRest::class.java,
      MyScriptPagesRest::class.java,
      MyScriptExecutePageRest::class.java,
      ScriptExecutePageRest::class.java,
    )
    registerShortCutClasses(
      My2FAShortCut.MY_ACCOUNT,
      MyAccountPageRest::class.java,
      // My2FASetupPageRest::class.java, // Check done by this page itself.
      TokenInfoPageRest::class.java,
    )
    // UserServicesRest shares its @RequestMapping with UserPagesRest (/rs/user), so these methods are already
    // covered by the ADMIN registration above - by accident and with the wrong period. They are self service
    // calls, so they belong to MY_ACCOUNT: an installation configuring MY_ACCOUNT but not ADMIN would leave
    // them ungated otherwise. Both registrations may match, the shorter period wins (My2FARequestHandler
    // iterates the periods from minutes1 to days90).
    registerShortCutMethods(
      My2FAShortCut.MY_ACCOUNT,
      UserServicesRest::renewToken,
      UserServicesRest::logoutAllDevices,
    )
    // Logging other users out of all their devices is an admin operation on foreign accounts:
    registerShortCutMethods(My2FAShortCut.ADMIN_WRITE, UserServicesRest::logoutAllDevicesOfUser)
    registerShortCutClasses(
      My2FAShortCut.PASSWORD,
      ChangePasswordPageRest::class.java,
      ChangeWlanPasswordPageRest::class.java
    )
  }

  override fun registerShortCutValues(shortCut: My2FAShortCut, vararg values: String)
      : IProjectForge2FAInitialization {
    my2FARequestHandler.registerShortCutValues(shortCut, *values)
    return this
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  override fun registerShortCutClasses(
    shortCut: My2FAShortCut,
    vararg restClasses: Class<*>,
  ) {
    restClasses.forEach { restClass ->
      my2FARequestHandler.registerShortCutValues(shortCut, RestResolver.getRestUrl(restClass))
    }
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  override fun registerShortCutMethods(
    shortCut: My2FAShortCut,
    restClass: Class<*>,
    vararg methods: KFunction<*>
  ) {
    require(methods.isNotEmpty()) { "registerShortCutMethods(String, Class<*>, methods) called with empty methods. Use registerShortCutClasses instead." }
    methods.forEach { method ->
      my2FARequestHandler.registerShortCutValues(shortCut, RestResolver.getRestMethodUrl(restClass, method))
    }
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  override fun registerShortCutMethods(
    shortCut: My2FAShortCut,
    vararg methods: KFunction<*>
  ) {
    require(methods.isNotEmpty()) { "registerShortCutMethods(String, Class<*>, methods) called with empty methods. Use registerShortCutClasses instead." }
    methods.forEach { method ->
      my2FARequestHandler.registerShortCutValues(shortCut, RestResolver.getRestMethodUrl(method))
    }
  }
}
