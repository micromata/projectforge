/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix

import mu.KotlinLogging
import org.projectforge.business.privacyprotection.CronPrivacyProtectionJob
import org.projectforge.business.privacyprotection.IPrivacyProtectionJob
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.jpa.PfEmgr
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class SkillMatrixPrivacyProtectionJob : IPrivacyProtectionJob {
  @Autowired
  private lateinit var emgrFactory: PfEmgrFactory

  @Autowired
  private lateinit var purgeCronPrivacyProtectionJob: CronPrivacyProtectionJob

  @Autowired
  private lateinit var userDao: UserDao

  @PostConstruct
  private fun postConstruct() {
    purgeCronPrivacyProtectionJob.register(this)
  }

  override fun execute() {
    val date = PFDateTime.now().minusMonths(3L)
    log.info("Purge skill matrix entries of leavers (deleted/deactivated users with lastUpdate < ${date.isoString}Z)...")

    userDao.internalLoadAll().forEach { user ->
      if (user != null && (user.deactivated || user.isDeleted)) {
        if (user.lastUpdate != null && user.lastUpdate < date.utilDate) {
          emgrFactory.runInTrans { emgr: PfEmgr ->
            val counter = emgr.entityManager
              .createNamedQuery(SkillEntryDO.DELETE_ALL_OF_USER)
              .setParameter("userId", user.id)
              .executeUpdate()
            if (counter > 0) {
              log.info { "Deleted $counter entries of the skill matrix of user '${user.username}' with id ${user.id}." }
            }
            true
          }
        }
      }
    }
    log.info("Purging of skill matrix entries done.")
  }
}
