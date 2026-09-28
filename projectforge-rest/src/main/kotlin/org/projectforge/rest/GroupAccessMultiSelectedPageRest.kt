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

package org.projectforge.rest

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.task.TaskFormatter
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

/**
 * Mass update of access rights after selection in the next list (`access.page.tsx`), the counterpart of the
 * invoice ([org.projectforge.rest.fibu.RechnungMultiSelectedPageRest]) and liquidity mass updates.
 *
 * Only the [GroupTaskAccessDO.description] may be changed here — the group, the structure element (task), the
 * recursive flag and the permission matrix are not mass-editable. Whole entries may additionally be
 * soft-deleted and restored ([supportsMassDeletion]); the delete/restore run over the generic default (see
 * [AbstractMultiSelectedPage.proceedMassDelete]/[AbstractMultiSelectedPage.proceedMassUndelete]), since access
 * has no virtual rows to resolve first. [AccessDao] keeps the task tree in sync through its afterDelete /
 * afterUndelete hooks.
 */
@RestController
@RequestMapping("${Rest.URL}/access${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class GroupAccessMultiSelectedPageRest : AbstractMultiSelectedPage<GroupTaskAccessDO>() {

    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var groupAccessEntityRest: GroupAccessEntityRest

    override val layoutContext: LayoutContext = LayoutContext(GroupTaskAccessDO::class.java)

    override fun getTitleKey(): String {
        // Generic "Multi selection" / "Mehrfachauswahl"; no entity-specific title wanted.
        return "multiselection.button"
    }

    @PostConstruct
    private fun postConstruct() {
        pagesRest = groupAccessEntityRest
    }

    /**
     * The only editable field: the free-text description. It is an ordinary text field on
     * [GroupTaskAccessDO], so its label, type and length are resolved from the entity metadata; set / append /
     * replace / delete all apply.
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            MassUpdateFieldDeclaration("description", showAppendOption = true),
        )
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        createAndAddFields(layoutContext, massUpdateData, layout, "description", showAppendOption = true)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<GroupTaskAccessDO>,
    ): ResponseEntity<*>? {
        val entries = accessDao.select(selectedIds)
        if (entries.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        entries.forEach { access ->
            massUpdateContext.startUpdate(access)
            TextFieldModification.processTextParameter(access, "description", params)
            massUpdateContext.commitUpdate(
                identifier4Message = identifier(access),
                access,
                update = { accessDao.update(access) },
            )
        }
        return null
    }

    /** Access rights may be deleted and restored in bulk (soft delete via [AccessDao]). */
    override fun supportsMassDeletion(): Boolean = true

    /** How an entry is named in the protocol and per-entry messages: the group on the structure element. */
    private fun identifier(access: GroupTaskAccessDO): String {
        val group = caches.getGroup(access.groupId)?.name
        val task = TaskFormatter.getTaskPath(access.taskId)
        return listOfNotNull(group, task).joinToString(" – ").ifEmpty { "#${access.id}" }
    }

    override fun ensureUserLogSubscription(): LogSubscription {
        val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
        val displayTitle = translate("multiselection.button")
        return LogSubscription.ensureSubscription(
            title = "GroupTaskAccess",
            displayTitle = displayTitle,
            user = username,
            create = { title, user ->
                LogSubscription(
                    title,
                    user,
                    LogEventLoggerNameMatcher(
                        "org.projectforge.framework.access.AccessDao",
                        "org.projectforge.framework.persistence.api.BaseDaoSupport|GroupTaskAccessDO",
                    ),
                    maxSize = 10000,
                    displayTitle = displayTitle,
                )
            },
        )
    }
}
