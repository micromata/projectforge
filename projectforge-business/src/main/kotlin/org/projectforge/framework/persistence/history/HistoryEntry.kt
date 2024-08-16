//
// Copyright (C) 2010-2016 Micromata GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.projectforge.framework.persistence.history

import org.projectforge.framework.persistence.api.IdObject
import java.io.Serializable
import java.util.*

/**
 * An change for an Entity.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
interface HistoryEntry<I : Serializable> : IdObject<I> {
    /**
     * Gets the modified at.
     *
     * @return the modified at
     */
    val modifiedAt: Date?

    /**
     * Gets the modified by.
     *
     * @return the modified by
     */
    val modifiedBy: String?

    val userName: String?
        /**
         * alias to getModifiedBy.
         *
         * @return the user which modified the entry
         */
        get() = modifiedBy

    /**
     * Gets the diff entries.
     *
     * @return the diff entries
     */
    val diffEntries: List<DiffEntry>?

    /**
     * Gets the entity op type.
     *
     * @return the entity op type
     */
    val entityOpType: EntityOpType?

    /**
     * Gets the entity name.
     *
     * @return the entity name
     */
    val entityName: String?

    /**
     * Gets the entity id.
     *
     * @return the entity id
     */
    val entityId: Long?
}
