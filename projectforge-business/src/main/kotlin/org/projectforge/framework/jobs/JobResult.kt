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

package org.projectforge.framework.jobs

class JobResult<T> {
  fun getResultObject(): T? {
    return resultObject
  }

  fun setStatus(status: Status?): JobResult<T> {
    this.status = status
    return this
  }

  fun setResultObject(resultObject: T): JobResult<T> {
    this.resultObject = resultObject
    return this
  }

  fun setErrorString(errorString: String?): JobResult<T> {
    this.errorString = errorString
    return this
  }

  enum class Status {
    OK, ERROR
  }

  var status: Status? = null
    private set
  private var resultObject: T? = null
  var errorString: String? = null
    private set
}
