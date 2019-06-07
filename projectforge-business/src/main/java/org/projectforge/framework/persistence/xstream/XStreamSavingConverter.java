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

package org.projectforge.framework.persistence.xstream;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.api.IManualIndex;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.hibernate.history.delta.PropertyDelta;

/**
 * Registers all read objects and saves them in the configurable order to the data base.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class XStreamSavingConverter implements Converter
{
  /** The logger */
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XStreamSavingConverter.class);

  private final ConverterLookup defaultConv;

  private final Map<Class<?>, List<Object>> allObjects = new HashMap<Class<?>, List<Object>>();

  private final Set<Class<?>> writtenObjectTypes = new HashSet<Class<?>>();

  // Objekte dürfen nur einmal geschrieben werden, daher merken, was bereits gespeichert wurde
  private final Set<Object> writtenObjects = new HashSet<Object>();

  // Store the objects in the given order and all the other object types which are not listed here afterwards.
  private final List<Class<?>> orderOfSaving = new ArrayList<Class<?>>();

  // Ignore these objects from saving because the are saved implicit by their parent objects.
  private final Set<Class<?>> ignoreFromSaving = new HashSet<Class<?>>();

  // This map contains the mapping between the id's of the given xml stream and the new id's given by Hibernate. This is needed for writing
  // the history entries with the new id's.
  private final Map<String, Serializable> entityMapping = new HashMap<String, Serializable>();

  private final List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();

  private final Map<String, Class<?>> historyClassMapping = new HashMap<String, Class<?>>();

  private Session session;

  public XStreamSavingConverter() throws HibernateException
  {
    final XStream xstream = new XStream();
    defaultConv = xstream.getConverterLookup();
    // TODO HISTORY
    //    this.ignoreFromSaving.add(PropertyDelta.class);
    //    this.ignoreFromSaving.add(SimplePropertyDelta.class);
    //    this.ignoreFromSaving.add(AssociationPropertyDelta.class);
    //    this.ignoreFromSaving.add(CollectionPropertyDelta.class);
  }

  public void setSession(final Session session)
  {
    this.session = session;
  }

  public Map<Class<?>, List<Object>> getAllObjects()
  {
    return allObjects;
  }

  public List<HistoryEntry> getHistoryEntries()
  {
    return historyEntries;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean canConvert(final Class arg0)
  {
    return true;
  }

  public XStreamSavingConverter appendOrderedType(final Class<?>... types)
  {
    if (types != null) {
      for (final Class<?> type : types) {
        this.orderOfSaving.add(type);
      }
    }
    return this;
  }

  public XStreamSavingConverter appendIgnoredObjects(final Class<?>... types)
  {
    if (types != null) {
      for (final Class<?> type : types) {
        this.ignoreFromSaving.add(type);
      }
    }
    return this;
  }

  public void saveObjects()
  {
    for (final Class<?> type : orderOfSaving) {
      this.historyClassMapping.put(getClassname4History(type), type);
      save(type);
    }
    for (final Map.Entry<Class<?>, List<Object>> entry : allObjects.entrySet()) {
      if (entry.getKey().equals(PfHistoryMasterDO.class) == true) {
        continue;
      }
      final Class<?> type = entry.getKey();
      this.historyClassMapping.put(getClassname4History(type), type);
      save(type);
    }
    for (final Class<?> type : ignoreFromSaving) {
      this.historyClassMapping.put(getClassname4History(type), type);
    }
    save(PfHistoryMasterDO.class);
  }

  /**
   * Will be called directly before an object will be saved.
   * 
   * @param obj
   * @return id of the inserted objects if saved manually inside this method or null if the object has to be saved by
   *         save method (default).
   */
  public Serializable onBeforeSave(final Session session, final Object obj)
  {
    if (obj instanceof PfHistoryMasterDO) {
      final PfHistoryMasterDO entry = (PfHistoryMasterDO) obj;
      final Long origEntityId = entry.getEntityId();
      final String entityClassname = entry.getEntityName();
      final Serializable newId = getNewId(entityClassname, origEntityId);
      // TODO HISTORY
      //      final List<PropertyDelta> delta = entry.getDelta();
      Serializable id = null;
      if (newId != null) {
        // No public access, so try this:
        invokeHistorySetter(entry, "setEntityId", Integer.class, newId);
      } else {
        log.error("Can't find mapping of old entity id. This results in a corrupted history: " + entry);
      }
      invokeHistorySetter(entry, "setDelta", List.class, null);
      id = save(entry);
      final List<PropertyDelta> list = new ArrayList<PropertyDelta>();
      invokeHistorySetter(entry, "setDelta", List.class, list);
      // TODO HISTORY
      //      for (final PropertyDelta deltaEntry : delta) {
      //        list.add(deltaEntry);
      //        save(deltaEntry);
      //      }
      this.historyEntries.add(entry);
      return id;
    }
    return null;
  }

  /**
   * Does nothing at default.
   * 
   * @param obj Please note: the id isn't yet set to this object!
   * @param id The new id of the data-base.
   */
  public void onAfterSave(final Object obj, final Serializable id)
  {
  }

  protected Serializable save(final BaseDO<? extends Serializable> obj,
      final Collection<? extends BaseDO<? extends Serializable>> children)
  {
    final List<Serializable> oldIdList = beforeSave(children);
    final Serializable id = save(obj);
    afterSave(children, oldIdList);
    return id;
  }

  /**
   * Remove the id (pk) of every children and stores it to the returned list.
   * 
   * @param children
   * @return The list of (old) ids of the children.
   */
  protected List<Serializable> beforeSave(final Collection<? extends BaseDO<? extends Serializable>> children)
  {
    if (children == null || children.size() == 0) {
      return null;
    }
    final List<Serializable> idList = new ArrayList<Serializable>(children.size());
    for (final BaseDO<?> child : children) {
      idList.add(child.getId());
      child.setId(null);
    }
    return idList;
  }

  /**
   * Registers all children with their old and new id.
   * 
   * @param children
   * @param oldIdList The returned list of beforeSave(...) method.
   */
  protected void afterSave(final Collection<? extends BaseDO<? extends Serializable>> children,
      final List<Serializable> oldIdList)
  {
    if (oldIdList == null) {
      return;
    }
    final Iterator<Serializable> oldIdListIterator = oldIdList.iterator();
    final Iterator<? extends BaseDO<?>> childIterator = children.iterator();
    while (oldIdListIterator.hasNext() == true) {
      final BaseDO<?> child = childIterator.next();
      registerEntityMapping(child.getClass(), oldIdListIterator.next(), child.getId());
    }
  }

  /**
   * These methods are not public.
   * 
   * @param name
   * @param value
   */
  private void invokeHistorySetter(final HistoryEntry entry, final String name, final Class<?> parameterType,
      final Object value)
  {
    try {
      final Method method = HistoryEntry.class.getDeclaredMethod(name, parameterType);
      method.setAccessible(true);
      method.invoke(entry, value);
    } catch (final IllegalArgumentException ex) {
      log.error("Can't modify id of history entry. This results in a corrupted history: " + entry);
      log.error("Exception encountered " + ex, ex);
    } catch (final IllegalAccessException ex) {
      log.error("Can't modify id of history entry. This results in a corrupted history: " + entry);
      log.error("Exception encountered " + ex, ex);
    } catch (final InvocationTargetException ex) {
      log.error("Can't modify id of history entry. This results in a corrupted history: " + entry);
      log.error("Exception encountered " + ex, ex);
    } catch (final SecurityException ex) {
      log.error("Can't modify id of history entry. This results in a corrupted history: " + entry);
      log.error("Exception encountered " + ex, ex);
    } catch (final NoSuchMethodException ex) {
      log.error("Can't modify id of history entry. This results in a corrupted history: " + entry);
      log.error("Exception encountered " + ex, ex);
    }
  }

  private void save(final Class<?> type)
  {
    if (ignoreFromSaving.contains(type) == true || writtenObjectTypes.contains(type) == true) {
      // Already written.
      return;
    }
    writtenObjectTypes.add(type);
    // Persistente Klasse?
    if (HibernateUtils.isEntity(type) == false) {
      return;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("Writing objects from type: " + type);
    }
    final List<Object> list = allObjects.get(type);
    if (list == null) {
      return;
    }
    for (final Object obj : list) {
      if (obj == null || writtenObjects.contains(obj) == true) {
        // Object null or already written. Skip this item.
        continue;
      }
      if (session.contains(obj) == true) {
        continue;
      }
      try {
        if (log.isDebugEnabled()) {
          log.debug("Try to write object " + obj);
        }
        Serializable id = onBeforeSave(session, obj);
        if (id == null) {
          id = save(obj);
        }
        onAfterSave(obj, id);
        if (log.isDebugEnabled() == true) {
          log.debug("wrote object " + obj + " under id " + id);
        }
      } catch (final HibernateException ex) {
        log.error("Failed to write " + obj + " ex=" + ex, ex);
      } catch (final NullPointerException ex) {
        log.error("Failed to write " + obj + " ex=" + ex, ex);
      }
    }
  }

  /**
   * Should return the id value of the imported xml object (the origin id of the data-base the dump is from).
   * 
   * @param The object with the origin id.
   * @return null if not overridden.
   */
  protected Serializable getOriginalIdentifierValue(final Object obj)
  {
    return null;
  }

  protected Serializable save(final Object obj)
  {
    final Serializable oldId = getOriginalIdentifierValue(obj);
    final Serializable id;
    if (session.contains(obj) == false) {
      if (obj instanceof BaseDO) {
        if (obj instanceof IManualIndex == false) {
          ((BaseDO<?>) obj).setId(null);
        }
        id = session.save(obj);
        if (oldId != null) {
          registerEntityMapping(obj.getClass(), oldId, id);
        }
        writtenObjects.add(obj);
      } else if (obj instanceof HistoryEntry) {
        // HistoryEntry
        ((HistoryEntry) obj).setPk(null);
        id = session.save(obj);
      } else if (obj instanceof PropertyDelta) {
        // PropertyDelta
        ((PropertyDelta) obj).setId(null);
        id = session.save(obj);
      } else if (obj instanceof UserXmlPreferencesDO) {
        ((UserXmlPreferencesDO) obj).setId(null);
        id = session.save(obj);
      } else {
        log.warn("Unknown object: " + obj);
        id = session.save(obj);
      }
    } else {
      session.saveOrUpdate(obj);
      id = ((BaseDO<?>) obj).getId();
    }
    session.flush();
    return id;
  }

  public Class<?> getClassFromHistoryName(final String classname)
  {
    return this.historyClassMapping.get(classname);
  }

  private String getClassname4History(final Class<?> cls)
  {
    return cls.getName();
  }

  protected void registerEntityMapping(final Class<?> entityClass, final Serializable oldId, final Serializable newId)
  {
    final Serializable registeredNewId = getNewId(entityClass, oldId);
    if (registeredNewId != null && registeredNewId.equals(newId) == false) {
      log.error("Oups, double entity mapping found for entity '"
          + entityClass
          + "' with old id="
          + oldId
          + " . New id "
          + newId
          + " ignored, using previous stored id "
          + registeredNewId
          + " instead.");
    } else {
      this.entityMapping.put(getClassname4History(entityClass) + oldId, newId);
    }
  }

  public Integer getNewIdAsInteger(final Class<?> entityClass, final Integer oldId)
  {
    final Serializable newId = getNewId(entityClass, oldId);
    if (newId == null) {
      log.error("Oups, can't find '" + entityClass + "' id '" + oldId + "'.");
      return null;
    } else if (newId instanceof Integer == false) {
      log.error("Oups, can't get '" + entityClass + "' id '" + oldId + "' as integer: " + newId);
      return null;
    }
    return (Integer) newId;
  }

  public Serializable getNewId(final Class<?> entityClass, final Serializable oldId)
  {
    return getNewId(getClassname4History(entityClass), oldId);
  }

  protected Serializable getNewId(final String entityClassname, final Serializable oldId)
  {
    return this.entityMapping.get(entityClassname + oldId);
  }

  @Override
  public void marshal(final Object arg0, final HierarchicalStreamWriter arg1, final MarshallingContext arg2)
  {
    defaultConv.lookupConverterForType(arg0.getClass()).marshal(arg0, arg1, arg2);
  }

  @Override
  public Object unmarshal(final HierarchicalStreamReader arg0, final UnmarshallingContext arg1)
  {
    Object result;
    Class<?> targetType = null;
    try {
      targetType = arg1.getRequiredType();
      Converter conv = defaultConv.lookupConverterForType(targetType);
      result = conv.unmarshal(arg0, arg1);
    } catch (final Exception ex) {
      log.warn("Ignore unknown class or property " + targetType + " " + ex.getMessage());
      return null;
    }
    try {
      if (result != null) {
        registerObject(result);
      }
    } catch (final HibernateException ex) {
      log.error("Failed to write " + result + " ex=" + ex, ex);
    } catch (final NullPointerException ex) {
      log.error("Failed to write " + result + " ex=" + ex, ex);
    }
    return result;
  }

  private void registerObject(final Object obj)
  {
    if (obj == null) {
      return;
    }
    if (HibernateUtils.isEntity(obj.getClass()) == false) {
      return;
    }
    if (this.ignoreFromSaving.contains(obj.getClass()) == true) {
      // Don't need this objects as "top level" objects in list. They're usually encapsulated.
      return;
    }
    List<Object> list = this.allObjects.get(obj.getClass());
    if (list == null) {
      list = new ArrayList<Object>();
      this.allObjects.put(obj.getClass(), list);
    }
    list.add(obj);
  }
}
