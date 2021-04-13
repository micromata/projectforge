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

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_plugin_merlin_template")
open class MerlinTemplateDO : DefaultBaseDO() {

  @PropertyInfo(i18nKey = "plugins.merlin.name")
  @get:Column(length = 100)
  open var name: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.description")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.fileNamePattern", tooltip = "plugins.merlin.fileNamePattern.info")
  @get:Column(name = "filename_pattern", length = 1000)
  open var fileNamePattern: String? = null

  /**
   * These users have full read/write access.
   */
  @get:Column(name = "admin_ids", length = 4000, nullable = true)
  open var adminIds: String? = null

  /**
   * Members of these groups have upload/download access.
   */
  @get:Column(name = "access_group_ids", length = 4000, nullable = true)
  open var accessGroupIds: String? = null

  /**
   * These users have upload/download access.
   */
  @get:Column(name = "access_user_ids", length = 4000, nullable = true)
  open var accessUserIds: String? = null

}
