/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.jobs;

import org.projectforge.business.meb.MebJobExecutor;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job should be scheduled nightly.
 *
 * @author Florian Blumenstein
 */
@Component
public class CronNightlyJob
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CronNightlyJob.class);

  @Autowired
  private HibernateSearchReindexer hibernateSearchReindexer;

  @Autowired
  private MebJobExecutor mebJobExecutor;

  //@Scheduled(cron = "0 30 2 * * *")
  @Scheduled(cron = "${projectforge.cron.nightly}")
  public void execute()
  {
    log.info("Nightly job started.");
    try {
      hibernateSearchReindexer.execute();
    } catch (final Throwable ex) {
      log.error("While executing hibernate search re-index job: " + ex.getMessage(), ex);
    }
    if (mebJobExecutor != null) {
      try {
        mebJobExecutor.execute(true);
      } catch (final Throwable ex) {
        log.error("While executing MEB job: " + ex.getMessage(), ex);
      }
    }
    log.info("Nightly job job finished.");
  }

}
