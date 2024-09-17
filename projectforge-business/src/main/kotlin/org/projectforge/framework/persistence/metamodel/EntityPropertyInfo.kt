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

package org.projectforge.framework.persistence.metamodel

import org.projectforge.common.AnnotationsUtils
import kotlin.reflect.KClass


/**
 * Represents a property of an entity and contains all annotation of package jakarta.persistence.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class EntityPropertyInfo(
    val entityClass: Class<*>,
    val propertyName: String,
) {
    private val annotations = mutableSetOf<Annotation>()

    init {
        AnnotationsUtils.getAnnotations(entityClass, propertyName).forEach { ann ->
            if (ann.annotationClass.qualifiedName?.startsWith("jakarta.persistence") == true) {
                annotations.add(ann)
            }
        }
    }

    fun hasAnnotation(annotationClass: KClass<out Annotation>): Boolean {
        return getAnnotation(annotationClass) != null
    }

    fun <Ann : Annotation> getAnnotation(annotationClass: KClass<out Ann>): Ann? {
        @Suppress("UNCHECKED_CAST")
        return annotations.find { it.annotationClass == annotationClass } as? Ann
    }
}
