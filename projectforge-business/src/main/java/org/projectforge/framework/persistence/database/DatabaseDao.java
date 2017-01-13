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

package org.projectforge.framework.persistence.database;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ClassUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.hibernate.HibernateCompatUtils;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.micromata.genome.jpa.StdRecord;

/**
 * Creates index creation script and re-indexes data-base.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class DatabaseDao
{
  private static final int MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE = 2000;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DatabaseDao.class);

  private Date currentReindexRun = null;

  @Autowired
  private SessionFactory sessionFactory;

  /**
   * Since yesterday and 1,000 newest entries at maximimum.
   *
   * @return
   */
  public static ReindexSettings createReindexSettings(final boolean onlyNewest)
  {
    if (onlyNewest == true) {
      final DayHolder day = new DayHolder();
      day.add(Calendar.DAY_OF_MONTH, -1); // Since yesterday:
      return new ReindexSettings(day.getDate(), 1000); // Maximum 1,000 newest entries.
    } else {
      return new ReindexSettings();
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public String rebuildDatabaseSearchIndices(final Class<?> clazz, final ReindexSettings settings)
  {
    if (currentReindexRun != null) {
      return "Another re-index job is already running. The job was started at: "
          + DateTimeFormatter.instance().getFormattedDateTime(currentReindexRun, Locale.ENGLISH, DateHelper.UTC)
          + " (UTC)";
    }
    final StringBuffer buf = new StringBuffer();
    reindex(clazz, settings, buf);
    return buf.toString();
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void reindex(final Class<?> clazz, final ReindexSettings settings, final StringBuffer buf)
  {
    if (currentReindexRun != null) {
      buf.append(" (cancelled due to another running index-job)");
      return;
    }
    synchronized (this) {
      try {
        currentReindexRun = new Date();
        buf.append(ClassUtils.getShortClassName(clazz));
        reindex(clazz, settings);
        buf.append(", ");
      } finally {
        currentReindexRun = null;
      }
    }
  }

  /**
   * @param clazz
   */
  private long reindex(final Class<?> clazz, final ReindexSettings settings)
  {
    if (settings.getLastNEntries() != null || settings.getFromDate() != null) {
      // OK, only partly re-index required:
      return reindexObjects(clazz, settings);
    }
    // OK, full re-index required:
    if (isIn(clazz, TimesheetDO.class, PfHistoryMasterDO.class) == true) {
      // MassIndexer throws LazyInitializationException for some classes, so use it only for the important classes (with most entries):
      return reindexMassIndexer(clazz);
    }
    return reindexObjects(clazz, null);
  }

  private boolean isIn(final Class<?> clazz, final Class<?>... classes)
  {
    for (final Class<?> cls : classes) {
      if (clazz.equals(cls) == true) {
        return true;
      }
    }
    return false;
  }

  private long reindexObjects(final Class<?> clazz, final ReindexSettings settings)
  {
    final Session session = sessionFactory.getCurrentSession();
    Criteria criteria = createCriteria(session, clazz, settings, true);
    final Long number = (Long) criteria.uniqueResult(); // Get number of objects to re-index (select count(*) from).
    final boolean scrollMode = number > MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE ? true : false;
    log.info("Starting re-indexing of "
        + number
        + " entries (total number) of type "
        + clazz.getName()
        + " with scrollMode="
        + scrollMode
        + "...");
    final int batchSize = 1000;// NumberUtils.createInteger(System.getProperty("hibernate.search.worker.batch_size")
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    HibernateCompatUtils.setFlushMode(fullTextSession, FlushMode.MANUAL);
    HibernateCompatUtils.setCacheMode(fullTextSession, CacheMode.IGNORE);
    long index = 0;
    if (scrollMode == true) {
      // Scroll-able results will avoid loading too many objects in memory
      criteria = createCriteria(fullTextSession, clazz, settings, false);
      final ScrollableResults results = criteria.scroll(ScrollMode.FORWARD_ONLY);
      while (results.next() == true) {
        final Object obj = results.get(0);
        if (obj instanceof ExtendedBaseDO<?>) {
          ((ExtendedBaseDO<?>) obj).recalculate();
        }
        HibernateCompatUtils.index(fullTextSession, obj);
        if (index++ % batchSize == 0) {
          session.flush(); // clear every batchSize since the queue is processed
        }
      }
    } else {
      criteria = createCriteria(session, clazz, settings, false);
      final List<?> list = criteria.list();
      for (final Object obj : list) {
        if (obj instanceof ExtendedBaseDO<?>) {
          ((ExtendedBaseDO<?>) obj).recalculate();
        }
        HibernateCompatUtils.index(fullTextSession, obj);
        if (index++ % batchSize == 0) {
          session.flush(); // clear every batchSize since the queue is processed
        }
      }
    }
    final SearchFactory searchFactory = fullTextSession.getSearchFactory();
    searchFactory.optimize(clazz);
    log.info("Re-indexing of " + index + " objects of type " + clazz.getName() + " done.");
    return index;
  }

  /**
   * @param clazz
   */
  private long reindexMassIndexer(final Class<?> clazz)
  {
    final Session session = sessionFactory.getCurrentSession();
    final Criteria criteria = createCriteria(session, clazz, null, true);
    final Long number = (Long) criteria.uniqueResult(); // Get number of objects to re-index (select count(*) from).
    log.info("Starting (mass) re-indexing of " + number + " entries of type " + clazz.getName() + "...");
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    try {

      fullTextSession.createIndexer(clazz)//
          .batchSizeToLoadObjects(25) //
          //.cacheMode(CacheMode.NORMAL) //
          .threadsToLoadObjects(5) //
          //.threadsForIndexWriter(1) //
          .threadsForSubsequentFetching(20) //
          .startAndWait();
    } catch (final InterruptedException ex) {
      log.error("Exception encountered while reindexing: " + ex.getMessage(), ex);
    }
    final SearchFactory searchFactory = fullTextSession.getSearchFactory();
    searchFactory.optimize(clazz);
    log.info("Re-indexing of " + number + " objects of type " + clazz.getName() + " done.");
    return number;

  }

  private Criteria createCriteria(final Session session, final Class<?> clazz, final ReindexSettings settings,
      final boolean rowCount)
  {
    final Criteria criteria = session.createCriteria(clazz);
    if (rowCount == true) {
      criteria.setProjection(Projections.rowCount());
    } else {
      if (settings != null) {
        if (settings.getLastNEntries() != null) {
          criteria.addOrder(Order.desc("id")).setMaxResults(settings.getLastNEntries());
        }
        String lastUpdateProperty = null;
        if (AbstractBaseDO.class.isAssignableFrom(clazz) == true) {
          lastUpdateProperty = "lastUpdate";
        } else if (StdRecord.class.isAssignableFrom(clazz) == true) {
          lastUpdateProperty = "modifiedAt";
        }
        if (lastUpdateProperty != null && settings.getFromDate() != null) {
          criteria.add(Restrictions.ge(lastUpdateProperty, settings.getFromDate()));
        }
      }
    }
    return criteria;
  }
}
