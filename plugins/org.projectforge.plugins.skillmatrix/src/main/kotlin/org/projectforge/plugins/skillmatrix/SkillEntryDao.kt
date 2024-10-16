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

package org.projectforge.plugins.skillmatrix

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.springframework.stereotype.Service

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class SkillEntryDao : BaseDao<SkillEntryDO>(SkillEntryDO::class.java) {

    init {
        userRightId = SkillRightId.PLUGIN_SKILL_MATRIX
    }

    private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf("skill")

    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return ENABLED_AUTOCOMPLETION_PROPERTIES.contains(property)
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override fun onInsertOrModify(obj: SkillEntryDO, operationType: OperationType) {
        if (obj.owner == null) {
            obj.owner = ThreadLocalUserContext.loggedInUser // Set always the logged-in user as owner.
        }
        obj.rating = NumberHelper.ensureRange(SkillEntryDO.MIN_VAL_RATING, SkillEntryDO.MAX_VAL_RATING, obj.rating)
        obj.interest =
            NumberHelper.ensureRange(SkillEntryDO.MIN_VAL_INTEREST, SkillEntryDO.MAX_VAL_INTEREST, obj.interest)

        val skillText = obj.normalizedSkill
        getSkills(obj.owner!!)
            .forEach {
                if (obj.id != it.id && skillText == it.normalizedSkill) {
                    throw UserException("plugins.skillmatrix.error.doublet", it.skill)
                }
            }
    }

    override fun newInstance(): SkillEntryDO {
        return SkillEntryDO()
    }

    fun getSkills(owner: PFUserDO): List<SkillEntryDO> {
        return persistenceService.executeNamedQuery(
            SkillEntryDO.FIND_OF_OWNER,
            SkillEntryDO::class.java,
            Pair("ownerId", owner.id),
        )
    }

    companion object {
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("owner.username", "owner.firstname", "owner.lastname")
    }
}
