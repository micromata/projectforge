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

package org.projectforge.framework.persistence.database;

import de.micromata.genome.jpa.StdRecord;
import org.apache.commons.lang3.ClassUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Creates index creation script and re-indexes data-base.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class DatabaseDao {
  private static final int MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE = 2000;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseDao.class);

  private Date currentReindexRun = null;

  @Autowired
  private PfEmgrFactory emgrFactory;

  /**
   * Since yesterday and 1,000 newest entries at maximimum.
   */
  public static ReindexSettings createReindexSettings(final boolean onlyNewest) {
    if (onlyNewest) {
      final DayHolder day = new DayHolder();
      day.add(Calendar.DAY_OF_MONTH, -1); // Since yesterday:
      return new ReindexSettings(day.getDate(), 1000); // Maximum 1,000 newest entries.
    } else {
      return new ReindexSettings();
    }
  }

  public <T> String rebuildDatabaseSearchIndices(final Class<T> clazz, final ReindexSettings settings) {
    if (currentReindexRun != null) {
      return "Another re-index job is already running. The job was started at: "
              + DateTimeFormatter.instance().getFormattedDateTime(currentReindexRun, Locale.ENGLISH, DateHelper.UTC)
              + " (UTC)";
    }
    final StringBuffer buf = new StringBuffer();
    reindex(clazz, settings, buf);
    return buf.toString();
  }

  public <T> void reindex(final Class<T> clazz, final ReindexSettings settings, final StringBuffer buf) {
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
  private <T> long reindex(final Class<T> clazz, final ReindexSettings settings) {
    if (settings.getLastNEntries() != null || settings.getFromDate() != null) {
      // OK, only partly re-index required:
      return reindexObjects(clazz, settings);
    }
    // OK, full re-index required:
    if (isIn(clazz, TimesheetDO.class, PfHistoryMasterDO.class)) {
      // MassIndexer throws LazyInitializationException for some classes, so use it only for the important classes (with most entries):
      return reindexMassIndexer(clazz);
    }
    return reindexObjects(clazz, null);
  }

  private boolean isIn(final Class<?> clazz, final Class<?>... classes) {
    for (final Class<?> cls : classes) {
      if (clazz.equals(cls)) {
        return true;
      }
    }
    return false;
  }

  private <T> long reindexObjects(final Class<T> clazz, final ReindexSettings settings) {
    return emgrFactory.runInTrans(emgr -> {
      final EntityManager em = emgr.getEntityManager();
      final Long number = getRowCount(em, clazz); // Get number of objects to re-index (select count(*) from).
      final boolean scrollMode = number > MIN_REINDEX_ENTRIES_4_USE_SCROLL_MODE;
      log.info("Starting re-indexing of "
              + number
              + " entries (total number) of type "
              + clazz.getName()
              + " with scrollMode="
              + scrollMode
              + "...");
      final int batchSize = 1000;// NumberUtils.createInteger(System.getProperty("hibernate.search.worker.batch_size")
      final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
      fullTextEntityManager.setFlushMode(FlushModeType.COMMIT);
      //HibernateCompatUtils.setFlushMode(fullTextSession, FlushMode.MANUAL);
      //HibernateCompatUtils.setCacheMode(fullTextSession, CacheMode.IGNORE);
      long index = 0;
      if (scrollMode) {
        // Scroll-able results will avoid loading too many objects in memory
        TypedQuery<T> query = createCriteria(em, clazz, settings);
        org.hibernate.query.Query hquery = query.unwrap(org.hibernate.query.Query.class);
        ScrollableResults results = hquery.scroll(ScrollMode.FORWARD_ONLY);
        while (results.next()) {
          final Object obj = results.get(0);
          if (obj instanceof ExtendedBaseDO<?>) {
            ((ExtendedBaseDO<?>) obj).recalculate();
          }
          fullTextEntityManager.index(obj);
          if (index++ % batchSize == 0) {
            fullTextEntityManager.flush(); // clear every batchSize since the queue is processed
          }
        }
      } else {
        TypedQuery<T> query = createCriteria(em, clazz, settings);
        final List<T> list = query.getResultList();
        for (final Object obj : list) {
          if (obj instanceof ExtendedBaseDO<?>) {
            ((ExtendedBaseDO<?>) obj).recalculate();
          }
          fullTextEntityManager.index(obj);
          if (index++ % batchSize == 0) {
            fullTextEntityManager.flush(); // clear every batchSize since the queue is processed
          }
        }
      }
      final SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
      searchFactory.optimize(clazz);
      log.info("Re-indexing of " + index + " objects of type " + clazz.getName() + " done.");
      return index;
    });
  }

  private <T> long reindexMassIndexer(final Class<T> clazz) {
    return emgrFactory.runInTrans(emgr -> {
      final EntityManager em = emgr.getEntityManager();
      final Long number = getRowCount(em, clazz); // Get number of objects to re-index (select count(*) from).
      log.info("Starting (mass) re-indexing of " + number + " entries of type " + clazz.getName() + "...");
      final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
      try {
        fullTextEntityManager.createIndexer(clazz)//
                .batchSizeToLoadObjects(25) //
                //.cacheMode(CacheMode.NORMAL) //
                .threadsToLoadObjects(5) //
                //.threadsForIndexWriter(1) //
                .startAndWait();
      } catch (final InterruptedException ex) {
        log.error("Exception encountered while reindexing: " + ex.getMessage(), ex);
      }
      final SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
      searchFactory.optimize(clazz);
      log.info("Re-indexing of " + number + " objects of type " + clazz.getName() + " done.");
      return number;
    });
  }

  private <T> Long getRowCount(final EntityManager entityManager, final Class<T> clazz) {
    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    cq.select(cb.count(cq.from(clazz)));
    return entityManager.createQuery(cq).getSingleResult();
  }

  private <T> TypedQuery<T> createCriteria(final EntityManager entityManager, final Class<T> clazz, final ReindexSettings settings) {
    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> cr = cb.createQuery(clazz);
    From root = cr.from(clazz);
    if (settings != null) {
      String lastUpdateProperty = null;
      if (AbstractBaseDO.class.isAssignableFrom(clazz)) {
        lastUpdateProperty = "lastUpdate";
      } else if (StdRecord.class.isAssignableFrom(clazz)) {
        lastUpdateProperty = "modifiedAt";
      }
      if (lastUpdateProperty != null && settings.getFromDate() != null) {
        cb.equal(root.get(lastUpdateProperty), settings.getFromDate());
      }
      if (settings.getLastNEntries() != null) {
        if (clazz.isAssignableFrom(PfHistoryMasterDO.class)) {
          cr.orderBy(cb.desc(root.get("pk")));
        } else {
          cr.orderBy(cb.desc(root.get("id")));
        }
      }
    }
    TypedQuery<T> query = entityManager.createQuery(cr);
    if (settings != null && settings.getLastNEntries() != null) {
      query.setMaxResults(settings.getLastNEntries());
    }
    return query;
  }
}
