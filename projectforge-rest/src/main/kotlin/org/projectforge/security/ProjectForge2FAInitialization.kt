/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.rest.fibu.kost.Kost1PagesRest
import org.projectforge.rest.fibu.kost.Kost2PagesRest
import org.projectforge.rest.hr.HRPlanningListPagesRest
import org.projectforge.rest.hr.HRPlanningPagesRest
import org.projectforge.rest.hr.LeaveAccountEntryPagesRest
import org.projectforge.rest.orga.*
import org.projectforge.rest.scripting.MyScriptExecutePageRest
import org.projectforge.rest.scripting.MyScriptPagesRest
import org.projectforge.rest.scripting.ScriptExecutePageRest
import org.projectforge.rest.scripting.ScriptPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct
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
      "WRITE:user;WRITE:group;",
      "/wa/userEdit;/wa/groupEdit;/wa/admin",
      "/wa/accessEdit",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin",
      "/wa/configurationEdit"
    )
    registerShortCutValues(
      My2FAShortCut.ADMIN,
      "/wa/user;/wa/group;/wa/admin",
      "/wa/access",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin",
      "/wa/configuration"
    )
    registerShortCutClasses(
      My2FAShortCut.ADMIN,
      UserPagesRest::class.java,
      GroupPagesRest::class.java,
      AdminLogViewerPageRest::class.java,
      GroupAccessPagesRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.HR_WRITE,
      "WRITE:employee;WRITE:leaveAccountEntry;WRITE:employee;",
      "/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed;/wa/hrPlanningEdit"
    )
    registerShortCutValues(
      My2FAShortCut.HR,
      "WRITE:employee;/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed;/wa/hr"
    )
    registerShortCutClasses(
      My2FAShortCut.HR,
      LeaveAccountEntryPagesRest::class.java,
      HRPlanningPagesRest::class.java,
      HRPlanningListPagesRest::class.java,
      EmployeePagesRest::class.java,
    )

    registerShortCutValues(
      My2FAShortCut.FINANCE_WRITE,
      "WRITE:incomingInvoice;WRITE:outgoingInvoice;",
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
      AccountingRecordPagesRest::class.java,
      Kost1PagesRest::class.java,
      Kost2PagesRest::class.java,
      KontoPagesRest::class.java,
      EingangsrechnungPagesRest::class.java,
      RechnungPagesRest::class.java,
      CustomerPagesRest::class.java,
      ProjectPagesRest::class.java,
      AuftragPagesRest::class.java,
      EingangsrechnungMultiSelectedPageRest::class.java,
      RechnungMultiSelectedPageRest::class.java,
    )

    my2FARequestHandler.registerShortCutValues(
      My2FAShortCut.ORGA_WRITE,
      "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;WRITE:visitorBook;/wa/.*VisitorbookEdit"
    )
    my2FARequestHandler.registerShortCutValues(
      My2FAShortCut.ORGA,
      "/wa/.*Visitorbook"
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
    registerShortCutMethods(My2FAShortCut.MY_ACCOUNT, UserServicesRest::renewToken)
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
