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

package org.projectforge.rest.orga

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.orga.PostType
import org.projectforge.business.orga.PostausgangDO
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOEntityRest
import org.projectforge.ui.ValidationError
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the outbox ("Postausgang"), serving the hand-built list and edit page of
 * projectforge-next (see components/features/post). Replaces the React UILayout `PostausgangPagesRest`.
 *
 * The free texts complete from the stored entries through the inherited `autocomplete?property=` endpoint
 * ([PostausgangDao.isAutocompletionPropertyEnabled]).
 */
@RestController
@RequestMapping("${Rest.URL}/outgoingMail")
class PostausgangEntityRest :
    AbstractDOEntityRest<PostausgangDO, PostausgangDao>(PostausgangDao::class.java, "orga.postausgang.title") {

    /**
     * A new entry is dated today and sent as letter.
     */
    override fun newBaseDO(request: HttpServletRequest?): PostausgangDO {
        val outbox = super.newBaseDO(request)
        outbox.datum = PFDay.now().localDate
        outbox.type = PostType.BRIEF
        return outbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: PostausgangDO) {
        validatePostDate(validationErrors, dto.datum)
    }
}
