/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.jobs

import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.rest.dto.User

class JobDTO(
  var id: Int? = null,
  var title: String? = null,
  var area: String? = null,
  var queueName: String? = null,
  var user: User? = null,
) {
  companion object {
    fun create(job: AbstractJob): JobDTO {
      val dto = JobDTO(job.id, job.title, area = job.area, queueName = job.queueName)
      job.userId?.let { userId ->
        dto.user = User.getUser(userId)
      }
      return dto
    }
  }
}
