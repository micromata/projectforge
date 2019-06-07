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

import org.projectforge.business.book.BookFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.MagicFilter
import org.projectforge.rest.core.MagicFilterEntry
import org.projectforge.rest.dto.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/dev")
class DevelopmentRest() {

    /**
     * Magic demo filter.
     */
    @GetMapping("magicDemoFilters")
    fun magicFilter(): List<MagicFilter<BookFilter>> {
        val list = mutableListOf<MagicFilter<BookFilter>>()

        var filter = MagicFilter<BookFilter>(entries = mutableListOf())
        filter.entries!!.add(MagicFilterEntry(search = "fin"))
        filter.entries!!.add(MagicFilterEntry(search = "inhard", matchType = MagicFilterEntry.MatchType.ENDS_WITH))
        filter.entries!!.add(MagicFilterEntry(search = "amb", matchType = MagicFilterEntry.MatchType.CONTAINS))
        filter.entries!!.add(MagicFilterEntry(field = "modifiedByUser", value = User(2)))
        filter.entries!!.add(MagicFilterEntry(field = "title", search = "java", matchType = MagicFilterEntry.MatchType.STARTS_WITH))
        filter.entries!!.add(MagicFilterEntry(field = "yearOfPublishing", fromValue = 2010))
        list.add(filter)

        filter = MagicFilter<BookFilter>(BookFilter(), entries = mutableListOf())
        filter.entries!!.add(MagicFilterEntry(search = "Mixed filter (magic and classic)"))
        filter.searchFilter!!.isPresent = true
        filter.searchFilter!!.searchString = "java"
        list.add(filter)

        filter = MagicFilter<BookFilter>(entries = mutableListOf())
        filter.entries!!.add(MagicFilterEntry(field = "modifiedInterval",
                fromValue = "2019-04-28'T'10:00:05.000Z",
                toValue = "2019-04-28'T'17:00:05.000Z"))
        filter.entries!!.add(MagicFilterEntry(field = "type", values = mutableListOf("BOOK", "MAGAZINE")))
        list.add(filter)

        return list
    }
}
