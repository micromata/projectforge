/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.importer

abstract class ImportStorage<O : Any> {
  val foundColumns = mutableMapOf<String, String>()
  val ignoredColumns = mutableListOf<String>()

  /**
   * Mapping of columns to properties.
   */
  val columnMapping = mutableMapOf<Int, MappingInfoEntry>()

  var entries = mutableListOf<ImportEntry<O>>()

  /**
   * Prepares an entity (normally only by return new object).
   */
  abstract fun prepareEntity(): O

  /**
   * Set the property of the prepared entity.
   */
  abstract fun setProperty(obj: O, mappingInfoEntry: MappingInfoEntry, value: String)

  /**
   * Store or skip this entity after the setting of all properties.
   */
  abstract fun commitEntity(obj: O)
}
