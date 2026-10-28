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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.SkillEntry
import org.projectforge.rest.dto.User
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the skill matrix, serving the hand-built projectforge-next page (see
 * components/features/skill-entry). Replaces the retired `SkillEntryPagesRest`, whose server-side `UILayout`
 * (React) moved onto the frontend; the path `/rs/skillentry` is unchanged.
 *
 * Everybody reads the skills of all users, but writes only the own ones ([SkillRight]). The list shows only the
 * own skills by default (filter "my skills"), and a clone always belongs to the logged-in user.
 */
@RestController
@RequestMapping("${Rest.URL}/skillentry")
class SkillEntryEntityRest : AbstractDTOEntityRest<SkillEntryDO, SkillEntry, SkillEntryDao>(
    SkillEntryDao::class.java,
    "plugins.skillmatrix.title",
    cloneSupport = CloneSupport.CLONE,
) {
    @Autowired
    private lateinit var caches: PfCaches

    /**
     * A new skill belongs to the logged-in user.
     */
    override fun newBaseDO(request: HttpServletRequest?): SkillEntryDO {
        val entry = super.newBaseDO(request)
        entry.owner = ThreadLocalUserContext.loggedInUser
        return entry
    }

    override fun transformFromDB(obj: SkillEntryDO, editMode: Boolean): SkillEntry {
        // Resolve the owner from the user cache before copyFrom: the reflective copy reads the DO's owner
        // field and its copyFromMinimal touches deleted/deactivated, which would initialize the lazy proxy
        // and fire one T_PF_USER select per row (N+1). Swapping in the cached, fully-initialized user (by
        // id only, see UserGroupCache.getUserIfNotInitialized) avoids the load. Same pattern as PfCaches.
        obj.owner = caches.getUserIfNotInitialized(obj.owner)
        val entry = SkillEntry()
        entry.copyFrom(obj)
        obj.owner?.let { entry.owner = User(it) }
        return entry
    }

    override fun transformForDB(dto: SkillEntry): SkillEntryDO {
        val entryDO = SkillEntryDO()
        dto.copyTo(entryDO)
        return entryDO
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterElement(
                "owner",
                UIFilterElement.FilterType.BOOLEAN,
                translate("plugins.skillmatrix.filter.mySkills"),
                defaultFilter = true
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<SkillEntryDO>>? {
        val ownerFilterEntry = source.entries.find { it.field == "owner" }
        if (ownerFilterEntry?.isTrueValue == true) {
            ownerFilterEntry.synthetic = true
            target.createJoin("owner")
            target.add(
                QueryFilter.eq(
                    "owner",
                    ThreadLocalUserContext.loggedInUser!!
                )
            ) // Show only own skills, not from others.
        }
        return null
    }

    /**
     * Sets logged-in-user as owner.
     */
    override fun prepareClone(dto: SkillEntry): SkillEntry {
        val clone = super.prepareClone(dto)
        clone.owner = User(ThreadLocalUserContext.requiredLoggedInUser)
        return clone
    }
}
