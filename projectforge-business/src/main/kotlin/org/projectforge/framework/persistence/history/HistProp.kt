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
 * Description of a History Property.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
class HistProp {
    /**
     * The name.
     */
    var name: String? = null

    /**
     * The type.
     */
    var type: String? = null

    /**
     * The value.
     */
    @JvmField
    var value: String? = null

    constructor()

    constructor(name: String?, type: String?, value: String?) : super() {
        this.name = name
        this.type = type
        this.value = value
    }
}
