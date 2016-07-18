/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Job should be scheduled nightly.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CronNightlyJob extends AbstractCronJob
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CronNightlyJob.class);

  private HibernateSearchReindexer hibernateSearchReindexer;

  private MebJobExecutor mebJobExecutor;

  public void execute(final JobExecutionContext context) throws JobExecutionException
  {
    log.info("Nightly job started.");
    if (hibernateSearchReindexer == null) {
      wire(context);
    }
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

  @Override
  protected void wire(final JobExecutionContext context)
  {
    hibernateSearchReindexer = (HibernateSearchReindexer) wire(context, "hibernateSearchReindexer");
    mebJobExecutor = (MebJobExecutor) wire(context, "mebJobExecutor");
  }
}
