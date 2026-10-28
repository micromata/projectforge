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

package org.projectforge.plugins.skillmatrix

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.PageRoutes
import org.projectforge.business.PfCaches
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMassUpdateRest
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UISelectValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

/**
 * Mass update of skill entries after selection in the next list (`skill-entry.page.tsx`): rating, interest
 * and comment. The skill itself is left out (one skill per owner, setting it on several entries would only
 * produce doublets), as is the owner (read-only).
 *
 * Rating and interest are plain ints 0..3, which the metadata path would offer as a bare number input; they
 * are declared with explicit [MassUpdateFieldDeclaration.values], so the frontend renders a combobox of the
 * level texts (empty = no change) and posts the level as `textValue`. Level 0 ("none") is one of the
 * values, so there is no delete option.
 *
 * Entries of other users are only readable ([SkillRight]): selecting them is possible, their update fails
 * with the access error of the dao and is reported per entry.
 */
@RestController
@RequestMapping("${Rest.URL}/${SkillMatrixPlugin.CATEGORY}${AbstractMassUpdateRest.URL_SUFFIX_SELECTED}")
class SkillEntryMassUpdateRest : AbstractMassUpdateRest<SkillEntryDO>() {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var skillEntryDao: SkillEntryDao

    @Autowired
    private lateinit var skillEntryEntityRest: SkillEntryEntityRest

    override val layoutContext: LayoutContext = LayoutContext(SkillEntryDO::class.java)

    override val listPageUrl: String = "/${PageRoutes.listUrl(SkillMatrixPlugin.CATEGORY)}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = skillEntryEntityRest
    }

    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            levelDeclaration("rating", SkillEntryDO.MIN_VAL_RATING..SkillEntryDO.MAX_VAL_RATING),
            levelDeclaration("interest", SkillEntryDO.MIN_VAL_INTEREST..SkillEntryDO.MAX_VAL_INTEREST),
            MassUpdateFieldDeclaration("comment", showAppendOption = true),
        )
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<SkillEntryDO>,
    ): ResponseEntity<*>? {
        val entries = skillEntryDao.select(selectedIds)
        if (entries.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        entries.forEach { entry ->
            massUpdateContext.startUpdate(entry)
            params["rating"]?.let { param ->
                if (param.hasAction) {
                    param.textValue?.toIntOrNull()?.let { entry.rating = it }
                }
            }
            params["interest"]?.let { param ->
                if (param.hasAction) {
                    param.textValue?.toIntOrNull()?.let { entry.interest = it }
                }
            }
            TextFieldModification.processTextParameter(entry, "comment", params)
            // Skill and owner, since the skill alone is ambiguous over the entries of several users.
            val owner = caches.getUserIfNotInitialized(entry.owner)
            massUpdateContext.commitUpdate(
                identifier4Message = "${entry.skill} (${owner?.displayName})",
                entry,
                update = { skillEntryDao.update(entry) },
            )
        }
        return null
    }

    /** A level field (rating or interest) as a combobox of its translated level texts. */
    private fun levelDeclaration(field: String, levels: IntRange): MassUpdateFieldDeclaration {
        return MassUpdateFieldDeclaration(
            field = field,
            showDeleteOption = false,
            values = levels.map {
                UISelectValue("$it", translate("plugins.skillmatrix.$field.$it"))
            },
        )
    }
}
