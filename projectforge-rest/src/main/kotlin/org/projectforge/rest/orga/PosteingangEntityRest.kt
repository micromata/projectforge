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
import org.projectforge.business.orga.PosteingangDO
import org.projectforge.business.orga.PosteingangDao
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOEntityRest
import org.projectforge.ui.ValidationError
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the inbox ("Posteingang"), serving the hand-built list and edit page of
 * projectforge-next (see components/features/post). Replaces the React UILayout `PosteingangPagesRest`.
 *
 * The free texts complete from the stored entries through the inherited `autocomplete?property=` endpoint
 * ([PosteingangDao.isAutocompletionPropertyEnabled]).
 */
@RestController
@RequestMapping("${Rest.URL}/incomingMail")
class PosteingangEntityRest :
    AbstractDOEntityRest<PosteingangDO, PosteingangDao>(PosteingangDao::class.java, "orga.posteingang.title") {

    /**
     * A new entry is dated today and received as e-mail.
     */
    override fun newBaseDO(request: HttpServletRequest?): PosteingangDO {
        val inbox = super.newBaseDO(request)
        inbox.datum = PFDay.now().localDate
        inbox.type = PostType.E_MAIL
        return inbox
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: PosteingangDO) {
        validatePostDate(validationErrors, dto.datum)
    }
}
