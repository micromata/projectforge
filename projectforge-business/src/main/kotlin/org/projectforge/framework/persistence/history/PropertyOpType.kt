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

/**
 * Operation type on one entity.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
enum class PropertyOpType {
    /**
     * The Undefined.
     */
    Undefined,

    /**
     * The Insert.
     */
    Insert,

    /**
     * The Update.
     */
    Update,

    /**
     * The Delete.
     */
    Delete;

    companion object {
        /**
         * From string.
         *
         * @param key the key
         * @return the property op type
         */
        fun fromString(key: String): PropertyOpType {
            for (v in entries) {
                if (v.name == key == true) {
                    return v
                }
            }
            return Undefined
        }
    }
}
