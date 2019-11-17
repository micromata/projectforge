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

import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * Job should be scheduled hourly.
 *
 * @author Florian Blumenstein
 */
@Component
public class CronReindexingHourlyJob {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CronReindexingHourlyJob.class);

  @Autowired
  private DatabaseService databaseUpdateDao;

  @Autowired
  private HibernateSearchReindexer hibernateSearchReindexer;

  @Autowired
  private PfEmgrFactory emgrFactory;

  //@Scheduled(cron = "0 0 * * * *")
  //@Scheduled(cron = "${projectforge.cron.hourly}")

  /**
   * In ms.
   */
  @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
  public void execute() {
    log.info("Hourly job started.");
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

}
