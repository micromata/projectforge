/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.Column
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class ColumnMetaData(fieldName: String, columnAnnotation: Column) {
    val name: String

    val length: Int = columnAnnotation.length

    val nullable = columnAnnotation.nullable

    init {
        if (columnAnnotation.name.isBlank()) {
            name = fieldName
        } else {
            name = columnAnnotation.name
        }
    }
}