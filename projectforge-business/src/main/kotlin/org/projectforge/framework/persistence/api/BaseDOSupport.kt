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

package org.projectforge.framework.persistence.api

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
class BaseDOSupport {
    private var transientAttributes: MutableMap<String, Any?>? = null

    fun getTransientAttribute(key: String): Any? {
        transientAttributes.let {
            return if (it == null) {
                 null
            } else {
                synchronized(this) {
                    it[key]
                }
            }
        }
    }

    fun removeTransientAttribute(key: String): Any? {
        transientAttributes.let {
            return if (it == null) {
                null
            } else {
                synchronized(this) {
                    it.remove(key)
                }
            }
        }
    }

    fun setTransientAttribute(key: String, value: Any?) {
        synchronized(this) {
            if (transientAttributes == null) {
                transientAttributes = mutableMapOf()
            }
            transientAttributes!![key] = value
        }
    }
}
