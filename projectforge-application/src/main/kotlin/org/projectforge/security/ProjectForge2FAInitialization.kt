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
import org.projectforge.rest.*
import org.projectforge.rest.admin.AdminLogViewerPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.fibu.*
import org.projectforge.rest.fibu.kost.Kost1PagesRest
import org.projectforge.rest.fibu.kost.Kost2PagesRest
import org.projectforge.rest.orga.AccountingRecordPagesRest
import org.projectforge.rest.orga.PostausgangPagesRest
import org.projectforge.rest.orga.PosteingangPagesRest
import org.projectforge.rest.orga.VisitorbookPagesRest
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
open class ProjectForge2FAInitialization {
  @Autowired
  internal lateinit var my2FARequestHandler: My2FARequestHandler

  @PostConstruct
  internal fun init() {
    registerShortCutValues(
      "ADMIN-WRITE",
      "WRITE:user;WRITE:group;/wa/userEdit;/wa/groupEdit;/wa/admin",
      "/wa/licenseEdit;/wa/accessEdit",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin",
      "/wa/configurationEdit"
    )
    registerShortCutValues(
      "ADMIN",
      "/wa/user;/wa/group;/wa/admin",
      "/wa/license;/wa/access",
      // LuceneConsole, GroovyConsole, SQLConsole:
      "/wa/wicket/bookmarkable/org.projectforge.web.admin",
      "/wa/configuration"
    )
    registerShortCutClasses(
      "ADMIN",
      UserPagesRest::class.java,
      GroupPagesRest::class.java,
      AdminLogViewerPageRest::class.java,
      GroupAccessPagesRest::class.java,
    )

    registerShortCutValues(
      "HR-WRITE",
      "WRITE:employee;/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed;/wa/hrPlanningEdit"
    )
    registerShortCutValues(
      "HR",
      "WRITE:employee;/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed;/wa/hr"
    )

    registerShortCutValues(
      "FINANCE-WRITE",
      "/wa/reportEdit;/wa/accountingEdit;/wa/datev;/wa/liquidityplanningEdit;/wa/incomingInvoiceEdit;/wa/outgoingInvoiceEdit;/wa/cost.*Edit;/wa/customerEdit;/wa/accountEdit;",
      "/wa/projectEdit;/wa/orderBookEdit"
    )

    registerShortCutValues(
      "FINANCE",
      "/wa/report;/wa/accounting;/wa/datev;/wa/liquidity;/wa/incomingInvoice;/wa/outgoingInvoice;/wa/cost;/wa/customer;/wa/account;",
      "/wa/project;/wa/orderBook"
    )
    registerShortCutClasses(
      "FINANCE",
      AccountingRecordPagesRest::class.java,
      Kost1PagesRest::class.java,
      Kost2PagesRest::class.java,
      KontoPagesRest::class.java,
      EingangsrechnungPagesRest::class.java,
      RechnungPagesRest::class.java,
      CustomerPagesRest::class.java,
      ProjectPagesRest::class.java,
      AuftragPagesRest::class.java,
    )

    my2FARequestHandler.registerShortCutValues(
      "ORGA-WRITE",
      "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;WRITE:visitorBook;/wa/.*VisitorbookEdit"
    )
    my2FARequestHandler.registerShortCutValues(
      "ORGA",
      "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;/wa/.*Visitorbook"
    )
    registerShortCutClasses("ORGA",
      PostausgangPagesRest::class.java,
      PosteingangPagesRest::class.java,
      VisitorbookPagesRest::class.java,
    )

    my2FARequestHandler.registerShortCutValues(
      "SCRIPT-WRITE", "WRITE:script"
    )
    registerShortCutClasses(
      "SCRIPT",
      ScriptPagesRest::class.java,
      MyScriptPagesRest::class.java,
      MyScriptExecutePageRest::class.java,
      ScriptExecutePageRest::class.java,
    )
    registerShortCutClasses(
      "MY_ACCOUNT",
      MyAccountPageRest::class.java,
      // My2FASetupPageRest::class.java, // Check done by this page itself.
      TokenInfoPageRest::class.java,
    )
    registerShortCutMethods("MY_ACCOUNT", UserServicesRest::renewToken)
    registerShortCutClasses("PASSWORD", ChangePasswordPageRest::class.java, ChangeWlanPasswordPageRest::class.java)
    log.info(my2FARequestHandler.printConfiguration())
  }

  fun registerShortCutValues(shortCut: String, vararg values: String)
      : ProjectForge2FAInitialization {
    my2FARequestHandler.registerShortCutValues(shortCut, *values)
    return this
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun registerShortCutClasses(
    shortCut: String,
    vararg restClasses: Class<*>,
  ) {
    restClasses.forEach { restClass ->
      my2FARequestHandler.registerShortCutValues(shortCut, RestResolver.getRestUrl(restClass))
    }
  }

  /**
   * @param restClass needed, otherwise for derived classes such as AdminLogViewerPagesRest the declaring class is LogViewerPagesRest.
   */
  fun registerShortCutMethods(
    shortCut: String,
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
  fun registerShortCutMethods(
    shortCut: String,
    vararg methods: KFunction<*>
  ) {
    require(methods.isNotEmpty()) { "registerShortCutMethods(String, Class<*>, methods) called with empty methods. Use registerShortCutClasses instead." }
    methods.forEach { method ->
      my2FARequestHandler.registerShortCutValues(shortCut, RestResolver.getRestMethodUrl(method))
    }
  }
}
