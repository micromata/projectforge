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


package org.projectforge.plugins.memo.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.memo.MemoDO
import org.projectforge.plugins.memo.MemoDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOEntityRest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the memos, serving the hand-built projectforge-next page (see
 * components/features/memo). Replaces the retired `MemoPagesRest`, whose server-side `UILayout` (React) moved
 * onto the frontend; the path `/rs/memo` is unchanged.
 *
 * Memos are personal: everybody reads and writes only the own ones ([org.projectforge.plugins.memo.MemoRight]),
 * and [MemoDao] always stores the logged-in user as owner.
 */
@RestController
@RequestMapping("${Rest.URL}/memo")
class MemoEntityRest : AbstractDOEntityRest<MemoDO, MemoDao>(MemoDao::class.java, "plugins.memo.title") {
    /**
     * A new memo belongs to the logged-in user.
     */
    override fun newBaseDO(request: HttpServletRequest?): MemoDO {
        val memo = super.newBaseDO(request)
        memo.owner = ThreadLocalUserContext.loggedInUser
        return memo
    }
}
