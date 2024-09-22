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

package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.BaseDO
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

class PropertyContext<IdType : Serializable>(
    val kClass: KClass<*>,
    val src: BaseDO<IdType>,
    val dest: BaseDO<IdType>,
    val propertyName: String,
    val property: KMutableProperty1<*, *>,
    val srcPropertyValue: Any?,
    val destPropertyValue: Any?,
)
