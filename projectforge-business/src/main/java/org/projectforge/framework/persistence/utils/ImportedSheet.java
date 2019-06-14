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

package org.projectforge.framework.persistence.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;

/**
 * Represents an imported sheet (e. g. MS Excel sheet) containing the bean objects.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImportedSheet<T> implements Serializable
{
  private static final long serialVersionUID = 3643437932617400461L;

  private List<ImportedElement<T>> elements;

  private String name;

  private boolean open;

  private int totalNumberOfElements;

  private int numberOfNewElements;

  private int numberOfModifiedElements;

  private int numberOfUnmodifiedElements;

  private int numberOfFaultyElements;

  private boolean dirty = true;

  private boolean reconciled = false;

  private int numberOfCommittedElements = -1;

  private Map<String, Object> properties;

  private Map<String, Set<Object>> errorProperties;

  private ImportStatus status = ImportStatus.NOT_RECONCILED;

  /**
   * List of imported elements (e. g. MS Excel rows as bean object).
   * @return
   */
  public List<ImportedElement<T>> getElements()
  {
    return elements;
  }

  public void addElement(final ImportedElement<T> element)
  {
    if (elements == null) {
      elements = new ArrayList<ImportedElement<T>>();
    }
    elements.add(element);
  }

  public int getTotalNumberOfElements()
  {
    checkStatistics();
    return totalNumberOfElements;
  }

  /**
   * Nur ungleich 0, falls die Datensätze schon verprobt wurden.
   */
  public int getNumberOfNewElements()
  {
    checkStatistics();
    return numberOfNewElements;
  }

  public void selectAll(final boolean select, final boolean onlyModified)
  {
    if (elements == null) {
      return;
    }
    for (final ImportedElement<T> element : elements) {
      if (onlyModified == false || element.isModified() == true || element.isNew() == true) {
        element.setSelected(select);
      } else {
        element.setSelected(!select);
      }
    }
  }

  public void select(final boolean select, final boolean onlyModified, final int number)
  {
    if (elements == null) {
      return;
    }
    int counter = number;
    for (final ImportedElement<T> element : elements) {
      if (onlyModified == false || element.isModified() == true || element.isNew() == true) {
        if (--counter < 0) {
          element.setSelected(!select);
        } else {
          element.setSelected(select);
        }
      } else {
        element.setSelected(!select);
      }
    }
  }

  /**
   * Nur ungleich 0, falls die Datensätze schon verprobt wurden.
   */
  public int getNumberOfModifiedElements()
  {
    checkStatistics();
    return numberOfModifiedElements;
  }

  /**
   * Nur ungleich 0, falls die Datensätze schon verprobt wurden.
   */
  public int getNumberOfUnmodifiedElements()
  {
    return numberOfUnmodifiedElements;
  }

  public void calculateStatistics()
  {
    totalNumberOfElements = 0;
    numberOfNewElements = 0;
    numberOfModifiedElements = 0;
    numberOfUnmodifiedElements = 0;
    numberOfFaultyElements = 0;
    boolean changes = false;
    if (elements != null) {
      for (final ImportedElement<T> element : elements) {
        totalNumberOfElements++;
        if (reconciled == true) {
          element.setReconciled(true);
          if (element.isNew() == true) {
            numberOfNewElements++;
            changes = true;
          } else if (element.isModified() == true) {
            numberOfModifiedElements++;
            changes = true;
          } else if (element.isUnmodified() == true) {
            numberOfUnmodifiedElements++;
          }
        }
        if (element.isFaulty() == true) {
          numberOfFaultyElements++;
        }
      }
    }
    if (status == ImportStatus.RECONCILED) {
      if (changes == false) {
        status = ImportStatus.NOTHING_TODO;
      }
    }
    if (isFaulty() == true) {
      status = ImportStatus.HAS_ERRORS;
    }
    dirty = false;
  }

  private void checkStatistics()
  {
    if (dirty == true) {
      calculateStatistics();
    }
  }

  /**
   * Name of the sheet (e. g. name of the MS Excel sheet).
   * @return
   */
  public String getName()
  {
    return name;
  }

  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * Can be used for opening and closing this sheet in gui.
   */
  public boolean isOpen()
  {
    return open;
  }

  public void setOpen(final boolean open)
  {
    this.open = open;
  }

  public ImportStatus getStatus()
  {
    if (status == null) {
      checkStatistics();
    }
    return status;
  }

  public boolean isReconciled()
  {
    return reconciled;
  }

  public boolean isFaulty()
  {
    return numberOfFaultyElements > 0;
  }

  /**
   * After commit, the number of committed values will be given.
   */
  public int getNumberOfCommittedElements()
  {
    return numberOfCommittedElements;
  }

  public void setNumberOfCommittedElements(final int numberOfCommittedElements)
  {
    this.numberOfCommittedElements = numberOfCommittedElements;
  }

  public int getNumberOfFaultyElements()
  {
    return numberOfFaultyElements;
  }

  public void setProperty(final String key, final Object value)
  {
    if (this.properties == null) {
      this.properties = new HashMap<String, Object>();
    }
    this.properties.put(key, value);
  }

  public Object getProperty(final String key)
  {
    if (this.properties == null) {
      return null;
    }
    return this.properties.get(key);
  }

  public Map<String, Set<Object>> getErrorProperties()
  {
    if (dirty == false && this.errorProperties != null) {
      return this.errorProperties;
    }
    if (CollectionUtils.isEmpty(this.elements) == true) {
      return null;
    }
    errorProperties = null;
    for (final ImportedElement<T> el : this.elements) {
      if (el.isFaulty() == true) {
        final Map<String, Object> map = el.getErrorProperties();
        for (final String key : map.keySet()) {
          final Object value = map.get(key);
          if (errorProperties == null) {
            errorProperties = new HashMap<String, Set<Object>>();
          }
          Set<Object> set = null;
          if (errorProperties.containsKey(key) == true) {
            set = errorProperties.get(key);
          }
          if (set == null) {
            set = new TreeSet<Object>();
            errorProperties.put(key, set);
          }
          set.add(value);
        }
      }
    }
    return errorProperties;
  }

  public void setStatus(final ImportStatus status)
  {
    boolean allowed = true;
    if (this.status == ImportStatus.NOT_RECONCILED || this.status == null) {
      if (status.isIn(ImportStatus.IMPORTED, ImportStatus.NOTHING_TODO) == true) {
        // State change not allowed.
        allowed = false;
      }
    } else if (this.status == ImportStatus.RECONCILED) {
      // Everything is allowed
    } else {
      // Everything is allowed
    }
    if (allowed == false) {
      throw new UnsupportedOperationException("State change not allowed: '" + this.status + "' -> '" + status + "'");
    }
    this.status = status;
    if (status == ImportStatus.RECONCILED) {
      reconciled = true;
    } else if (status == ImportStatus.NOT_RECONCILED) {
      reconciled = false;
    }
    if (isFaulty() == true) {
      this.status = ImportStatus.HAS_ERRORS;
    }
  }
}
