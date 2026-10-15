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

package org.projectforge.rest.fibu.kost

import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Kost2Art
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the cost-2 types ("Kost2-Arten"), serving the hand-built
 * projectforge-next page (see components/features/cost2-type). Replaces the retired
 * `Kost2ArtPagesRest`, whose server-side `UILayout` moved onto the frontend.
 *
 * Unlike its cost-1/cost-2 siblings, a Kost2Art's primary key is the user-entered two-digit number,
 * not a generated id (`Kost2ArtDao.avoidNullIdCheckBeforeSave`); the edit page therefore lets the id
 * be typed for a new entry. `Kost2ArtDao.select` already sorts ascending by id, so no sort mapping is
 * needed here.
 */
@RestController
@RequestMapping("${Rest.URL}/cost2Type")
class Kost2ArtEntityRest :
    AbstractDTOEntityRest<Kost2ArtDO, Kost2Art, Kost2ArtDao>(Kost2ArtDao::class.java, "fibu.kost2art.title") {
    override fun transformFromDB(obj: Kost2ArtDO, editMode: Boolean): Kost2Art {
        val kost2Art = Kost2Art()
        kost2Art.copyFrom(obj)
        return kost2Art
    }

    override fun transformForDB(dto: Kost2Art): Kost2ArtDO {
        val kost2ArtDO = Kost2ArtDO()
        dto.copyTo(kost2ArtDO)
        return kost2ArtDO
    }
}
