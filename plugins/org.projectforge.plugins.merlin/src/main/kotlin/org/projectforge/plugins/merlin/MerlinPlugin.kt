/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import org.projectforge.Constants
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.merlin.rest.MerlinAttachmentsActionListener
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.core.PagesResolver
import org.springframework.beans.factory.annotation.Autowired

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MerlinPlugin :
  AbstractPlugin(
    ID,
    "Merlin-Word®-Templates",
    "Plugin for Microsoft Word® templating (with variables, dependant variables as well as serial execution). Useful for contracts, serial documents etc."
  ) {
  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  @Autowired
  private lateinit var attachmentsServicesRest: AttachmentsServicesRest

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var menuCreator: MenuCreator

  override fun initialize() {
    // Register it:
    register(MerlinTemplateDao::class.java, merlinTemplateDao, "plugins.merlin")

    // Will only delivered to client but has to be ignored on sending back from client:
    JacksonConfiguration.registerAllowedUnknownProperties(
      MerlinVariable::class.java,
      "allowedValuesFormatted",
      "mappingMasterValues",
    )

    menuCreator.register(
      MenuItemDefId.MISC,
      MenuItemDef(info.id, "plugins.merlin.menu", "${Constants.REACT_APP_PATH}merlin")
    )

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME)

    attachmentsServicesRest.register(
      ID,
      MerlinAttachmentsActionListener(attachmentsService, merlinTemplateDao)
    )
  }

  companion object {
    const val ID = PluginAdminService.PLUGIN_MERLIN_ID
    const val RESOURCE_BUNDLE_NAME = "MerlinI18nResources"

    fun ensureUserLogSubscription(): LogSubscription {
      val username = ThreadLocalUserContext.user!!.username ?: throw InternalError("User not given")
      return LogSubscription.ensureSubscription(
        title = "Merlin",
        user = username,
        create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher("de.micromata.merlin", "org.projectforge.plugins.merlin"),
          maxSize = 10000,
        )
      })
    }

    /**
     * Calls also [ensureUserLogSubscription].
     */
    fun createUserLogSubscriptionMenuItem(): MenuItem {
      return MenuItem(
        "logViewer",
        i18nKey = "plugins.merlin.viewLogs",
        url = PagesResolver.getDynamicPageUrl(LogViewerPageRest::class.java, id = ensureUserLogSubscription().id),
        type = MenuItemTargetType.REDIRECT,
      )
    }
  }
}
