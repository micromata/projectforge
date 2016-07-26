package org.projectforge.framework.persistence.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistProp;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.genome.db.jpa.history.api.HistoryService;
import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import de.micromata.genome.db.jpa.history.entities.EntityOpType;
import de.micromata.genome.db.jpa.history.impl.HistoryEmgrAfterInsertedEventHandler;
import de.micromata.genome.db.jpa.history.impl.HistoryUpdateCopyFilterEventListener;
import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.EmgrAfterInsertedEvent;
import de.micromata.genome.jpa.events.EmgrUpdateCopyFilterEvent;
import de.micromata.hibernate.history.delta.PropertyDelta;
import de.micromata.hibernate.history.delta.SimplePropertyDelta;

/**
 * Utility to provide compat with BaseDao.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class HistoryBaseDaoAdapter
{
  private static final Logger log = Logger.getLogger(HistoryBaseDaoAdapter.class);

  private static final HistoryEntry[] HISTORY_ARR_TEMPL = new HistoryEntry[] {};

  public static HistoryEntry[] getHistoryFor(BaseDO<?> obj)
  {
    long begin = System.currentTimeMillis();
    HistoryEntry[] result = getHistoryEntries(obj).toArray(HISTORY_ARR_TEMPL);
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.getHistoryFor took: " + (end - begin) + " ms.");
    return result;
  }

  public static List<? extends HistoryEntry> getHistoryEntries(BaseDO<?> ob)
  {
    long begin = System.currentTimeMillis();
    HistoryService histservice = HistoryServiceManager.get().getHistoryService();
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    List<? extends HistoryEntry> ret = emf.runWoTrans((emgr) -> {
      return histservice.getHistoryEntries(emgr, ob);
    });
    List<? extends HistoryEntry> nret = ret.stream()
        .sorted((e1, e2) -> e2.getModifiedAt().compareTo(e1.getModifiedAt())).collect(Collectors.toList());
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.getHistoryEntries took: " + (end - begin) + " ms.");
    return nret;
  }

  public static PropertyDelta diffEntryToPropertyDelta(DiffEntry de)
  {
    long begin = System.currentTimeMillis();
    SimplePropertyDelta ret = new SimplePropertyDelta(de.getPropertyName(), String.class, de.getOldValue(),
        de.getNewValue());
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.diffEntryToPropertyDelta took: " + (end - begin) + " ms.");
    return ret;
  }

  public static List<SimpleHistoryEntry> getSimpleHistoryEntries(final BaseDO<?> ob, UserGroupCache userGroupCache)
  {
    long begin = System.currentTimeMillis();
    List<SimpleHistoryEntry> ret = new ArrayList<>();
    List<? extends HistoryEntry> hel = getHistoryEntries(ob);

    for (HistoryEntry he : hel) {
      List<DiffEntry> deltas = he.getDiffEntries();
      if (deltas.isEmpty() == true) {
        SimpleHistoryEntry se = new SimpleHistoryEntry(userGroupCache, he);
        ret.add(se);
      } else {
        for (DiffEntry de : deltas) {
          final SimpleHistoryEntry se = new SimpleHistoryEntry(userGroupCache, he, diffEntryToPropertyDelta(de));
          ret.add(se);
        }
      }

    }
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.getSimpleHistoryEntries took: " + (end - begin) + " ms.");
    return ret;
  }

  public static boolean isHistorizable(Object bean)
  {
    if (bean == null) {
      return false;
    }
    return isHistorizable(bean.getClass());
  }

  public static boolean isHistorizable(Class<?> clazz)
  {
    long begin = System.currentTimeMillis();
    boolean result = HistoryServiceManager.get().getHistoryService().hasHistory(clazz);
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.isHistorizable took: " + (end - begin) + " ms.");
    return result;
  }

  private static String histCollectionValueToString(Class<?> valueClass, Collection<?> value)
  {
    StringBuilder sb = new StringBuilder();
    for (Object ob : value) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      if (ob instanceof DbRecord) {
        DbRecord rec = (DbRecord) ob;
        sb.append(rec.getPk());
      } else {
        sb.append(ObjectUtils.toString(ob));
      }
    }
    return sb.toString();
  }

  private static String histValueToString(Class<?> valueClass, Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof Collection) {
      return histCollectionValueToString(valueClass, (Collection) value);
    }
    return ObjectUtils.toString(value);
  }

  public static void createHistoryEntry(Object entity, Number id, String user, String property,
      Class<?> valueClass, Object oldValue, Object newValue)
  {
    long begin = System.currentTimeMillis();
    String oldVals = histValueToString(valueClass, oldValue);
    String newVals = histValueToString(valueClass, newValue);

    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    emf.runInTrans((emgr) -> {
      HistoryServiceManager.get().getHistoryService().insertManualEntry(emgr, EntityOpType.Update,
          entity.getClass().getName(),
          id, user, property, valueClass.getName(), oldVals, newVals);
      return null;
    });
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.createHistoryEntry took: " + (end - begin) + " ms.");
  }

  public static void inserted(BaseDO<?> ob)
  {
    long begin = System.currentTimeMillis();
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    emf.runInTrans((emgr) -> {
      EmgrAfterInsertedEvent event = new EmgrAfterInsertedEvent(emgr, ob);
      new HistoryEmgrAfterInsertedEventHandler().onEvent(event);
      return null;
    });
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.inserted took: " + (end - begin) + " ms.");
  }

  public static ModificationStatus wrappHistoryUpdate(BaseDO<?> dbo, Supplier<ModificationStatus> callback)
  {
    long begin = System.currentTimeMillis();
    HistoryService historyService = HistoryServiceManager.get().getHistoryService();
    List<WithHistory> whanots = historyService.internalFindWithHistoryEntity(dbo);
    if (whanots.isEmpty() == true) {
      return callback.get();

    }
    Serializable entPk = dbo.getPk();

    Class<?> entClass = dbo.getClass();
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    ModificationStatus result = emf.runInTrans((emgr) -> {

      Map<String, HistProp> oprops = historyService.internalGetPropertiesForHistory(emgr, whanots, dbo);

      ModificationStatus ret = callback.get();

      Map<String, HistProp> nprops = historyService.internalGetPropertiesForHistory(emgr, whanots,
          dbo);
      historyService.internalOnUpdate(emgr, entClass.getName(), entPk, oprops, nprops);
      return ret;
    });
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.wrappHistoryUpdate took: " + (end - begin) + " ms.");
    return result;
  }

  public static void updated(BaseDO<?> oldo, BaseDO<?> newo)
  {
    long begin = System.currentTimeMillis();
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    emf.runInTrans((emgr) -> {
      EmgrUpdateCopyFilterEvent event = new EmgrUpdateCopyFilterEvent(emgr, oldo.getClass(), oldo.getClass(), oldo,
          newo,
          true);
      new HistoryUpdateCopyFilterEventListener().onEvent(event);
      return null;
    });
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.updated took: " + (end - begin) + " ms.");
  }

  public static void markedAsDeleted(ExtendedBaseDO<?> oldo, ExtendedBaseDO<?> newoj)
  {
    long begin = System.currentTimeMillis();
    boolean prev = newoj.isDeleted();
    newoj.setDeleted(true);
    updated(oldo, newoj);
    newoj.setDeleted(prev);
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.markedAsDeleted took: " + (end - begin) + " ms.");
  }

  public static void markedAsUnDeleted(ExtendedBaseDO<?> oldo, ExtendedBaseDO<?> newoj)
  {
    long begin = System.currentTimeMillis();
    boolean prev = newoj.isDeleted();
    newoj.setDeleted(false);
    updated(oldo, newoj);
    newoj.setDeleted(prev);
    long end = System.currentTimeMillis();
    log.info("HistoryBaseDaoAdapter.markedAsUnDeleted took: " + (end - begin) + " ms.");
  }
}
