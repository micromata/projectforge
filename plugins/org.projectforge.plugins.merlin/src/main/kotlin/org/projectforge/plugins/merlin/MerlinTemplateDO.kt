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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.micromata.genome.db.jpa.history.api.NoHistory
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_plugin_merlin_template")
open class MerlinTemplateDO : AbstractBaseDO<Int>(), AttachmentsInfo {
  @PropertyInfo(i18nKey = "id")
  private var id: Int? = null

  @PropertyInfo(i18nKey = "plugins.merlin.name")
  @get:Column(length = 100, nullable = false)
  open var name: String? = null

  @PropertyInfo(i18nKey = "description")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.fileNamePattern", tooltip = "plugins.merlin.fileNamePattern.info")
  @get:Column(name = "filename_pattern", length = 1000, nullable = false)
  open var fileNamePattern: String? = null

  @PropertyInfo(i18nKey = "plugins.merlin.forceStrictFilenames", tooltip = "plugins.merlin.forceStrictFilenames.info")
  @get:Column(name = "strongly_restricted_filenames")
  open var stronglyRestrictedFilenames: Boolean? = null

  @PropertyInfo(i18nKey = "plugins.merlin.format.pdf", tooltip = "plugins.merlin.format.pdf.info")
  @get:Column(name = "pdf_export")
  open var pdfExport: Boolean? = null

  /**
   * These users have full read/write/execute access.
   */
  @get:Column(name = "admin_ids", length = 4000, nullable = true)
  open var adminIds: String? = null

  /**
   * Members of these groups have read and execute access.
   */
  @get:Column(name = "access_group_ids", length = 4000, nullable = true)
  open var accessGroupIds: String? = null

  /**
   * These users have read and execute access.
   */
  @get:Column(name = "access_user_ids", length = 4000, nullable = true)
  open var accessUserIds: String? = null

  /**
   * Master variables of TemplateDefinition ([de.micromata.merlin.word.templating.TemplateDefinition.variableDefinitions])
   * serialized as json array.
   */
  //@get:Column(name = "variables", length = 100000, nullable = true)
  //open var variables: String? = null

  /**
   * Dependant variables of TemplateDefinition ([de.micromata.merlin.word.templating.TemplateDefinition.dependentVariableDefinitions])
   * serialized as json array.
   */
  //@get:Column(name = "dependent_variables", length = 100000, nullable = true)
  //open var dependentVariables: String? = null

  /**
   * Names of attachments for displaying purposes only.
   */
  @JsonIgnore
  @Field
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_names")
  override var attachmentsNames: String? = null

  @JsonIgnore
  @Field
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_ids")
  override var attachmentsIds: String? = null

  @JsonIgnore
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_counter")
  override var attachmentsCounter: Int? = null

  @JsonIgnore
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_size")
  override var attachmentsSize: Long? = null

  @PropertyInfo(i18nKey = "attachment")
  @JsonIgnore
  @get:Column(length = 10000, name = "attachments_last_user_action")
  override var attachmentsLastUserAction: String? = null

  @Id
  @GeneratedValue
  @Column(name = "pk")
  override fun getId(): Int? {
    return id
  }

  override fun setId(id: Int?) {
    this.id = id
  }
}
