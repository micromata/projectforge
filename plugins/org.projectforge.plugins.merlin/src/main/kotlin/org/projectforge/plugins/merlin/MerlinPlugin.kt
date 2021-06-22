/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Const
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.merlin.rest.MerlinAttachmentsActionListener
import org.projectforge.rest.AttachmentsServicesRest
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

  @Autowired
  private lateinit var merlinRunner: MerlinRunner

  override fun initialize() {
    // Register it:
    register(MerlinTemplateDao::class.java, merlinTemplateDao, "plugins.merlin")

    menuCreator.register(
      MenuItemDefId.MISC,
      MenuItemDef(info.id, "plugins.merlin.menu", "${Const.REACT_APP_PATH}merlin")
    )

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME)

    attachmentsServicesRest.register(
      ID,
      MerlinAttachmentsActionListener(attachmentsService, merlinTemplateDao, merlinRunner)
    )
  }

  companion object {
    const val ID = PluginAdminService.PLUGIN_MERLIN_ID
    const val RESOURCE_BUNDLE_NAME = "MerlinI18nResources"
  }
}