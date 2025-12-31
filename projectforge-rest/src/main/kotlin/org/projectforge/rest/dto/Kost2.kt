/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.framework.configuration.ApplicationContextProvider

class Kost2(
  id: Long? = null,
  displayName: String? = null,
  var nummernkreis: Int = 0,
  var bereich: Int = 0,
  var teilbereich: Int = 0,
  var endziffer: Int = 0,
  var kostentraegerStatus: KostentraegerStatus? = null,
  var effectiveKostentraegerStatus: KostentraegerStatus? = null,
  var description: String? = null,
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
    this.project = src.projekt?.let {
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
