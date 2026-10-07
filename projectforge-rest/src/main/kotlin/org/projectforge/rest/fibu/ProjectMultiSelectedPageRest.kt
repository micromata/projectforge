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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.ProjektKost2Service
import org.projectforge.common.StringHelper
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateFieldMeta
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UISelectValue
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
  private lateinit var projectEntityRest: ProjectEntityRest

  @Autowired
  private lateinit var kostCache: KostCache

  @Autowired
  private lateinit var projektKost2Service: ProjektKost2Service

  override val layoutContext: LayoutContext = LayoutContext(ProjektDO::class.java)

  override val listPageUrl: String = "/${MenuItemDefId.PROJECT_LIST.url}"

  @PostConstruct
  private fun postConstruct() {
    pagesRest = projectEntityRest
  }

  /**
   * The fields of the mass update, for the next frontend that renders the form itself, including
   * the cost 2 types ([KOST2_ARTS]): a custom field whose options are all cost 2 types. Its
   * parameter carries the picked type ids comma separated in `textValue`, with `append` to create/activate
   * their cost 2 units or `delete` to deactivate them (see [proceedMassUpdate]).
   */
  override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
    return listOf(
      MassUpdateFieldDeclaration("headOfBusinessManager"),
      MassUpdateFieldDeclaration("projectManager"),
      MassUpdateFieldDeclaration("salesManager"),
      MassUpdateFieldDeclaration("status"),
      MassUpdateFieldDeclaration("description", showAppendOption = true, minLengthOfTextArea = 1001),
      MassUpdateFieldDeclaration(KOST2_ARTS, custom = true, values = kost2ArtValues()),
    )
  }

  override fun checkParamHasAction(
    params: Map<String, MassUpdateParameter>,
    param: MassUpdateParameter,
    field: String,
  ): Boolean {
    if (field == "headOfBusinessManager" || field == "projectManager" || field == "salesManager") {
      return param.id != null
    }
    if (field == KOST2_ARTS) {
      return kost2ArtIdsOf(param).isNotEmpty() && ((param.append == true) xor (param.delete == true))
    }
    return super.checkParamHasAction(params, param, field)
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
    val params = massUpdateContext.massUpdateParams
    // No property of the project: its cost 2 units are changed besides it (markCurrentModified).
    massUpdateContext.ignoreFieldsForModificationCheck = listOf(KOST2_ARTS)
    val kost2ArtsParam = params[KOST2_ARTS]?.takeIf { checkParamHasAction(params, it, KOST2_ARTS) }
    val kost2ArtIds = kost2ArtsParam?.let { kost2ArtIdsOf(it) }.orEmpty()
    projects.forEach { project ->
      massUpdateContext.startUpdate(project)
      TextFieldModification.processTextParameter(project, "description", params)
      proceedMassUpdateUserField(params, ProjektDO::headOfBusinessManager, project)
      proceedMassUpdateUserField(params, ProjektDO::projectManager, project)
      proceedMassUpdateUserField(params, ProjektDO::salesManager, project)
      params["status"]?.let { param ->
        if (param.delete == true) {
          project.status = null
        }
        param.textValue?.let { textValue ->
          project.status = ProjektStatus.valueOf(textValue)
        }
      }
      massUpdateContext.commitUpdate(
        identifier4Message = project.displayName,
        project,
        update = {
          projektDao.update(project)
          // As the edit form: an ended project's cost 2 units are ended anyway, so they are left alone
          // (also if it is ended by this mass update).
          val projektId = project.id
          if (kost2ArtIds.isNotEmpty() && projektId != null && project.status != ProjektStatus.ENDED) {
            val oldArtIds = activeKost2ArtIds(projektId)
            val append = kost2ArtsParam?.append == true
            val changed = if (append) {
              projektKost2Service.activate(projektId, kost2ArtIds)
            } else {
              projektKost2Service.deactivate(projektId, kost2ArtIds) // A no-op for a type it has no active unit of.
            }
            if (changed) {
              massUpdateContext.markCurrentModified()
              // After activate all picked types are active, after deactivate none of them.
              val newArtIds = if (append) oldArtIds + kost2ArtIds else oldArtIds - kost2ArtIds.toSet()
              massUpdateContext.recordCurrentModification(
                KOST2_ARTS,
                formatKost2ArtIds(oldArtIds),
                formatKost2ArtIds(newArtIds),
              )
            }
          }
        },
      )
    }
    return null
  }

  override fun getFieldTranslation(field: String): String {
    if (field == KOST2_ARTS) {
      return translate("fibu.kost2art.kost2arten")
    }
    return super.getFieldTranslation(field)
  }

  /** The picked cost 2 types by their labels ("04: Name, 05: Name") rather than the posted ids. */
  override fun formatPreviewValue(param: MassUpdateParameter, meta: MassUpdateFieldMeta?): String? {
    if (meta?.field == KOST2_ARTS) {
      val labels = meta.values.orEmpty().associate { it.id to it.displayName }
      return kost2ArtIdsOf(param).joinToString { labels[it.toString()] ?: StringHelper.format2DigitNumber(it) }
    }
    return super.formatPreviewValue(param, meta)
  }

  /** Cost 2 types are activated or deactivated, not "appended" or "deleted" as the generic text reads. */
  override fun describePreviewChange(
    field: String,
    param: MassUpdateParameter,
    label: String,
    value: String?,
  ): String? {
    if (field != KOST2_ARTS) {
      return null
    }
    val key = if (param.delete == true) {
      "fibu.projekt.massUpdate.kost2Arts.confirm.deactivate"
    } else {
      "fibu.projekt.massUpdate.kost2Arts.confirm.activate"
    }
    return translateMsg(key, label, value ?: "")
  }

  /** All cost 2 types, "04: Name", as the list's cost 2 type filter offers them. */
  private fun kost2ArtValues(): List<UISelectValue<String>> {
    return kostCache.getKost2Arts().mapNotNull { art ->
      val id = art.id ?: return@mapNotNull null
      val number = StringHelper.format2DigitNumber(id)
      UISelectValue(id.toString(), if (art.name.isNullOrBlank()) number else "$number: ${art.name}")
    }
  }

  /** The types of the project's active cost 2 units (as the list column shows them). */
  private fun activeKost2ArtIds(projektId: Long): Set<Long> {
    return kostCache.getKost2ForProjekt(projektId)
      .filter { projektKost2Service.isActive(it) }
      .mapNotNull { it.kost2Art?.id }
      .toSet()
  }

  /** "01, 04, 33" as the list's cost 2 types column. */
  private fun formatKost2ArtIds(artIds: Collection<Long>): String {
    return artIds.sorted().joinToString { StringHelper.format2DigitNumber(it) }
  }

  private fun kost2ArtIdsOf(param: MassUpdateParameter): List<Long> {
    return param.textValue?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.distinct().orEmpty()
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

  companion object {
    /** The custom mass update field of the cost 2 types (see [fieldDeclarations]). */
    private const val KOST2_ARTS = "kost2Arts"
  }
}
