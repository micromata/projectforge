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

import java.util.Calendar;

import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.time.DateHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Job should be scheduled hourly.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CronHourlyJob extends AbstractCronJob
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CronHourlyJob.class);

  private MyDatabaseUpdateService databaseUpdateDao;

  private HibernateSearchReindexer hibernateSearchReindexer;

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException
  {
    log.info("Hourly job started.");
    if (databaseUpdateDao == null) {
      wire(context);
    }
    if (databaseUpdateDao == null) {
      log.fatal("Job not configured, aborting.");
      return;
    }
    try {
      log.info("Starting (re-)indexing of history entries of the last 24 hours.");
      final Calendar cal = Calendar.getInstance(DateHelper.UTC);
      cal.add(Calendar.DAY_OF_YEAR, -1);
      final ReindexSettings settings = new ReindexSettings(cal.getTime(), null);
      hibernateSearchReindexer.rebuildDatabaseSearchIndices(settings, PfHistoryMasterDO.class);
    } catch (final Throwable ex) {
      log.error("While executing fix job for data base history entries: " + ex.getMessage(), ex);
    }
    log.info("Hourly job job finished.");
  }

  @Override
  protected void wire(final JobExecutionContext context)
  {
    databaseUpdateDao = (MyDatabaseUpdateService) wire(context, "databaseUpdateDao");
    hibernateSearchReindexer = (HibernateSearchReindexer) wire(context, "hibernateSearchReindexer");
  }
}
