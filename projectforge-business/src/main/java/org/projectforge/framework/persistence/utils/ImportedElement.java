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

package org.projectforge.framework.persistence.utils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import java.util.Objects;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.utils.NumberHelper;

import de.micromata.hibernate.history.delta.PropertyDelta;
import de.micromata.hibernate.history.delta.SimplePropertyDelta;

/**
 * Stores one imported object (e. g. MS Excel row as bean object). It also contains information about the status: New object or modified
 * object.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImportedElement<T> implements Serializable
{
  private static final long serialVersionUID = -3405918702811291053L;

  protected T value;

  protected T oldValue;

  private final int index;

  private final Class<T> clazz;

  private final String[] diffProperties;

  private List<PropertyDelta> propertyDeltas;

  private boolean reconciled;

  private boolean selected;

  private Map<String, Object> errorProperties;

  /**
   * Later the diff properties should be replaced by HibernateHistory and AbstractBaseDO mechanism.
   * @param clazz Needed for reflection.
   * @param diffProperties List of property names which will be used for display property changes.
   */
  public ImportedElement(int index, Class<T> clazz, String[] diffProperties)
  {
    this.index = index;
    this.clazz = clazz;
    this.diffProperties = diffProperties;
  }

  public void setValue(T value)
  {
    this.value = value;
    this.propertyDeltas = null;
  }

  public T getValue()
  {
    return value;
  }

  public void setOldValue(T oldValue)
  {
    this.oldValue = oldValue;
    this.propertyDeltas = null;
  }

  public T getOldValue()
  {
    return oldValue;
  }

  public List<PropertyDelta> getPropertyChanges()
  {
    if (oldValue == null) {
      return null;
    }
    if (propertyDeltas != null) {
      return propertyDeltas;
    }

    final List<PropertyDelta> deltas = new ArrayList<>();
    for (final String fieldname : diffProperties) {
      final Method method = BeanHelper.determineGetter(clazz, fieldname);
      if (method == null) {
        throw new UnsupportedOperationException("Oups, no getter for property '"
            + fieldname
            + "' found for (maybe typo in fieldname?): "
            + value);
      }
      final Object newValue = BeanHelper.invoke(value, method);
      final Object origValue = BeanHelper.invoke(oldValue, method);
      final Class<?> type = method.getReturnType();

      createPropertyDelta(fieldname, newValue, origValue, type)
          .ifPresent(deltas::add);
    }

    deltas.addAll(addAdditionalPropertyDeltas());

    if (deltas.size() > 0) {
      propertyDeltas = deltas;
    }

    return propertyDeltas;
  }

  /**
   * Can be overridden by sub class to add additional property deltas.
   *
   * @return Collection of additional property deltas.
   */
  protected Collection<? extends PropertyDelta> addAdditionalPropertyDeltas()
  {
    return Collections.emptyList();
  }

  protected Optional<PropertyDelta> createPropertyDelta(String fieldname, Object newValue, Object origValue, Class<?> type)
  {
    boolean modified = false;
    if (type == BigDecimal.class) {
      if (NumberHelper.isEqual((BigDecimal) newValue, (BigDecimal) origValue) == false) {
        modified = true;
      }
    } else if (Objects.equals(newValue, origValue) == false) {
      modified = true;
    }
    if (modified) {
      Object ov;
      Object nv;
      if (origValue instanceof ShortDisplayNameCapable) {
        ov = ((ShortDisplayNameCapable) origValue).getShortDisplayName();
      } else {
        ov = origValue;
      }
      if (newValue instanceof ShortDisplayNameCapable) {
        nv = ((ShortDisplayNameCapable) newValue).getShortDisplayName();
      } else {
        nv = newValue;
      }
      return Optional.of(new MySimplePropertyDelta(fieldname, type, ov, nv));
    }

    return Optional.empty();
  }

  private class MySimplePropertyDelta extends SimplePropertyDelta implements Serializable
  {
    private static final long serialVersionUID = -6828269529571580866L;

    private MySimplePropertyDelta(final String propertyName, final Class< ? > propertyType, final Object oldValue, final Object newValue)
    {
      super(propertyName, propertyType, oldValue, newValue);
    }
  }

  /**
   * Noch nicht verprobte Datensätze (isReconciled == false) gelten nicht als modifiziert.
   */
  public boolean isModified()
  {
    return reconciled == true && oldValue != null && CollectionUtils.isEmpty(getPropertyChanges()) == false;
  }

  /**
   * Noch nicht verprobte Datensätze (isReconciled == false) gelten weder als modifiziert noch als nicht modifiziert.
   */
  public boolean isUnmodified()
  {
    return reconciled == true && oldValue != null && oldValue.equals(value) == true;
  }

  /**
   * Noch nicht verprobte Datensätze (isReconciled == false) gelten nicht als neu.
   */
  public boolean isNew()
  {
    return reconciled == true && oldValue == null;
  }

  /**
   * Wurde dieser Eintrag schon verprobt? Erst, wenn er verprobt wurde, ergeben die anderen Abfragen isModified etc. Sinn.
   */
  public boolean isReconciled()
  {
    return reconciled;
  }

  /**
   * @return true, if errorProperties is not empty, otherwise false.
   */
  public boolean isFaulty()
  {
    return MapUtils.isNotEmpty(errorProperties);
  }

  public void setReconciled(boolean reconciled)
  {
    this.reconciled = reconciled;
  }

  /**
   * Only selected values will be imported. If hasErrors = true, always false will be returned.
   */
  public boolean isSelected()
  {
    return isFaulty() == false && selected;
  }

  /**
   * If hasErrors == true then this item will be deselected.
   * @param selected
   */
  public void setSelected(boolean selected)
  {
    if (isFaulty() == false) {
      this.selected = selected;
    } else {
      this.selected = false;
    }
  }

  /**
   * Should be unique in the ImportedSheet and is use-able for indexed properties (e. g. check boxes).
   * @return
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * For properties which can't be mapped due to errors (e. g. referenced element not found).
   * @param key
   * @param value
   */
  public void putErrorProperty(String key, Object value)
  {
    if (errorProperties == null) {
      errorProperties = new HashMap<>();
    }
    errorProperties.put(key, value);
  }

  public void removeErrorProperty(String key)
  {
    if (errorProperties != null) {
      errorProperties.remove(key);
    }
  }

  public Map<String, Object> getErrorProperties()
  {
    return errorProperties;
  }

  /**
   * @param key
   * @return The error property if found, otherwise null.
   */
  public Object getErrorProperty(String key)
  {
    Object obj = null;
    if (errorProperties != null) {
      obj = errorProperties.get(key);
    }
    if (obj == null) {
      return null;
    }
    return obj;
  }
}
