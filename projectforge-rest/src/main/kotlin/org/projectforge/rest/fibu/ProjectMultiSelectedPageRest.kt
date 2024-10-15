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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/project${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class ProjectMultiSelectedPageRest : AbstractMultiSelectedPage<ProjektDO>() {

  @Autowired
  private lateinit var projektDao: ProjektDao

  @Autowired
  private lateinit var projectPagesRest: ProjectPagesRest

  override val layoutContext: LayoutContext = LayoutContext(ProjektDO::class.java)

  override fun getTitleKey(): String {
    return "fibu.projekt.multiselected.title"
  }

  @PostConstruct
  private fun postConstruct() {
    pagesRest = projectPagesRest
  }

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    createAndAddFields(
      layoutContext,
      massUpdateData,
      layout,
      "headOfBusinessManager",
      "projectManager",
      "salesManager",
      "description",
      minLengthOfTextArea = 1001, // reference has length 1.000 and description 4.000
    )
  }

  override fun checkParamHasAction(
    params: Map<String, MassUpdateParameter>,
    param: MassUpdateParameter,
    field: String,
    validationErrors: MutableList<ValidationError>
  ): Boolean {
    if (field == "headOfBusinessManager" || field == "projectManager" || field == "salesManager") {
      return param.id != null
    }
    return super.checkParamHasAction(params, param, field, validationErrors)
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<ProjektDO>,
  ): ResponseEntity<*>? {
    val projects = projektDao.select(selectedIds)
    if (projects.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateData
    projects.forEach { project ->
      massUpdateContext.startUpdate(project)
      TextFieldModification.processTextParameter(project, "description", params)
      proceedMassUpdateUserField(params, ProjektDO::headOfBusinessManager, project)
      proceedMassUpdateUserField(params, ProjektDO::projectManager, project)
      proceedMassUpdateUserField(params, ProjektDO::salesManager, project)
      massUpdateContext.commitUpdate(
        identifier4Message = project.displayName,
        project,
        update = { projektDao.update(project) },
      )
    }
    return null
  }

  override fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
    val displayTitle = translate("fibu.projekt.multiselected.title")
    return LogSubscription.ensureSubscription(
      title = "Projects",
      displayTitle = displayTitle,
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher(
            "de.micromata.fibu.ProjektDao",
            "org.projectforge.framework.persistence.api.BaseDaoSupport|ProjektDO"
          ),
          maxSize = 10000,
          displayTitle = displayTitle
        )
      })
  }
}
