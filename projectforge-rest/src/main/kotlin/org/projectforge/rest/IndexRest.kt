/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.rest.config.Rest
import org.projectforge.ui.UILayout
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/index")
class IndexRest {
    @GetMapping
    fun getTranslations(): UILayout {
        val layout = UILayout("")
        layout.addTranslations("goreact.index.classics.header",
                "goreact.index.classics.body1",
                "goreact.index.classics.body2",
                "goreact.index.react.header",
                "goreact.index.react.body1",
                "goreact.index.react.body2",
                "goreact.index.both.header",
                "goreact.index.both.body1",
                "goreact.index.both.body2")
        return layout
    }
}
