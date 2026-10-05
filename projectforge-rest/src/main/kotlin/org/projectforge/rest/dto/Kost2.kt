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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.framework.configuration.ApplicationContextProvider
import java.math.BigDecimal

class Kost2(
  id: Long? = null,
  displayName: String? = null,
  var nummernkreis: Int = 0,
  var bereich: Int = 0,
  var teilbereich: Int = 0,
  var endziffer: Int = 0,
  var kostentraegerStatus: KostentraegerStatus? = null,
  var effectiveKostentraegerStatus: KostentraegerStatus? = null,
  var workFraction: BigDecimal? = null,
  var sharedCost: Boolean? = null,
  var description: String? = null,
  var comment: String? = null,
  var formattedNumber: String? = null,
  var project: Project? = null,
  var kost2Art: Kost2Art? = null,
) : BaseDTODisplayObject<Kost2DO>(id, displayName = displayName) {

  /**
   * @see copyFromMinimal
   */
  constructor(src: Kost2DO) : this() {
    copyFromMinimal(src)
  }

  override fun copyFromMinimal(src: Kost2DO) {
    super.copyFromMinimal(src)
    nummernkreis = src.nummernkreis
    bereich = src.bereich
    teilbereich = src.teilbereich
    endziffer = src.kost2Art?.id?.toInt() ?: 0
    kostentraegerStatus = src.kostentraegerStatus
    effectiveKostentraegerStatus = src.effectiveKostentraegerStatus
    description = src.description
    // Resolve the project through the cache: the Kost2DO handed out by KostCache is detached, so its
    // lazy projekt (and the projekt's lazy kunde) cannot be initialized here - touching them throws
    // "statement closed". The cache returns a fully initialized ProjektDO instead.
    this.project = ProjektCache.instance.getProjektIfNotInitialized(src.projekt)?.let {
      val project = Project()
      project.copyFromMinimal(it)
      project
    }
    displayName = KostFormatter.instance.formatKost2(src, KostFormatter.FormatType.TEXT)
  }

  override fun copyFrom(src: Kost2DO) {
    super.copyFrom(src)
    endziffer = src.kost2Art?.id?.toInt() ?: 0
    kostentraegerStatus = src.kostentraegerStatus
    effectiveKostentraegerStatus = src.effectiveKostentraegerStatus
    formattedNumber = src.formattedNumber
    displayName = KostFormatter.instance.formatKost2(src, KostFormatter.FormatType.TEXT)
    this.project = src.projekt?.let {
      val project = Project()
      project.copyFromMinimal(it)
      project.name = it.name
      project
    }
  }

  override fun copyTo(dest: Kost2DO) {
    super.copyTo(dest)
    // The number's last part is the Kost2Art's id; the edit form carries it flat as endziffer (mirroring
    // the legacy Wicket number boxes), so resolve it here to the reference the entity persists - Hibernate
    // loads the row from the stub's id.
    dest.kost2Art = Kost2ArtDO().also { it.id = endziffer.toLong() }
    // The DTO field is named 'project' while the DO field is 'projekt', so the name-based super.copyTo
    // skips it; map it here by id (null clears the reference, e.g. a cost unit without a project).
    dest.projekt = project?.id?.let { id -> ProjektDO().also { it.id = id } }
  }

  companion object {
    private val kost2Dao = ApplicationContextProvider.getApplicationContext().getBean(Kost2Dao::class.java)

    fun getkost2(kost2Id: Long?, minimal: Boolean = true): Kost2? {
      kost2Id ?: return null
      val kost2DO = kost2Dao.findOrLoad(kost2Id) ?: return null
      val kost2 = Kost2()
      if (minimal) {
        kost2.copyFromMinimal(kost2DO)
      } else {
        kost2.copyFrom(kost2DO)
      }
      return kost2
    }
  }
}
