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
     * Lends the given book out by the logged-in user.
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
