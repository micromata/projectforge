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

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Hotfix: Hibernate-search does not update index of dependent objects.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class HibernateSearchDependentObjectsReindexer {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(HibernateSearchDependentObjectsReindexer.class);

  @Autowired
  private PfEmgrFactory emgrFactory;

  /**
   * Key is the embedded class (annotated with @IndexEmbedded), value the set of all dependent objects.
   */
  final Map<Class<? extends BaseDO<?>>, List<Entry>> map = new HashMap<>();

  @PostConstruct
  public void init() {
    for (final RegistryEntry registryEntry : Registry.getInstance().getOrderedList()) {
      register(registryEntry);
    }
  }

  class Entry {
    Class<? extends BaseDO<?>> clazz; // The dependent class which contains the annotated field.

    String fieldName;

    boolean setOrCollection;

    Entry(final Class<? extends BaseDO<?>> clazz, final String fieldName, final boolean setOrCollection) {
      this.clazz = clazz;
      this.fieldName = fieldName;
      this.setOrCollection = setOrCollection;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "Entry[clazz=" + clazz.getName() + ",fieldName=" + fieldName + "]";
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof Entry)) {
        return false;
      }
      final Entry o = (Entry) obj;
      return clazz.equals(o.clazz) && fieldName.equals(o.fieldName);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
      result = prime * result + (setOrCollection ? 1231 : 1237);
      return result;
    }

  }

  public void reindexDependents(final BaseDO<?> obj) {
    new Thread() {
      @Override
      public void run() {
        emgrFactory.runInTrans(emgr -> {
                  final EntityManager em = emgr.getEntityManager();
                  final Set<String> alreadyReindexed = new HashSet<>();
                  final List<Entry> entryList = map.get(obj.getClass());
                  reindexDependents(em, obj, entryList, alreadyReindexed);
                  final int size = alreadyReindexed.size();
                  if (size >= 10) {
                    log.info("Re-indexing of " + size + " objects done after updating " + obj.getClass().getName() + ":"
                            + obj.getId());
                  }
                  return null;
                }
        );
      }
    }.start();
  }

  private void reindexDependents(final EntityManager em, final BaseDO<?> obj,
                                 final List<Entry> entryList, final Set<String> alreadyReindexed) {
    if (CollectionUtils.isEmpty(entryList)) {
      // Nothing to do.
      return;
    }
    for (final Entry entry : entryList) {
      final RegistryEntry registryEntry = Registry.getInstance().getEntryByDO(entry.clazz);
      if (registryEntry == null) {
        // Nothing to do
        return;
      }
      final List<?> result = getDependents(em, registryEntry, entry, obj);
      if (result != null) {
        for (Object dependentObject : result) {
          if (dependentObject instanceof Object[]) {
            dependentObject = ((Object[]) dependentObject)[0];
          }
          if (dependentObject instanceof BaseDO) {
            reindexDependents(em, (BaseDO<?>) dependentObject, alreadyReindexed);
          }
        }
      }
    }
  }

  private void reindexDependents(final EntityManager em, final BaseDO<?> obj,
                                 final Set<String> alreadyReindexed) {
    if (alreadyReindexed.contains(getReindexId(obj))) {
      if (log.isDebugEnabled()) {
        log.debug("Object already re-indexed (skipping): " + getReindexId(obj));
      }
      return;
    }
    em.flush(); // Needed to flush the object changes!
    final FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

    fullTextEntityManager.setFlushMode(FlushModeType.AUTO);
    //HibernateCompatUtils.setCacheMode(fullTextSession, CacheMode.IGNORE);
    try {
      BaseDO<?> dbObj = em.find(obj.getClass(), obj.getId());
      if (dbObj == null) {
        dbObj = em.find(obj.getClass(), obj.getId());
      }
      fullTextEntityManager.index(dbObj);
      alreadyReindexed.add(getReindexId(dbObj));
      if (log.isDebugEnabled()) {
        log.debug("Object added to index: " + getReindexId(dbObj));
      }
    } catch (final Exception ex) {
      // Don't fail if any exception while re-indexing occurs.
      log.info("Fail to re-index " + obj.getClass() + ": " + ex.getMessage());
    }
    // em.flush(); // clear every batchSize since the queue is processed
    final List<Entry> entryList = map.get(obj.getClass());
    reindexDependents(em, obj, entryList, alreadyReindexed);
  }

  private List<?> getDependents(final EntityManager em,
                                final RegistryEntry registryEntry,
                                final Entry entry,
                                final BaseDO<?> obj) {
    final String queryString;
    if (entry.setOrCollection) {
      queryString = "from " + registryEntry.getDOClass().getName() + " o join o." + entry.fieldName + " r where r.id=:id";
    } else {
      queryString = "from " + registryEntry.getDOClass().getName() + " o where o." + entry.fieldName + ".id=:id";
    }
    if (log.isDebugEnabled()) {
      log.debug(queryString + ", id=" + obj.getId());
    }
    final List<?> result = em.createQuery(queryString, registryEntry.getDOClass())
            .setParameter("id", obj.getId())
            .getResultList();
    return result;
  }

  private String getReindexId(final BaseDO<?> obj) {
    return obj.getClass() + ":" + obj.getId();
  }

  void register(final RegistryEntry registryEntry) {
    final Class<? extends BaseDO<?>> clazz = registryEntry.getDOClass();
    register(clazz);
  }

  void register(final Class<? extends BaseDO<?>> clazz) {
    final Field[] fields = clazz.getDeclaredFields();
    for (final Field field : fields) {
      if (field.isAnnotationPresent(IndexedEmbedded.class) ||
              field.isAnnotationPresent(ContainedIn.class)) {
        Class<?> embeddedClass = field.getType();
        boolean setOrCollection = false;
        if (Set.class.isAssignableFrom(embeddedClass)
                || Collection.class.isAssignableFrom(embeddedClass)) {
          // Please use @ContainedIn.
          final Type type = field.getGenericType();
          if (type instanceof ParameterizedType) {
            final Type actualTypeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (actualTypeArgument instanceof Class) {
              embeddedClass = (Class<?>) actualTypeArgument;
              setOrCollection = true;
            }
          }
        }
        if (!BaseDO.class.isAssignableFrom(embeddedClass)) {
          // Only BaseDO objects are supported.
          continue;
        }
        final String name = field.getName();
        final Entry entry = new Entry(clazz, name, setOrCollection);
        List<Entry> list = map.get(embeddedClass);
        if (list == null) {
          list = new ArrayList<>();
          @SuppressWarnings("unchecked") final Class<? extends BaseDO<?>> embeddedBaseDOClass = (Class<? extends BaseDO<?>>) embeddedClass;
          map.put(embeddedBaseDOClass, list);
        } else {
          for (final Entry e : list) {
            if (entry.equals(e)) {
              log.warn("Entry already registerd: " + entry);
            }
          }
        }
        list.add(entry);
      }
    }
  }
}
