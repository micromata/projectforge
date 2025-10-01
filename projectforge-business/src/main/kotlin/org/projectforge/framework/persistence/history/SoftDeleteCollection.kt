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

package org.projectforge.framework.persistence.history

/**
 * Marks a collection to use soft-delete instead of physical removal when entries are removed.
 *
 * When a collection is annotated with @SoftDeleteCollection, removed entries will be marked
 * as deleted (deleted = true) instead of being physically removed from the collection.
 * This preserves:
 * - History entries attached to the removed entities
 * - Database referential integrity
 * - Avoids unique constraint violations during collection updates
 *
 * Use this annotation for 1:n parent-child relationships where children are historizable
 * entities (extending AbstractHistorizableBaseDO) that should not be physically deleted.
 *
 * Example:
 * ```
 * @SoftDeleteCollection
 * @PersistenceBehavior(autoUpdateCollectionEntries = true)
 * @get:OneToMany(mappedBy = "parent", ...)
 * var children: MutableList<ChildDO>? = null
 * ```
 *
 * Note: This only affects entries that extend AbstractHistorizableBaseDO. Other entry types
 * will still be physically removed.
 *
 * @see org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SoftDeleteCollection
