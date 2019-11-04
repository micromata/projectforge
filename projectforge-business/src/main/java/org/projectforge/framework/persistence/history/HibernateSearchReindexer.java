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

package org.projectforge.framework.persistence.history;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

@Service
public class HibernateSearchReindexer {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HibernateSearchReindexer.class);

  private static final String ERROR_MSG = "Error while re-indexing data base: found lock files while re-indexing data-base. "
          + "Try to run re-index manually in the web administration menu and if occured again, "
          + "shutdown ProjectForge, delete lock file(s) in hibernate-search sub directory and restart.";

  @Autowired
  private SendMail sendMail;

  @Autowired
  private DatabaseDao databaseDao;

  private Date currentReindexRun = null;

  @Autowired
  private PfEmgrFactory emf;

  public void execute() {
    log.info("Re-index job started.");
    if (databaseDao == null) {
      log.error("Job not configured, aborting.");
      return;
    }
    final String result = rebuildDatabaseSearchIndices();
    if (result.contains("*")) {
      log.error(ERROR_MSG);
      final String recipients = GlobalConfiguration.getInstance()
              .getStringValue(ConfigurationParam.SYSTEM_ADMIN_E_MAIL);
      if (StringUtils.isNotBlank(recipients)) {
        log.info("Try to inform administrator about re-indexing error.");
        final Mail msg = new Mail();
        msg.addTo(recipients);
        msg.setProjectForgeSubject("Error while re-indexing ProjectForge data-base.");
        msg.setContent(ERROR_MSG + "\n\nResult:\n" + result);
        msg.setContentType(Mail.CONTENTTYPE_TEXT);
        sendMail.send(msg, null, null);
      }
    }
    log.info("Re-index job finished successfully.");
  }

  public String rebuildDatabaseSearchIndices(final ReindexSettings settings, final Class<?>... classes) {
    if (currentReindexRun != null) {
      final StringBuffer buf = new StringBuffer();
      if (classes != null && classes.length > 0) {
        boolean first = true;
        for (final Class<?> cls : classes) {
          first = StringHelper.append(buf, first, cls.getName(), ", ");
        }
      }
      final String date = DateTimeFormatter.instance().getFormattedDateTime(currentReindexRun, Locale.ENGLISH,
              DateHelper.UTC);
      log.info("Re-indexing of '" + buf.toString()
              + "' cancelled due to another already running re-index job started at " + date + " (UTC):");
      return "Another re-index job is already running. The job was started at: " + date;
    }
    synchronized (this) {
      try {
        currentReindexRun = new Date();
        final StringBuffer buf = new StringBuffer();
        if (classes != null && classes.length > 0) {
          for (final Class<?> cls : classes) {
            reindex(cls, settings, buf);
          }
        } else {
          // Re-index of all ProjectForge entities:
          Set<Class<?>> clsses = emf.getSearchableEntities();
          for (Class<?> clz : clsses) {
            reindex(clz, settings, buf);
          }
        }
        return buf.toString();
      } finally {
        currentReindexRun = null;
      }
    }
  }

  private void reindex(final Class<?> clazz, final ReindexSettings settings, final StringBuffer buf) {
    // PF-378: Performance of run of full re-indexing the data-base is very slow for large data-bases
    // Single transactions needed, otherwise the full run will be very slow for large data-bases.
    try {
      emf.runInTrans(emgr -> {
        databaseDao.reindex(clazz, settings, buf);
        return true;
      });
    } catch (Exception ex) {
      buf.append(" (an error occured, see log file for further information.), ");
      log.error("While rebuilding data-base-search-index for '" + clazz.getName() + "': " + ex.getMessage(), ex);
    }
  }

  public String rebuildDatabaseSearchIndices() {
    return rebuildDatabaseSearchIndices(new ReindexSettings());
  }

}
