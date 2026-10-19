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

import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The cost search ("Kost-Suche") of projectforge-next: a cost 1 or cost 2 unit picked by the shared autocompletion
 * (`cost1/autosearch`, `cost2/autosearch`) resolved to its number and long name, which finance users copy into other
 * systems. Shown as a page, as a floating panel on every page and as a pop-out window.
 */
@RestController
@RequestMapping("${Rest.URL}/costSearch")
class CostSearchRest {
    /**
     * @param number The raw number (cost 2: `512345678`), as copied into other systems.
     * @param name The long display name, number included (`5.123.45.67 - …`).
     */
    class CostDetails(val number: Int, val name: String)

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @AccessChecked("DAO: Kost1Dao.find (select access)")
    @GetMapping("cost1/{id}")
    fun getCost1(@PathVariable("id") id: Long): ResponseEntity<CostDetails> {
        val kost = kost1Dao.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CostDetails(kost.nummer, kostFormatter.formatKost1(kost, KostFormatter.FormatType.LONG)))
    }

    @AccessChecked("DAO: Kost2Dao.find (select access)")
    @GetMapping("cost2/{id}")
    fun getCost2(@PathVariable("id") id: Long): ResponseEntity<CostDetails> {
        val kost = kost2Dao.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(CostDetails(kost.nummer, kostFormatter.formatKost2(kost, KostFormatter.FormatType.LONG)))
    }
}
