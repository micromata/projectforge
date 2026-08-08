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

package org.projectforge.framework.persistence.database

/**
 * You may define different strategies for hibernate search reindexing by registering in ReindexerRegistry.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class ReindexerStrategy(
        /**
         * The fetch string for the hql, e. g. "left join fetch t.attributes"
         */
        join: String? = "",
        /**
         * The id property of the entity ('id' for most objects, 'pk' for HistoryEntryDO).
         */
        val idProperty: String? = "id",
        /**
         * Where to find the property of last modification of the entities, if any.
         * If not given, no re-indexing of entities updated since yesterday is supported and all entities will
         * be re-indexed.
         * 'lastUpdate' for AbstractBaseDO and 'modifiedAt' for StdRecord.
         */
        val modifiedAtProperty: String? = "lastUpdate",
        /**
         * Where to find the name of the entity the rows belong to, if the table holds the rows of several entities
         * (HistoryEntryDO does, no other indexed entity). Only then a re-index started for a list page can
         * restrict itself to its own rows, see [org.projectforge.framework.persistence.api.ReindexSettings.getEntityNames].
         */
        val entityNameProperty: String? = null) {
    val join = if (join.isNullOrBlank()) "" else " ${join.trim()}"
}
