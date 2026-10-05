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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.plugins.core.IProjectForge2FAInitialization
import org.projectforge.rest.*
import org.projectforge.rest.admin.AdminErrorsRest
import org.projectforge.rest.admin.AdminLogViewerRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.fibu.*
import org.projectforge.rest.fibu.importer.DatevAccountImportRest
import org.projectforge.rest.fibu.importer.DatevRecordImportRest
import org.projectforge.rest.fibu.kost.Kost1EntityRest
import org.projectforge.rest.fibu.kost.Kost2EntityRest
import org.projectforge.rest.hr.HRPlanningEntityRest
import org.projectforge.rest.hr.HRPlanningEntryEntityRest
import org.projectforge.rest.hr.HRViewRest
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
    // The pages of projectforge-next are static files served by a resource handler (see WebApplicationConfig), so
    // no filter sees their urls and a client side navigation doesn't even reach the server: only their rest calls
    // are left to gate. Hence WRITE:<category> for the saves and the *Rest classes for the reading shortcuts
    // (NextMigration2FATest keeps the gates the former Wicket pages /wa/... had).
    registerShortCutValues(
      My2FAShortCut.ADMIN_WRITE,
      "WRITE:user;WRITE:group;WRITE:access;WRITE:configuration;",
    )
    // The System page (SystemRest, /rs/system), successor of Wicket's admin page, and the Plugins page
    // (PluginAdminRest, /rs/pluginList), successor of Wicket's PluginListPage: both legacy pages were gated by
    // ADMIN_WRITE as a whole, so the whole rest path keeps the write period.
    registerShortCutClasses(My2FAShortCut.ADMIN_WRITE, SystemRest::class.java, PluginAdminRest::class.java)
    registerShortCutClasses(
      My2FAShortCut.ADMIN,
      UserPagesRest::class.java,
      GroupEntityRest::class.java,
      AdminLogViewerRest::class.java,
      AdminErrorsRest::class.java,
      GroupAccessEntityRest::class.java,
      PluginAdminRest::class.java,
      ConfigurationEntityRest::class.java,
      SystemRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.HR_WRITE,
      "WRITE:employee;WRITE:leaveAccountEntry;WRITE:hrPlanning;"
    )
    registerShortCutValues(
      My2FAShortCut.HR,
      "WRITE:employee"
    )
    registerShortCutClasses(
      My2FAShortCut.HR,
      // The HR view, successor of Wicket's /wa/hrList:
      HRViewRest::class.java,
      LeaveAccountEntryPagesRest::class.java,
      HRPlanningEntityRest::class.java,
      HRPlanningEntryEntityRest::class.java,
      EmployeePagesRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.FINANCE_WRITE,
      "WRITE:incomingInvoice;WRITE:outgoingInvoice;WRITE:project;",
      // The entity of WRITE: is the rest category (/rs/order), which for the order is not the identifier of its
      // dao ("auftrag").
      "WRITE:order;WRITE:account;WRITE:cost1;WRITE:cost2;WRITE:cost2Type;WRITE:customer;",
    )
    // The DATEV import (successor of Wicket's /wa/datevImport) writes as a whole, so its whole rest paths keep the
    // write period, as Wicket's page did. Of the report objectives (successor of /wa/reportObjectives) only the
    // import was a write (Wicket's /wa/reportEdit); evaluating them is reading, gated by FINANCE below.
    registerShortCutClasses(
      My2FAShortCut.FINANCE_WRITE,
      DatevRecordImportRest::class.java,
      DatevAccountImportRest::class.java,
    )
    registerShortCutMethods(
      My2FAShortCut.FINANCE_WRITE,
      ReportObjectivesPageRest::upload,
      ReportObjectivesPageRest::paste,
    )

    registerShortCutValues(
      My2FAShortCut.FINANCE,
      "WRITE:employeeSalary",
    )
    registerShortCutClasses(
      My2FAShortCut.FINANCE,
      DatevRecordImportRest::class.java,
      DatevAccountImportRest::class.java,
      ReportObjectivesPageRest::class.java,
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
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerRest the declaring class is LogViewerRest.
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
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerRest the declaring class is LogViewerRest.
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
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerRest the declaring class is LogViewerRest.
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
